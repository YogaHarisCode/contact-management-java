package contactmanagement.controller;

import contactmanagement.entity.User;
import contactmanagement.model.ContactResponse;
import contactmanagement.model.CreateContactRequest;
import contactmanagement.model.WebResponse;
import contactmanagement.repository.UserRepository;
import contactmanagement.service.ContactService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

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

    @Test
    void shouldReturnCreatedContactWhenRequestIsValid() {
        User user = givenAuthenticatedUser();
        CreateContactRequest request = fullRequest();
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
        CreateContactRequest request = fullRequest();
        when(contactService.create(user, request)).thenReturn(fullResponse());

        postContact(TOKEN, request).expectStatus().isOk();

        verify(contactService).create(user, request);
    }

    @Test
    void shouldReturnOkWhenOnlyFirstNameIsProvided() {
        User user = givenAuthenticatedUser();
        CreateContactRequest request = CreateContactRequest.builder().firstName("Eko").build();
        when(contactService.create(user, request)).thenReturn(
                ContactResponse.builder().id(CONTACT_ID).firstName("Eko").build());

        WebResponse<ContactResponse> response = postContact(TOKEN, request)
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<ContactResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals("Eko", response.getData().getFirstName());
        verify(contactService).create(user, request);
    }

    @Test
    void shouldReturnOkWhenValuesAreOnLengthBoundaries() {
        User user = givenAuthenticatedUser();
        String of100 = "a".repeat(100);
        CreateContactRequest request = CreateContactRequest.builder()
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
        CreateContactRequest request = fullRequest();
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
        CreateContactRequest invalid = CreateContactRequest.builder().firstName("").build();

        assertUnauthorized(postContact(null, invalid));

        verifyNoInteractions(contactService);
    }

    // --------------------------------------------------------- validation

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void shouldReturnBadRequestWhenRequestFailsValidation(CreateContactRequest invalid) {
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
                CreateContactRequest.builder().firstName("   ").build())
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
                CreateContactRequest.builder().firstName("Eko").email("not-an-email").build())
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
    static Stream<CreateContactRequest> invalidRequests() {
        String tooLong = "a".repeat(101);
        return Stream.of(
                CreateContactRequest.builder().firstName(null).build(),
                CreateContactRequest.builder().firstName("").build(),
                CreateContactRequest.builder().firstName("   ").build(),
                CreateContactRequest.builder().firstName(tooLong).build(),
                CreateContactRequest.builder().firstName("Eko").lastName(tooLong).build(),
                CreateContactRequest.builder().firstName("Eko").email("not-an-email").build(),
                CreateContactRequest.builder().firstName("Eko").email("eko@").build(),
                CreateContactRequest.builder().firstName("Eko").email("a".repeat(195) + "@x.com").build(),
                CreateContactRequest.builder().firstName("Eko").phone(tooLong).build()
        );
    }

    // ------------------------------------------------------------ helpers

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

    private static CreateContactRequest fullRequest() {
        return CreateContactRequest.builder()
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
}