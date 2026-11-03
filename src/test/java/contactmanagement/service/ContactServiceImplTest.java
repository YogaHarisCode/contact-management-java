package contactmanagement.service;

import contactmanagement.entity.Contact;
import contactmanagement.entity.User;
import contactmanagement.model.ContactResponse;
import contactmanagement.model.CreateContactRequest;
import contactmanagement.repository.ContactRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ContactServiceImpl} (create-contact), with the repository mocked.
 */
@ExtendWith(MockitoExtension.class)
class ContactServiceImplTest {

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private ContactServiceImpl contactService;

    private static final String CONTACT_ID = "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d";
    
    private static final String NOT_FOUND_MESSAGE = "Contact is not found";

    // ------------------------------------------------------------ create

    @Test
    void shouldReturnContactResponseWhenDataIsValid() {
        givenSaveAssignsGeneratedId();
        User user = authenticatedUser();

        ContactResponse response = contactService.create(user, fullRequest());

        assertNotNull(response);
        assertEquals("Yoga", response.getFirstName());
        assertEquals("Harisistianugroho", response.getLastName());
        assertEquals("yoga@example.com", response.getEmail());
        assertEquals("081234567890", response.getPhone());
    }

    @Test
    void shouldReturnIdOfSavedEntityWhenContactIsCreated() {
        givenSaveAssignsGeneratedId();

        ContactResponse response = contactService.create(authenticatedUser(), fullRequest());

        Contact saved = captureSavedContact();
        assertEquals(saved.getId(), response.getId());
    }

    @Test
    void shouldReturnIdAssignedBySaveWhenContactIsCreated() {
        givenSaveAssignsGeneratedId();

        ContactResponse response = contactService.create(authenticatedUser(), fullRequest());

        assertNotNull(response.getId());
        assertEquals(captureSavedContact().getId(), response.getId());
    }

    @Test
    void shouldAssignAuthenticatedUserToContactWhenContactIsCreated() {
        givenSaveAssignsGeneratedId();
        User user = authenticatedUser();

        contactService.create(user, fullRequest());

        assertSame(user, captureSavedContact().getUser());
    }

    @Test
    void shouldMapAllRequestFieldsToEntityBeforeSaving() {
        givenSaveAssignsGeneratedId();

        contactService.create(authenticatedUser(), fullRequest());

        Contact saved = captureSavedContact();
        assertEquals("Yoga", saved.getFirstName());
        assertEquals("Harisistianugroho", saved.getLastName());
        assertEquals("yoga@example.com", saved.getEmail());
        assertEquals("081234567890", saved.getPhone());
    }

    @Test
    void shouldSaveContactWithNullOptionalFieldsWhenOnlyFirstNameIsProvided() {
        givenSaveAssignsGeneratedId();
        CreateContactRequest request = CreateContactRequest.builder().firstName("Yoga").build();

        ContactResponse response = contactService.create(authenticatedUser(), request);

        assertEquals("Yoga", response.getFirstName());
        assertNull(response.getLastName());
        assertNull(response.getEmail());
        assertNull(response.getPhone());

        Contact saved = captureSavedContact();
        assertNull(saved.getLastName());
        assertNull(saved.getEmail());
        assertNull(saved.getPhone());
    }

    @Test
    void shouldCallRepositorySaveExactlyOnceWhenContactIsCreated() {
        givenSaveAssignsGeneratedId();

        contactService.create(authenticatedUser(), fullRequest());

        verify(contactRepository, times(1)).save(any(Contact.class));
    }

    @Test
    void shouldNotModifyAuthenticatedUserWhenContactIsCreated() {
        givenSaveAssignsGeneratedId();
        User user = authenticatedUser();

        contactService.create(user, fullRequest());

        assertEquals("johndoe", user.getUsername());
        assertEquals("John Doe", user.getName());
        assertEquals("bcrypt-hash", user.getPassword());
        assertEquals("valid-token", user.getToken());
    }

    // ------------------------------------------------------------ failure

    @Test
    void shouldPropagateExceptionWhenRepositorySaveFails() {
        when(contactRepository.save(any(Contact.class)))
                .thenThrow(new DataIntegrityViolationException("save failed"));

        assertThrows(DataIntegrityViolationException.class,
                () -> contactService.create(authenticatedUser(), fullRequest()));
    }

    // ---------------------------------------------------------------- get

    @Test
    void shouldReturnContactResponseWhenContactBelongsToUser() {
        User user = authenticatedUser();
        when(contactRepository.findByUserAndId(user, CONTACT_ID))
                .thenReturn(Optional.of(storedContact(user)));

        ContactResponse response = contactService.get(user, CONTACT_ID);

        ContactResponse expected = ContactResponse.builder()
                .id(CONTACT_ID)
                .firstName("Yoga")
                .lastName("Harisistianugroho")
                .email("yoga@example.com")
                .phone("081234567890")
                .build();
        assertEquals(expected, response);
    }

    @Test
    void shouldReturnNullOptionalFieldsWhenStoredContactHasOnlyFirstName() {
        User user = authenticatedUser();
        Contact contact = new Contact();
        contact.setId(CONTACT_ID);
        contact.setFirstName("Yoga");
        contact.setUser(user);
        when(contactRepository.findByUserAndId(user, CONTACT_ID)).thenReturn(Optional.of(contact));

        ContactResponse response = contactService.get(user, CONTACT_ID);

        assertEquals("Yoga", response.getFirstName());
        assertNull(response.getLastName());
        assertNull(response.getEmail());
        assertNull(response.getPhone());
    }

    @Test
    void shouldQueryRepositoryWithAuthenticatedUserAndContactId() {
        User user = authenticatedUser();
        when(contactRepository.findByUserAndId(user, CONTACT_ID))
                .thenReturn(Optional.of(storedContact(user)));

        contactService.get(user, CONTACT_ID);

        verify(contactRepository, times(1)).findByUserAndId(user, CONTACT_ID);
    }

    @Test
    void shouldThrowNotFoundWhenContactDoesNotExist() {
        User user = authenticatedUser();
        when(contactRepository.findByUserAndId(user, CONTACT_ID)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> contactService.get(user, CONTACT_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals(NOT_FOUND_MESSAGE, exception.getReason());
    }

    /** IDOR (Insecure Direct Object Reference): the lookup is scoped to the caller, so another user's id is "not found". */
    @Test
    void shouldThrowNotFoundWhenContactBelongsToAnotherUser() {
        User intruder = new User();
        intruder.setUsername("janedoe");
        when(contactRepository.findByUserAndId(intruder, CONTACT_ID)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> contactService.get(intruder, CONTACT_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals(NOT_FOUND_MESSAGE, exception.getReason());
        verify(contactRepository).findByUserAndId(intruder, CONTACT_ID);
    }

    @Test
    void shouldNotSaveAnythingWhenGettingContact() {
        User user = authenticatedUser();
        when(contactRepository.findByUserAndId(user, CONTACT_ID))
                .thenReturn(Optional.of(storedContact(user)));

        contactService.get(user, CONTACT_ID);

        verify(contactRepository, never()).save(any(Contact.class));
    }

    @Test
    void shouldPropagateExceptionWhenRepositoryLookupFails() {
        User user = authenticatedUser();
        when(contactRepository.findByUserAndId(user, CONTACT_ID))
                .thenThrow(new IllegalStateException("database down"));

        assertThrows(IllegalStateException.class, () -> contactService.get(user, CONTACT_ID));
    }

    // ------------------------------------------------------------ helpers

    /** Simulates the repository: assigns an id like GenerationType.UUID does on persist. */
    private void givenSaveAssignsGeneratedId() {
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> {
            Contact contact = invocation.getArgument(0);
            contact.setId(UUID.randomUUID().toString());
            return contact;
        });
    }

    /** Captures the entity passed to {@code contactRepository.save(...)}. */
    private Contact captureSavedContact() {
        ArgumentCaptor<Contact> captor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(captor.capture());
        return captor.getValue();
    }

    private static CreateContactRequest fullRequest() {
        return CreateContactRequest.builder()
                .firstName("Yoga")
                .lastName("Harisistianugroho")
                .email("yoga@example.com")
                .phone("081234567890")
                .build();
    }

    private static User authenticatedUser() {
        User user = new User();
        user.setUsername("johndoe");
        user.setPassword("bcrypt-hash");
        user.setName("John Doe");
        user.setToken("valid-token");
        user.setTokenExpiredAt(System.currentTimeMillis() + 60_000L);
        return user;
    }

    /** A contact as it would be loaded from the database for the given owner. */
    private static Contact storedContact(User owner) {
        Contact contact = new Contact();
        contact.setId(CONTACT_ID);
        contact.setFirstName("Yoga");
        contact.setLastName("Harisistianugroho");
        contact.setEmail("yoga@example.com");
        contact.setPhone("081234567890");
        contact.setUser(owner);
        return contact;
    }
}