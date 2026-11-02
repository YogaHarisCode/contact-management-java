package contactmanagement.service;

import contactmanagement.entity.Contact;
import contactmanagement.entity.User;
import contactmanagement.model.ContactResponse;
import contactmanagement.model.CreateContactRequest;
import contactmanagement.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService{

    private final ContactRepository contactRepository;

    @Override
    @Transactional
    public ContactResponse create(User user, CreateContactRequest request) {
        String username = user.getUsername();
        log.debug("Create contact started: username={}", username);

        Contact contact = new Contact();
        contact.setFirstName(request.getFirstName());
        contact.setLastName(request.getLastName());
        contact.setEmail(request.getEmail());
        contact.setPhone(request.getPhone());
        contact.setUser(user);

        Contact saved = contactRepository.save(contact);

        // Only identifiers are logged; name, email, and phone are personal data (PII).
        log.info("Contact created: contactId={}, username={}", saved.getId(), username);
        return toContactResponse(saved);
    }

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