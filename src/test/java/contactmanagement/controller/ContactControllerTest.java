package contactmanagement.controller;

import contactmanagement.entity.Contact;
import contactmanagement.entity.User;
import contactmanagement.model.ContactResponse;
import contactmanagement.model.CreateContactRequest;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
                CreateContactRequest.builder().firstName("Yoga").build());

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

        WebResponse<ContactResponse> response = postOk(VALID_TOKEN, CreateContactRequest.builder()
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
        assertUnauthorized(postContact(null, CreateContactRequest.builder().firstName("").build()));

        assertEquals(0, contactRepository.count());
    }

    // --------------------------------------------------------- validation

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void shouldReturnBadRequestAndSaveNothingWhenRequestFailsValidation(CreateContactRequest invalid) {
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
    static Stream<CreateContactRequest> invalidRequests() {
        String tooLong = "a".repeat(101);
        return Stream.of(
                CreateContactRequest.builder().firstName(null).build(),
                CreateContactRequest.builder().firstName("").build(),
                CreateContactRequest.builder().firstName("   ").build(),
                CreateContactRequest.builder().firstName(tooLong).build(),
                CreateContactRequest.builder().firstName("Yoga").lastName(tooLong).build(),
                CreateContactRequest.builder().firstName("Yoga").email("not-an-email").build(),
                CreateContactRequest.builder().firstName("Yoga").email("eko@").build(),
                CreateContactRequest.builder().firstName("Yoga").email("a".repeat(195) + "@x.com").build(),
                CreateContactRequest.builder().firstName("Yoga").phone(tooLong).build()
        );
    }

    // ------------------------------------------------------------ helpers

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

    private static CreateContactRequest fullRequest() {
        return CreateContactRequest.builder()
                .firstName("Yoga")
                .lastName("Harisistianugroho")
                .email("yoga@example.com")
                .phone("081234567890")
                .build();
    }

    private static long futureExpiry() {
        return System.currentTimeMillis() + 60_000L;
    }
}