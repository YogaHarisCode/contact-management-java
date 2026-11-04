package contactmanagement.controller;

import contactmanagement.entity.User;
import contactmanagement.model.ContactResponse;
import contactmanagement.model.ContactRequest;
import contactmanagement.model.WebResponse;
import contactmanagement.repository.UserRepository;
import contactmanagement.service.ContactService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Web-layer tests for {@code POST /api/contacts}.
 *
 * <p>Runs the real {@link ContactController}, argument resolver, and exception handler, with
 * {@link ContactService} and {@link UserRepository} mocked, so no database is needed.
 */
@WebMvcTest(ContactController.class)
@AutoConfigureRestTestClient
class ContactControllerWebMvcTest {

    private static final String CONTACTS_URI = "/api/contacts";
    private static final String TOKEN_HEADER = "X-API-TOKEN";
    private static final String TOKEN = "3c829e12-4217-48f5-a169-715a31bc0d6d";
    private static final String CONTACT_ID = "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d";

    @Autowired
    private RestTestClient client;

    @MockitoBean
    private ContactService contactService;

    // The real UserArgumentResolver is loaded by @WebMvcTest and needs this repository.
    @MockitoBean
    private UserRepository userRepository;

    // ------------------------------------------------------------ success

    @ParameterizedTest
    @ValueSource(strings = {"081234567890", "6281234567890", "+6281234567890"})
    void shouldReturnOkWhenPhoneFormatIsValid(String phone) {
        User user = givenAuthenticatedUser();
        ContactRequest request = ContactRequest.builder()
                .firstName("Yoga").phone(phone).build();
        when(contactService.create(user, request)).thenReturn(
                ContactResponse.builder().id(CONTACT_ID).firstName("Yoga").phone(phone).build());

        postContact(TOKEN, request).expectStatus().isOk();
    }

    @Test
    void shouldReturnCreatedContactWhenRequestIsValid() {
        User user = givenAuthenticatedUser();
        ContactRequest request = fullRequest();
        when(contactService.create(user, request)).thenReturn(fullResponse());

        WebResponse<ContactResponse> response = postContact(TOKEN, request)
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getErrors());
        assertEquals(CONTACT_ID, response.getData().getId());
        assertEquals("Yoga", response.getData().getFirstName());
        assertEquals("Harisistianugroho", response.getData().getLastName());
        assertEquals("yoga@example.com", response.getData().getEmail());
        assertEquals("081234567890", response.getData().getPhone());
    }

    @Test
    void shouldPassAuthenticatedUserAndRequestToServiceWhenRequestIsValid() {
        User user = givenAuthenticatedUser();
        ContactRequest request = fullRequest();
        when(contactService.create(user, request)).thenReturn(fullResponse());

        postContact(TOKEN, request).expectStatus().isOk();

        verify(contactService).create(user, request);
    }

    @Test
    void shouldReturnOkWhenOnlyFirstNameIsProvided() {
        User user = givenAuthenticatedUser();
        ContactRequest request = ContactRequest.builder().firstName("Yoga").build();
        when(contactService.create(user, request)).thenReturn(
                ContactResponse.builder().id(CONTACT_ID).firstName("Yoga").build());

        WebResponse<ContactResponse> response = postContact(TOKEN, request)
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals("Yoga", response.getData().getFirstName());
        verify(contactService).create(user, request);
    }

    @Test
    void shouldReturnOkWhenValuesAreOnLengthBoundaries() {
        User user = givenAuthenticatedUser();
        String of100 = "a".repeat(100);
        ContactRequest request = ContactRequest.builder()
                .firstName(of100)
                .lastName(of100)
//                .phone(of100)
                .build();
        when(contactService.create(user, request)).thenReturn(
                ContactResponse.builder().id(CONTACT_ID).firstName(of100).build());

        postContact(TOKEN, request).expectStatus().isOk();

        verify(contactService).create(user, request);
    }

    @Test
    void shouldNotExposeTokenOrPasswordWhenContactIsCreated() {
        User user = givenAuthenticatedUser();
        ContactRequest request = fullRequest();
        when(contactService.create(user, request)).thenReturn(fullResponse());

        String rawBody = postContact(TOKEN, request)
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(rawBody);
        assertFalse(rawBody.contains("password"));
        assertFalse(rawBody.contains("bcrypt-hash"));
        assertFalse(rawBody.contains("token"));
        assertFalse(rawBody.contains(TOKEN));
    }

    // ------------------------------------------------------- unauthorized

    @Test
    void shouldReturnUnauthorizedWhenTokenHeaderIsMissing() {
        assertUnauthorized(postContact(null, fullRequest()));

        verifyNoInteractions(userRepository, contactService);
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenHeaderIsEmpty() {
        assertUnauthorized(postContact("", fullRequest()));

        verifyNoInteractions(userRepository, contactService);
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenIsNotFound() {
        when(userRepository.findFirstByToken("unknown-token")).thenReturn(Optional.empty());

        assertUnauthorized(postContact("unknown-token", fullRequest()));

        verifyNoInteractions(contactService);
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenIsExpired() {
        User expired = validUser();
        expired.setTokenExpiredAt(System.currentTimeMillis() - 1_000L);
        when(userRepository.findFirstByToken(TOKEN)).thenReturn(Optional.of(expired));

        assertUnauthorized(postContact(TOKEN, fullRequest()));

        verifyNoInteractions(contactService);
    }

    /** The {@code User} parameter is resolved before the body, so 401 wins over 400. */
    @Test
    void shouldReturnUnauthorizedWhenTokenIsMissingAndBodyIsInvalid() {
        ContactRequest invalid = ContactRequest.builder().firstName("").build();

        assertUnauthorized(postContact(null, invalid));

        verifyNoInteractions(contactService);
    }

    // --------------------------------------------------------- validation

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void shouldReturnBadRequestWhenRequestFailsValidation(ContactRequest invalid) {
        givenAuthenticatedUser();

        WebResponse<ContactResponse> response = postContact(TOKEN, invalid)
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getData());
        assertNotNull(response.getErrors());
        verifyNoInteractions(contactService);
    }

    @Test
    void shouldMentionFirstNameInErrorWhenFirstNameIsBlank() {
        givenAuthenticatedUser();

        WebResponse<ContactResponse> response = postContact(TOKEN,
                ContactRequest.builder().firstName("   ").build())
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertTrue(response.getErrors().toString().contains("firstName"));
    }

    @Test
    void shouldMentionEmailInErrorWhenEmailFormatIsInvalid() {
        givenAuthenticatedUser();

        WebResponse<ContactResponse> response = postContact(TOKEN,
                ContactRequest.builder().firstName("Yoga").email("not-an-email").build())
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertTrue(response.getErrors().toString().contains("email"));
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsMalformedJson() {
        givenAuthenticatedUser();

        WebResponse<ContactResponse> response = postContact(TOKEN, "{ \"firstName\": ")
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals("Malformed or missing request body", response.getErrors());
        verifyNoInteractions(contactService);
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsMissing() {
        givenAuthenticatedUser();

        WebResponse<ContactResponse> response = client.post()
                .uri(CONTACTS_URI)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.add(TOKEN_HEADER, TOKEN))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals("Malformed or missing request body", response.getErrors());
        verifyNoInteractions(contactService);
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
                ContactRequest.builder().firstName("Yoga").email("yoga@").build(),
                ContactRequest.builder().firstName("Yoga").email("a".repeat(195) + "@x.com").build(),
                ContactRequest.builder().firstName("Yoga").phone(tooLong).build(),
                ContactRequest.builder().firstName("Yoga").phone("abc").build(),
                ContactRequest.builder().firstName("Yoga").phone("0812345").build(),
                ContactRequest.builder().firstName("Yoga").phone("081" + "2".repeat(11)).build(),
                ContactRequest.builder().firstName("Yoga").phone("0812-3456-7890").build()
        );
    }

    // ====================================================== GET CONTACT

    @Test
    void shouldReturnContactWhenTokenIsValidAndContactExists() {
        User user = givenAuthenticatedUser();
        when(contactService.get(user, CONTACT_ID)).thenReturn(fullResponse());

        WebResponse<ContactResponse> response = getContact(TOKEN)
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getErrors());
        assertEquals(CONTACT_ID, response.getData().getId());
        assertEquals("Yoga", response.getData().getFirstName());
        assertEquals("Harisistianugroho", response.getData().getLastName());
        assertEquals("yoga@example.com", response.getData().getEmail());
        assertEquals("081234567890", response.getData().getPhone());
    }

    @Test
    void shouldPassAuthenticatedUserAndContactIdToServiceWhenGettingContact() {
        User user = givenAuthenticatedUser();
        when(contactService.get(user, CONTACT_ID)).thenReturn(fullResponse());

        getContact(TOKEN).expectStatus().isOk();

        verify(contactService).get(user, CONTACT_ID);
    }

    @Test
    void shouldOmitNullOptionalFieldsWhenGettingContactWithOnlyFirstName() {
        User user = givenAuthenticatedUser();
        when(contactService.get(user, CONTACT_ID)).thenReturn(
                ContactResponse.builder().id(CONTACT_ID).firstName("Yoga").build());

        String rawBody = getContact(TOKEN)
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(rawBody);
        assertTrue(rawBody.contains("firstName"));
        assertFalse(rawBody.contains("lastName"));
        assertFalse(rawBody.contains("email"));
        assertFalse(rawBody.contains("phone"));
    }

    @Test
    void shouldNotExposeTokenOrPasswordWhenGettingContact() {
        User user = givenAuthenticatedUser();
        when(contactService.get(user, CONTACT_ID)).thenReturn(fullResponse());

        String rawBody = getContact(TOKEN)
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(rawBody);
        assertFalse(rawBody.contains("password"));
        assertFalse(rawBody.contains("bcrypt-hash"));
        assertFalse(rawBody.contains("token"));
        assertFalse(rawBody.contains(TOKEN));
    }

    @Test
    void shouldReturnNotFoundWhenServiceCannotFindContact() {
        User user = givenAuthenticatedUser();
        when(contactService.get(user, CONTACT_ID))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact is not found"));

        WebResponse<ContactResponse> response = getContact(TOKEN)
                .expectStatus().isNotFound()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getData());
        assertEquals("Contact is not found", response.getErrors());
    }

    @Test
    void shouldReturnUnauthorizedWhenGetContactTokenHeaderIsMissing() {
        assertUnauthorized(getContact(null));

        verifyNoInteractions(userRepository, contactService);
    }

    @Test
    void shouldReturnUnauthorizedWhenGetContactTokenHeaderIsEmpty() {
        assertUnauthorized(getContact(""));

        verifyNoInteractions(userRepository, contactService);
    }

    @Test
    void shouldReturnUnauthorizedWhenGetContactTokenIsNotFound() {
        when(userRepository.findFirstByToken("unknown-token")).thenReturn(Optional.empty());

        assertUnauthorized(getContact("unknown-token"));

        verifyNoInteractions(contactService);
    }

    @Test
    void shouldReturnUnauthorizedWhenGetContactTokenIsExpired() {
        User expired = validUser();
        expired.setTokenExpiredAt(System.currentTimeMillis() - 1_000L);
        when(userRepository.findFirstByToken(TOKEN)).thenReturn(Optional.of(expired));

        assertUnauthorized(getContact(TOKEN));

        verifyNoInteractions(contactService);
    }

    // ====================================================== UPDATE CONTACT

    @Test
    void shouldReturnUpdatedContactWhenRequestIsValid() {
        User user = givenAuthenticatedUser();
        ContactRequest request = updateRequest();
        when(contactService.update(user, CONTACT_ID, request)).thenReturn(updatedResponse());

        WebResponse<ContactResponse> response = putContact(TOKEN, request)
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getErrors());
        assertEquals(CONTACT_ID, response.getData().getId());
        assertEquals("Eko", response.getData().getFirstName());
        assertEquals("Kurniawan", response.getData().getLastName());
        assertEquals("eko@example.com", response.getData().getEmail());
        assertEquals("089876543210", response.getData().getPhone());
    }

    @Test
    void shouldPassAuthenticatedUserContactIdAndRequestToServiceWhenUpdatingContact() {
        User user = givenAuthenticatedUser();
        ContactRequest request = updateRequest();
        when(contactService.update(user, CONTACT_ID, request)).thenReturn(updatedResponse());

        putContact(TOKEN, request).expectStatus().isOk();

        verify(contactService).update(user, CONTACT_ID, request);
    }

    @ParameterizedTest
    @ValueSource(strings = {"081234567890", "6281234567890", "+6281234567890"})
    void shouldReturnOkWhenUpdatingWithValidPhoneFormat(String phone) {
        User user = givenAuthenticatedUser();
        ContactRequest request = ContactRequest.builder().firstName("Eko").phone(phone).build();
        when(contactService.update(user, CONTACT_ID, request)).thenReturn(
                ContactResponse.builder().id(CONTACT_ID).firstName("Eko").phone(phone).build());

        putContact(TOKEN, request).expectStatus().isOk();
    }

    @Test
    void shouldReturnOkWhenUpdatingWithOnlyFirstName() {
        User user = givenAuthenticatedUser();
        ContactRequest request = ContactRequest.builder().firstName("Eko").build();
        when(contactService.update(user, CONTACT_ID, request)).thenReturn(
                ContactResponse.builder().id(CONTACT_ID).firstName("Eko").build());

        String rawBody = putContact(TOKEN, request)
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(rawBody);
        assertTrue(rawBody.contains("firstName"));
        assertFalse(rawBody.contains("lastName"));
        assertFalse(rawBody.contains("email"));
        assertFalse(rawBody.contains("phone"));
    }

    /** Id di body tidak boleh dipakai: hanya id dari path yang sampai ke service. */
    @Test
    void shouldIgnoreIdInBodyAndUseIdFromPathWhenUpdatingContact() {
        User user = givenAuthenticatedUser();
        ContactRequest expected = ContactRequest.builder().firstName("Eko").build();
        when(contactService.update(user, CONTACT_ID, expected)).thenReturn(
                ContactResponse.builder().id(CONTACT_ID).firstName("Eko").build());

        putContact(TOKEN, "{\"id\":\"other-id\",\"firstName\":\"Eko\"}")
                .expectStatus().isOk();

        verify(contactService).update(user, CONTACT_ID, expected);
    }

    @Test
    void shouldNotExposeTokenOrPasswordWhenContactIsUpdated() {
        User user = givenAuthenticatedUser();
        ContactRequest request = updateRequest();
        when(contactService.update(user, CONTACT_ID, request)).thenReturn(updatedResponse());

        String rawBody = putContact(TOKEN, request)
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(rawBody);
        assertFalse(rawBody.contains("password"));
        assertFalse(rawBody.contains("bcrypt-hash"));
        assertFalse(rawBody.contains("token"));
        assertFalse(rawBody.contains(TOKEN));
    }

    @Test
    void shouldReturnNotFoundWhenServiceCannotFindContactToUpdate() {
        User user = givenAuthenticatedUser();
        ContactRequest request = updateRequest();
        when(contactService.update(user, CONTACT_ID, request))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact is not found"));

        WebResponse<ContactResponse> response = putContact(TOKEN, request)
                .expectStatus().isNotFound()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getData());
        assertEquals("Contact is not found", response.getErrors());
    }

    // ---------------------------------------------- update: unauthorized

    @Test
    void shouldReturnUnauthorizedWhenUpdateTokenHeaderIsMissing() {
        assertUnauthorized(putContact(null, updateRequest()));

        verifyNoInteractions(userRepository, contactService);
    }

    @Test
    void shouldReturnUnauthorizedWhenUpdateTokenHeaderIsEmpty() {
        assertUnauthorized(putContact("", updateRequest()));

        verifyNoInteractions(userRepository, contactService);
    }

    @Test
    void shouldReturnUnauthorizedWhenUpdateTokenIsNotFound() {
        when(userRepository.findFirstByToken("unknown-token")).thenReturn(Optional.empty());

        assertUnauthorized(putContact("unknown-token", updateRequest()));

        verifyNoInteractions(contactService);
    }

    @Test
    void shouldReturnUnauthorizedWhenUpdateTokenIsExpired() {
        User expired = validUser();
        expired.setTokenExpiredAt(System.currentTimeMillis() - 1_000L);
        when(userRepository.findFirstByToken(TOKEN)).thenReturn(Optional.of(expired));

        assertUnauthorized(putContact(TOKEN, updateRequest()));

        verifyNoInteractions(contactService);
    }

    /** The {@code User} parameter is resolved before the body, so 401 wins over 400. */
    @Test
    void shouldReturnUnauthorizedWhenUpdateTokenIsMissingAndBodyIsInvalid() {
        ContactRequest invalid = ContactRequest.builder().firstName("").build();

        assertUnauthorized(putContact(null, invalid));

        verifyNoInteractions(contactService);
    }

    // ---------------------------------------------- update: validation

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void shouldReturnBadRequestWhenUpdateRequestFailsValidation(ContactRequest invalid) {
        givenAuthenticatedUser();

        WebResponse<ContactResponse> response = putContact(TOKEN, invalid)
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertNull(response.getData());
        assertNotNull(response.getErrors());
        verifyNoInteractions(contactService);
    }

    @Test
    void shouldMentionFirstNameInErrorWhenUpdatingWithBlankFirstName() {
        givenAuthenticatedUser();

        WebResponse<ContactResponse> response = putContact(TOKEN,
                ContactRequest.builder().firstName("   ").build())
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertTrue(response.getErrors().toString().contains("firstName"));
    }

    @Test
    void shouldReturnPhoneMessageWhenUpdatingWithInvalidPhone() {
        givenAuthenticatedUser();

        WebResponse<ContactResponse> response = putContact(TOKEN,
                ContactRequest.builder().firstName("Eko").phone("0812-3456-7890").build())
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals("phone must be a valid Indonesian mobile number", response.getErrors());
    }

    @Test
    void shouldReturnBadRequestWhenUpdateBodyIsMalformedJson() {
        givenAuthenticatedUser();

        WebResponse<ContactResponse> response = putContact(TOKEN, "{ \"firstName\": ")
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals("Malformed or missing request body", response.getErrors());
        verifyNoInteractions(contactService);
    }

    @Test
    void shouldReturnBadRequestWhenUpdateBodyIsMissing() {
        givenAuthenticatedUser();

        WebResponse<ContactResponse> response = client.put()
                .uri(CONTACTS_URI + "/{contactId}", CONTACT_ID)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.add(TOKEN_HEADER, TOKEN))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals("Malformed or missing request body", response.getErrors());
        verifyNoInteractions(contactService);
    }

    // ------------------------------------------------------------ helpers


    /** Sends PUT /api/contacts/{contactId}; the token header is omitted when {@code token} is null. */
    private RestTestClient.ResponseSpec putContact(String token, Object body) {
        return client.put()
                .uri(CONTACTS_URI + "/{contactId}", ContactControllerWebMvcTest.CONTACT_ID)
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

    private static ContactRequest updateRequest() {
        return ContactRequest.builder()
                .firstName("Eko")
                .lastName("Kurniawan")
                .email("eko@example.com")
                .phone("089876543210")
                .build();
    }

    private static ContactResponse updatedResponse() {
        return ContactResponse.builder()
                .id(CONTACT_ID)
                .firstName("Eko")
                .lastName("Kurniawan")
                .email("eko@example.com")
                .phone("089876543210")
                .build();
    }

    /** Stubs the repository so the resolver authenticates {@link #TOKEN} as {@code johndoe}. */
    private User givenAuthenticatedUser() {
        User user = validUser();
        when(userRepository.findFirstByToken(TOKEN)).thenReturn(Optional.of(user));
        return user;
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

    private static ContactRequest fullRequest() {
        return ContactRequest.builder()
                .firstName("Yoga")
                .lastName("Harisistianugroho")
                .email("yoga@example.com")
                .phone("081234567890")
                .build();
    }

    private static ContactResponse fullResponse() {
        return ContactResponse.builder()
                .id(CONTACT_ID)
                .firstName("Yoga")
                .lastName("Harisistianugroho")
                .email("yoga@example.com")
                .phone("081234567890")
                .build();
    }

    private static User validUser() {
        User user = new User();
        user.setUsername("johndoe");
        user.setPassword("bcrypt-hash");
        user.setName("John Doe");
        user.setToken(TOKEN);
        user.setTokenExpiredAt(System.currentTimeMillis() + 60_000L);
        return user;
    }

    /** Sends GET /api/contacts/{contactId}; the token header is omitted when {@code token} is null. */
    private RestTestClient.ResponseSpec getContact(String token) {
        return client.get()
                .uri(CONTACTS_URI + "/{contactId}", ContactControllerWebMvcTest.CONTACT_ID)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    if (token != null) {
                        headers.add(TOKEN_HEADER, token);
                    }
                })
                .exchange();
    }
}