package contactmanagement.service;

import contactmanagement.entity.Contact;
import contactmanagement.entity.User;
import contactmanagement.model.ContactResponse;
import contactmanagement.model.CreateContactRequest;
import contactmanagement.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /** Repository used to persist contacts. */
    private final ContactRepository contactRepository;

    private static final String CONTACT_NOT_FOUND = "Contact is not found";

    /**
     * {@inheritDoc}
     *
     * <p>Only the contact id and the username are logged; name, email, and phone are personal
     * data and are never logged.
     */
    @Override
    @Transactional
    public ContactResponse create(User user, CreateContactRequest request) {
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

    @Override
    @Transactional(readOnly = true)
    public ContactResponse get(User user, String contactId) {
        Contact contact = contactRepository.findByUserAndId(user, contactId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, CONTACT_NOT_FOUND));

        return toContactResponse(contact);
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