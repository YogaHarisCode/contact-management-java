package contactmanagement.controller;

import contactmanagement.entity.Contact;
import contactmanagement.entity.User;
import contactmanagement.model.ContactResponse;
import contactmanagement.model.ContactRequest;
import contactmanagement.model.WebResponse;
import contactmanagement.repository.ContactRepository;
import contactmanagement.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@code POST /api/contacts}.
 *
 * <p>Runs against the real datasource configured in {@code application.properties} and clears
 * the {@code contacts} and {@code users} tables before and after every test, so point it to a
 * dedicated test database.
 */
@SpringBootTest
@AutoConfigureRestTestClient
class ContactControllerTest {

    private static final String CONTACTS_URI = "/api/contacts";
    private static final String TOKEN_HEADER = "X-API-TOKEN";

    private static final String USERNAME = "johndoe";
    private static final String VALID_TOKEN = "valid-token-user-a";
    private static final String OTHER_USERNAME = "janedoe";
    private static final String OTHER_TOKEN = "valid-token-user-b";

    @Autowired
    private RestTestClient client;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContactRepository contactRepository;

    @BeforeEach
    void setUp() {
        // contacts first: it has a foreign key to users.
        contactRepository.deleteAll();
        userRepository.deleteAll();
        saveUser(USERNAME, "John Doe", VALID_TOKEN, futureExpiry());
    }

    @AfterEach
    void tearDown() {
        contactRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ------------------------------------------------------------ success

    @Test
    void shouldCreateContactSuccessfullyWhenDataIsValid() {
        WebResponse<ContactResponse> response = postOk(VALID_TOKEN, fullRequest());

        assertNotNull(response);
        assertNull(response.getErrors());

        ContactResponse data = response.getData();
        assertNotNull(data);
        assertDoesNotThrow(() -> UUID.fromString(data.getId()));
        assertEquals("Yoga", data.getFirstName());
        assertEquals("Harisistianugroho", data.getLastName());
        assertEquals("yoga@example.com", data.getEmail());
        assertEquals("081234567890", data.getPhone());
    }

    @Test
    void shouldPersistContactLinkedToAuthenticatedUserWhenDataIsValid() {
        WebResponse<ContactResponse> response = postOk(VALID_TOKEN, fullRequest());

        Contact saved = contactRepository.findById(response.getData().getId()).orElseThrow();
        assertEquals("Yoga", saved.getFirstName());
        assertEquals("Harisistianugroho", saved.getLastName());
        assertEquals("yoga@example.com", saved.getEmail());
        assertEquals("081234567890", saved.getPhone());
        assertEquals(USERNAME, saved.getUser().getUsername());
        assertEquals(1, contactRepository.count());
    }

    @Test
    void shouldLinkContactToOwnerOfTokenWhenMultipleUsersExist() {
        saveUser(OTHER_USERNAME, "Jane Doe", OTHER_TOKEN, futureExpiry());

        WebResponse<ContactResponse> response = postOk(OTHER_TOKEN, fullRequest());

        Contact saved = contactRepository.findById(response.getData().getId()).orElseThrow();
        assertEquals(OTHER_USERNAME, saved.getUser().getUsername());
    }

    @Test
    void shouldCreateContactWithNullOptionalFieldsWhenOnlyFirstNameIsProvided() {
        WebResponse<ContactResponse> response = postOk(VALID_TOKEN,
                ContactRequest.builder().firstName("Yoga").build());

        assertEquals("Yoga", response.getData().getFirstName());
        assertNull(response.getData().getLastName());
        assertNull(response.getData().getEmail());
        assertNull(response.getData().getPhone());

        Contact saved = contactRepository.findById(response.getData().getId()).orElseThrow();
        assertNull(saved.getLastName());
        assertNull(saved.getEmail());
        assertNull(saved.getPhone());
    }

    @Test
    void shouldAcceptValuesOnLengthBoundaries() {
        String of100 = "a".repeat(100);

        WebResponse<ContactResponse> response = postOk(VALID_TOKEN, ContactRequest.builder()
                .firstName(of100)
                .lastName(of100)
                .build());

        Contact saved = contactRepository.findById(response.getData().getId()).orElseThrow();
        assertEquals(of100, saved.getFirstName());
        assertEquals(of100, saved.getLastName());
    }

    @Test
    void shouldCreateTwoContactsWithDifferentIdsWhenCalledTwice() {
        String firstId = postOk(VALID_TOKEN, fullRequest()).getData().getId();
        String secondId = postOk(VALID_TOKEN, fullRequest()).getData().getId();

        assertNotEquals(firstId, secondId);
        assertEquals(2, contactRepository.count());
    }

    // ------------------------------------------------------- unauthorized

    @Test
    void shouldReturnUnauthorizedAndSaveNothingWhenTokenHeaderIsMissing() {
        assertUnauthorized(postContact(null, fullRequest()));

        assertEquals(0, contactRepository.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "unknown-token"})
    void shouldReturnUnauthorizedAndSaveNothingWhenTokenIsEmptyOrUnknown(String token) {
        assertUnauthorized(postContact(token, fullRequest()));

        assertEquals(0, contactRepository.count());
    }

    @Test
    void shouldReturnUnauthorizedAndSaveNothingWhenTokenIsExpired() {
        User user = userRepository.findById(USERNAME).orElseThrow();
        user.setTokenExpiredAt(System.currentTimeMillis() - 1_000L);
        userRepository.save(user);

        assertUnauthorized(postContact(VALID_TOKEN, fullRequest()));

        assertEquals(0, contactRepository.count());
    }

    /** The {@code User} parameter is resolved before the body, so 401 wins over 400. */
    @Test
    void shouldReturnUnauthorizedWhenTokenIsMissingAndBodyIsInvalid() {
        assertUnauthorized(postContact(null, ContactRequest.builder().firstName("").build()));

        assertEquals(0, contactRepository.count());
    }

    // --------------------------------------------------------- validation

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void shouldReturnBadRequestAndSaveNothingWhenRequestFailsValidation(ContactRequest invalid) {
        WebResponse<ContactResponse> response = postContact(VALID_TOKEN, invalid)
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getData());
        assertNotNull(response.getErrors());
        assertEquals(0, contactRepository.count());
    }

    @Test
    void shouldReturnBadRequestAndSaveNothingWhenBodyIsMalformedJson() {
        WebResponse<ContactResponse> response = postContact(VALID_TOKEN, "{ \"firstName\": ")
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals("Malformed or missing request body", response.getErrors());
        assertEquals(0, contactRepository.count());
    }

    /** One invalid payload per constraint: blank, null, whitespace-only, too long, bad email. */
    static Stream<ContactRequest> invalidRequests() {
        String tooLong = "a".repeat(101);
        return Stream.of(
                ContactRequest.builder().firstName(null).build(),
                ContactRequest.builder().firstName("").build(),
                ContactRequest.builder().firstName("   ").build(),
                ContactRequest.builder().firstName(tooLong).build(),
                ContactRequest.builder().firstName("Yoga").lastName(tooLong).build(),
                ContactRequest.builder().firstName("Yoga").email("not-an-email").build(),
                ContactRequest.builder().firstName("Yoga").email("eko@").build(),
                ContactRequest.builder().firstName("Yoga").email("a".repeat(195) + "@x.com").build(),
                ContactRequest.builder().firstName("Yoga").phone(tooLong).build()
        );
    }

    // ====================================================== GET CONTACT

    @Test
    void shouldReturnContactWhenContactBelongsToAuthenticatedUser() {
        Contact contact = saveContact(USERNAME, "Yoga", "Harisistianugroho", "yoga@example.com", "081234567890");

        WebResponse<ContactResponse> response = getContactOk(VALID_TOKEN, contact.getId());

        assertNotNull(response);
        assertNull(response.getErrors());
        assertEquals(contact.getId(), response.getData().getId());
        assertEquals("Yoga", response.getData().getFirstName());
        assertEquals("Harisistianugroho", response.getData().getLastName());
        assertEquals("yoga@example.com", response.getData().getEmail());
        assertEquals("081234567890", response.getData().getPhone());
    }

    @Test
    void shouldReturnRequestedContactWhenUserHasMultipleContacts() {
        saveContact(USERNAME, "First", null, null, null);
        Contact second = saveContact(USERNAME, "Second", null, null, null);

        WebResponse<ContactResponse> response = getContactOk(VALID_TOKEN, second.getId());

        assertEquals(second.getId(), response.getData().getId());
        assertEquals("Second", response.getData().getFirstName());
    }

    @Test
    void shouldOmitNullOptionalFieldsWhenStoredContactHasOnlyFirstName() {
        Contact contact = saveContact(USERNAME, "Yoga", null, null, null);

        String rawBody = getContact(VALID_TOKEN, contact.getId())
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(rawBody);
        assertFalse(rawBody.contains("lastName"));
        assertFalse(rawBody.contains("email"));
        assertFalse(rawBody.contains("phone"));
    }

    @Test
    void shouldNotExposeOwnerDataWhenGettingContact() {
        Contact contact = saveContact(USERNAME, "Yoga", null, null, null);

        String rawBody = getContact(VALID_TOKEN, contact.getId())
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(rawBody);
        assertFalse(rawBody.contains(USERNAME));
        assertFalse(rawBody.contains("password"));
        assertFalse(rawBody.contains("bcrypt-hash"));
        assertFalse(rawBody.contains(VALID_TOKEN));
    }

    @Test
    void shouldReturnNotFoundWhenContactIdDoesNotExist() {
        assertNotFound(getContact(VALID_TOKEN, UUID.randomUUID().toString()));
    }

    /** IDOR (Insecure Direct Object Reference): a valid id owned by someone else must look like "not found". */
    @Test
    void shouldReturnNotFoundWhenContactBelongsToAnotherUser() {
        saveUser(OTHER_USERNAME, "Jane Doe", OTHER_TOKEN, futureExpiry());
        Contact othersContact = saveContact(OTHER_USERNAME, "Secret", "Contact", "secret@example.com", "081234567890");

        WebResponse<ContactResponse> response = assertNotFound(getContact(VALID_TOKEN, othersContact.getId()));

        assertNull(response.getData());
        // The owner can still read it, so the 404 above is about ownership and not a missing row.
        assertEquals("Secret", getContactOk(OTHER_TOKEN, othersContact.getId()).getData().getFirstName());
    }

    @Test
    void shouldNotModifyContactWhenGettingContact() {
        Contact contact = saveContact(USERNAME, "Yoga", "Harisistianugroho", "yoga@example.com", "081234567890");

        getContactOk(VALID_TOKEN, contact.getId());

        Contact unchanged = contactRepository.findById(contact.getId()).orElseThrow();
        assertEquals("Yoga", unchanged.getFirstName());
        assertEquals("Harisistianugroho", unchanged.getLastName());
        assertEquals("yoga@example.com", unchanged.getEmail());
        assertEquals("081234567890", unchanged.getPhone());
        assertEquals(1, contactRepository.count());
    }

    @Test
    void shouldReturnUnauthorizedWhenGetContactTokenHeaderIsMissing() {
        Contact contact = saveContact(USERNAME, "Yoga", null, null, null);

        assertUnauthorized(getContact(null, contact.getId()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "unknown-token"})
    void shouldReturnUnauthorizedWhenGetContactTokenIsEmptyOrUnknown(String token) {
        Contact contact = saveContact(USERNAME, "Yoga", null, null, null);

        assertUnauthorized(getContact(token, contact.getId()));
    }

    @Test
    void shouldReturnUnauthorizedWhenGetContactTokenIsExpired() {
        Contact contact = saveContact(USERNAME, "Yoga", null, null, null);
        User user = userRepository.findById(USERNAME).orElseThrow();
        user.setTokenExpiredAt(System.currentTimeMillis() - 1_000L);
        userRepository.save(user);

        assertUnauthorized(getContact(VALID_TOKEN, contact.getId()));
    }

    // ====================================================== UPDATE CONTACT

    @Test
    void shouldUpdateContactSuccessfullyWhenDataIsValid() {
        Contact contact = givenStoredContact();

        WebResponse<ContactResponse> response = putOk(contact.getId(), updateRequest());

        assertNotNull(response);
        assertNull(response.getErrors());
        assertEquals(contact.getId(), response.getData().getId());
        assertEquals("Eko", response.getData().getFirstName());
        assertEquals("Kurniawan", response.getData().getLastName());
        assertEquals("eko@example.com", response.getData().getEmail());
        assertEquals("089876543210", response.getData().getPhone());
    }

    @Test
    void shouldPersistChangesInDatabaseWhenContactIsUpdated() {
        Contact contact = givenStoredContact();

        putOk(contact.getId(), updateRequest());

        assertStoredContact(contact.getId(), "Eko", "Kurniawan", "eko@example.com", "089876543210");
        assertEquals(1, contactRepository.count());
    }

    @Test
    void shouldReturnUpdatedValuesWhenContactIsFetchedAfterUpdate() {
        Contact contact = givenStoredContact();

        putOk(contact.getId(), updateRequest());

        WebResponse<ContactResponse> fetched = getContactOk(VALID_TOKEN, contact.getId());
        assertEquals("Eko", fetched.getData().getFirstName());
        assertEquals("089876543210", fetched.getData().getPhone());
    }

    @Test
    void shouldClearOptionalFieldsInDatabaseWhenTheyAreNotProvidedOnUpdate() {
        Contact contact = givenStoredContact();

        WebResponse<ContactResponse> response = putOk(contact.getId(),
                ContactRequest.builder().firstName("Eko").build());

        assertEquals("Eko", response.getData().getFirstName());
        assertNull(response.getData().getLastName());
        assertNull(response.getData().getEmail());
        assertNull(response.getData().getPhone());
        assertStoredContact(contact.getId(), "Eko", null, null, null);
    }

    @Test
    void shouldStoreNullEmailWhenEmailIsEmptyStringOnUpdate() {
        Contact contact = givenStoredContact();

        WebResponse<ContactResponse> response = putOk(contact.getId(),
                ContactRequest.builder().firstName("Eko").email("").build());

        assertNull(response.getData().getEmail());
        assertNull(contactRepository.findById(contact.getId()).orElseThrow().getEmail());
    }

    @Test
    void shouldKeepOwnerUnchangedWhenContactIsUpdated() {
        Contact contact = givenStoredContact();

        putOk(contact.getId(), updateRequest());

        User owner = userRepository.findById(USERNAME).orElseThrow();
        assertTrue(contactRepository.findByUserAndId(owner, contact.getId()).isPresent());
    }

    @Test
    void shouldUpdateOnlyTargetContactWhenUserHasMultipleContacts() {
        Contact target = givenStoredContact();
        Contact other = saveContact(USERNAME, "Other", "Contact", "other@example.com", "081111111111");

        putOk(target.getId(), updateRequest());

        assertStoredContact(target.getId(), "Eko", "Kurniawan", "eko@example.com", "089876543210");
        assertStoredContact(other.getId(), "Other", "Contact", "other@example.com", "081111111111");
    }

    @Test
    void shouldIgnoreIdInBodyAndUpdateContactFromPathWhenBodyContainsId() {
        Contact target = givenStoredContact();
        Contact other = saveContact(USERNAME, "Other", null, null, null);

        putContact(VALID_TOKEN, target.getId(),
                "{\"id\":\"" + other.getId() + "\",\"firstName\":\"Eko\"}")
                .expectStatus().isOk();

        assertEquals("Eko", contactRepository.findById(target.getId()).orElseThrow().getFirstName());
        assertEquals("Other", contactRepository.findById(other.getId()).orElseThrow().getFirstName());
    }

    // ---------------------------------------------- update: not found

    @Test
    void shouldReturnNotFoundAndCreateNothingWhenUpdatingUnknownContactId() {
        assertNotFound(putContact(VALID_TOKEN, UUID.randomUUID().toString(), updateRequest()));

        assertEquals(0, contactRepository.count());
    }

    /** IDOR: id milik user lain harus terlihat sebagai "not found" dan datanya tidak berubah. */
    @Test
    void shouldReturnNotFoundAndKeepDataWhenUpdatingContactOfAnotherUser() {
        saveUser(OTHER_USERNAME, "Jane Doe", OTHER_TOKEN, futureExpiry());
        Contact othersContact = saveContact(OTHER_USERNAME, "Secret", "Contact", "secret@example.com", "081234567890");

        assertNotFound(putContact(VALID_TOKEN, othersContact.getId(), updateRequest()));

        assertStoredContact(othersContact.getId(), "Secret", "Contact", "secret@example.com", "081234567890");
    }

    // ---------------------------------------------- update: unauthorized

    @Test
    void shouldReturnUnauthorizedAndKeepDataWhenUpdateTokenHeaderIsMissing() {
        Contact contact = givenStoredContact();

        assertUnauthorized(putContact(null, contact.getId(), updateRequest()));

        assertStoredContactIsOriginal(contact.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "unknown-token"})
    void shouldReturnUnauthorizedAndKeepDataWhenUpdateTokenIsEmptyOrUnknown(String token) {
        Contact contact = givenStoredContact();

        assertUnauthorized(putContact(token, contact.getId(), updateRequest()));

        assertStoredContactIsOriginal(contact.getId());
    }

    @Test
    void shouldReturnUnauthorizedAndKeepDataWhenUpdateTokenIsExpired() {
        Contact contact = givenStoredContact();
        User user = userRepository.findById(USERNAME).orElseThrow();
        user.setTokenExpiredAt(System.currentTimeMillis() - 1_000L);
        userRepository.save(user);

        assertUnauthorized(putContact(VALID_TOKEN, contact.getId(), updateRequest()));

        assertStoredContactIsOriginal(contact.getId());
    }

    /** The {@code User} parameter is resolved before the body, so 401 wins over 400. */
    @Test
    void shouldReturnUnauthorizedWhenUpdateTokenIsMissingAndBodyIsInvalid() {
        Contact contact = givenStoredContact();

        assertUnauthorized(putContact(null, contact.getId(),
                ContactRequest.builder().firstName("").build()));

        assertStoredContactIsOriginal(contact.getId());
    }

    // ---------------------------------------------- update: validation

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void shouldReturnBadRequestAndKeepDataWhenUpdateRequestFailsValidation(ContactRequest invalid) {
        Contact contact = givenStoredContact();

        WebResponse<ContactResponse> response = putContact(VALID_TOKEN, contact.getId(), invalid)
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getData());
        assertNotNull(response.getErrors());
        assertStoredContactIsOriginal(contact.getId());
    }

    @Test
    void shouldReturnBadRequestAndKeepDataWhenUpdateBodyIsMalformedJson() {
        Contact contact = givenStoredContact();

        WebResponse<ContactResponse> response = putContact(VALID_TOKEN, contact.getId(), "{ \"firstName\": ")
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals("Malformed or missing request body", response.getErrors());
        assertStoredContactIsOriginal(contact.getId());
    }

    // ------------------------------------------------------------ helpers


    /** Sends PUT /api/contacts/{contactId}; the token header is omitted when {@code token} is null. */
    private RestTestClient.ResponseSpec putContact(String token, String contactId, Object body) {
        return client.put()
                .uri(CONTACTS_URI + "/{contactId}", contactId)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    if (token != null) {
                        headers.add(TOKEN_HEADER, token);
                    }
                })
                .body(body)
                .exchange();
    }

    /** Sends PUT, asserts HTTP 200, and returns the parsed response. */
    private WebResponse<ContactResponse> putOk(String contactId, Object body) {
        return putContact(ContactControllerTest.VALID_TOKEN, contactId, body)
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();
    }

    private static ContactRequest updateRequest() {
        return ContactRequest.builder()
                .firstName("Eko")
                .lastName("Kurniawan")
                .email("eko@example.com")
                .phone("089876543210")
                .build();
    }

    /** Persists the contact that update tests start from, owned by the primary test user. */
    private Contact givenStoredContact() {
        return saveContact(USERNAME, "Yoga", "Harisistianugroho", "yoga@example.com", "081234567890");
    }

    /** Verifies the stored columns; reads only scalar fields, so the lazy owner is never touched. */
    private void assertStoredContact(String id, String firstName, String lastName,
                                     String email, String phone) {
        Contact stored = contactRepository.findById(id).orElseThrow();
        assertEquals(firstName, stored.getFirstName());
        assertEquals(lastName, stored.getLastName());
        assertEquals(email, stored.getEmail());
        assertEquals(phone, stored.getPhone());
    }

    private void assertStoredContactIsOriginal(String id) {
        assertStoredContact(id, "Yoga", "Harisistianugroho", "yoga@example.com", "081234567890");
    }

    /** Sends POST /api/contacts; the token header is omitted when {@code token} is null. */
    private RestTestClient.ResponseSpec postContact(String token, Object body) {
        return client.post()
                .uri(CONTACTS_URI)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    if (token != null) {
                        headers.add(TOKEN_HEADER, token);
                    }
                })
                .body(body)
                .exchange();
    }

    /** Sends POST, asserts HTTP 200, and returns the parsed response. */
    private WebResponse<ContactResponse> postOk(String token, Object body) {
        return postContact(token, body)
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();
    }

    private void assertUnauthorized(RestTestClient.ResponseSpec spec) {
        WebResponse<ContactResponse> response = spec
                .expectStatus().isUnauthorized()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getData());
        assertEquals("Unauthorized", response.getErrors());
    }

    private void saveUser(String username, String name, String token, long expiredAt) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("bcrypt-hash");
        user.setName(name);
        user.setToken(token);
        user.setTokenExpiredAt(expiredAt);
        userRepository.save(user);
    }

    private static ContactRequest fullRequest() {
        return ContactRequest.builder()
                .firstName("Yoga")
                .lastName("Harisistianugroho")
                .email("yoga@example.com")
                .phone("081234567890")
                .build();
    }

    private static long futureExpiry() {
        return System.currentTimeMillis() + 60_000L;
    }

    /** Sends GET /api/contacts/{contactId}; the token header is omitted when {@code token} is null. */
    private RestTestClient.ResponseSpec getContact(String token, String contactId) {
        return client.get()
                .uri(CONTACTS_URI + "/{contactId}", contactId)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    if (token != null) {
                        headers.add(TOKEN_HEADER, token);
                    }
                })
                .exchange();
    }

    /** Sends GET, asserts HTTP 200, and returns the parsed response. */
    private WebResponse<ContactResponse> getContactOk(String token, String contactId) {
        return getContact(token, contactId)
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();
    }

    /** Asserts HTTP 404 with the standard error body and returns the parsed response. */
    private WebResponse<ContactResponse> assertNotFound(RestTestClient.ResponseSpec spec) {
        WebResponse<ContactResponse> response = spec
                .expectStatus().isNotFound()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getData());
        assertEquals("Contact is not found", response.getErrors());
        return response;
    }

    /** Persists a contact owned by an existing user and returns it with its generated id. */
    private Contact saveContact(String ownerUsername, String firstName, String lastName,
                                String email, String phone) {
        Contact contact = new Contact();
        contact.setFirstName(firstName);
        contact.setLastName(lastName);
        contact.setEmail(email);
        contact.setPhone(phone);
        contact.setUser(userRepository.findById(ownerUsername).orElseThrow());
        return contactRepository.save(contact);
    }
}