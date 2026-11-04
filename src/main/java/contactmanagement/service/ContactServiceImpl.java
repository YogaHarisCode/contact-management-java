package contactmanagement.service;

import contactmanagement.entity.Contact;
import contactmanagement.entity.User;
import contactmanagement.model.ContactResponse;
import contactmanagement.model.ContactRequest;
import contactmanagement.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Default {@link ContactService} implementation backed by Spring Data JPA.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService{

    /** Repository used to persist and load contacts. */
    private final ContactRepository contactRepository;

    /** Error message returned when a contact does not exist or belongs to another user. */
    private static final String CONTACT_NOT_FOUND = "Contact is not found";

    /**
     * {@inheritDoc}
     *
     * <p>Only the contact id and the username are logged; name, email, and phone are personal
     * data and are never logged.
     */
    @Override
    @Transactional
    public ContactResponse create(User user, ContactRequest request) {
        String username = user.getUsername();
        log.debug("Create contact started: username={}", username);

        Contact contact = new Contact();
        // The id is not set here: Hibernate generates the UUID when the entity is persisted.
        contact.setFirstName(request.getFirstName());
        contact.setLastName(request.getLastName());
        contact.setEmail(request.getEmail());
        contact.setPhone(request.getPhone());
        // The owner is the authenticated user, never a value taken from the request body.
        contact.setUser(user);

        // Use the returned entity: it is the instance that carries the generated id.
        Contact saved = contactRepository.save(contact);

        // Only identifiers are logged; name, email, and phone are personal data (PII).
        log.info("Contact created: contactId={}, username={}", saved.getId(), username);
        return toContactResponse(saved);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The lookup is a single query on owner and id. It runs in a read-only transaction and
     * changes nothing. Reads are logged at DEBUG only; a rejected lookup is logged at WARN with
     * identifiers only, never with personal data.
     */
    @Override
    @Transactional(readOnly = true)
    public ContactResponse get(User user, String contactId) {
        String username = user.getUsername();
        log.debug("Get contact started: contactId={}, username={}", contactId, username);

        Contact contact = findOwnedContact(user, contactId);

        log.debug("Contact retrieved: contactId={}, username={}", contactId, username);
        return toContactResponse(contact);
    }

    @Override
    @Transactional
    public ContactResponse update(User user, String contactId, ContactRequest request) {
        Contact contact = findOwnedContact(user, contactId);

        contact.setFirstName(request.getFirstName());
        contact.setLastName(request.getLastName());
        // hasText is null-safe, and blank email is stored as null.
        contact.setEmail(StringUtils.hasText(request.getEmail()) ? request.getEmail() : null);
        contact.setPhone(request.getPhone());

        return toContactResponse(contactRepository.save(contact));
    }

    private Contact findOwnedContact(User user, String contactId){
        return contactRepository.findByUserAndId(user, contactId)
                .orElseThrow(() -> {
                    log.warn("Contact not found: contactId={}, username={}",
                            contactId, user.getUsername());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, CONTACT_NOT_FOUND);
                });
    }

    /**
     * Maps an entity to its public representation, leaving out the owner.
     *
     * @param contact the persisted contact
     * @return the response DTO
     */
    private ContactResponse toContactResponse(Contact contact){
        return ContactResponse.builder()
                .id(contact.getId())
                .firstName(contact.getFirstName())
                .lastName(contact.getLastName())
                .email(contact.getEmail())
                .phone(contact.getPhone())
                .build();
    }
}