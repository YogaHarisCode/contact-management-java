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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ContactServiceImpl} (create-contact), with the repository mocked.
 */
@ExtendWith(MockitoExtension.class)
class ContactServiceImplTest {

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private ContactServiceImpl contactService;

    // ------------------------------------------------------------ success

    @Test
    void shouldReturnContactResponseWhenDataIsValid() {
        givenSaveReturnsSameEntity();
        User user = authenticatedUser();

        ContactResponse response = contactService.create(user, fullRequest());

        assertNotNull(response);
        assertEquals("Eko", response.getFirstName());
        assertEquals("Khannedy", response.getLastName());
        assertEquals("eko@example.com", response.getEmail());
        assertEquals("081234567890", response.getPhone());
    }

    @Test
    void shouldReturnIdOfSavedEntityWhenContactIsCreated() {
        givenSaveReturnsSameEntity();

        ContactResponse response = contactService.create(authenticatedUser(), fullRequest());

        Contact saved = captureSavedContact();
        assertEquals(saved.getId(), response.getId());
    }

    @Test
    void shouldGenerateUuidAsIdWhenContactIsCreated() {
        givenSaveReturnsSameEntity();

        ContactResponse response = contactService.create(authenticatedUser(), fullRequest());

        assertNotNull(response.getId());
        assertDoesNotThrow(() -> UUID.fromString(response.getId()));
    }

    @Test
    void shouldGenerateDifferentIdForEachContact() {
        givenSaveReturnsSameEntity();
        User user = authenticatedUser();

        ContactResponse first = contactService.create(user, fullRequest());
        ContactResponse second = contactService.create(user, fullRequest());

        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void shouldAssignAuthenticatedUserToContactWhenContactIsCreated() {
        givenSaveReturnsSameEntity();
        User user = authenticatedUser();

        contactService.create(user, fullRequest());

        assertSame(user, captureSavedContact().getUser());
    }

    @Test
    void shouldMapAllRequestFieldsToEntityBeforeSaving() {
        givenSaveReturnsSameEntity();

        contactService.create(authenticatedUser(), fullRequest());

        Contact saved = captureSavedContact();
        assertEquals("Eko", saved.getFirstName());
        assertEquals("Khannedy", saved.getLastName());
        assertEquals("eko@example.com", saved.getEmail());
        assertEquals("081234567890", saved.getPhone());
    }

    @Test
    void shouldSaveContactWithNullOptionalFieldsWhenOnlyFirstNameIsProvided() {
        givenSaveReturnsSameEntity();
        CreateContactRequest request = CreateContactRequest.builder().firstName("Eko").build();

        ContactResponse response = contactService.create(authenticatedUser(), request);

        assertEquals("Eko", response.getFirstName());
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
        givenSaveReturnsSameEntity();

        contactService.create(authenticatedUser(), fullRequest());

        verify(contactRepository, times(1)).save(any(Contact.class));
    }

    @Test
    void shouldNotModifyAuthenticatedUserWhenContactIsCreated() {
        givenSaveReturnsSameEntity();
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

    // ------------------------------------------------------------ helpers

    /** Makes the mocked repository behave like a real one: it returns the entity it was given. */
    private void givenSaveReturnsSameEntity() {
        when(contactRepository.save(any(Contact.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    /** Captures the entity passed to {@code contactRepository.save(...)}. */
    private Contact captureSavedContact() {
        ArgumentCaptor<Contact> captor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(captor.capture());
        return captor.getValue();
    }

    private static CreateContactRequest fullRequest() {
        return CreateContactRequest.builder()
                .firstName("Eko")
                .lastName("Khannedy")
                .email("eko@example.com")
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
}