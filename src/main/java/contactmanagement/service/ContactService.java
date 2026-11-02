package contactmanagement.service;

import contactmanagement.entity.User;
import contactmanagement.model.ContactResponse;
import contactmanagement.model.CreateContactRequest;

/**
 * Business operations related to contacts.
 */
public interface ContactService {

    /**
     * Creates a new contact owned by the given user.
     *
     * <p>The id is generated when the contact is persisted. The owner always comes from the
     * authenticated user, never from the request.
     *
     * @param user    the authenticated user, resolved from the {@code X-API-TOKEN} header
     * @param request the validated contact data
     * @return the created contact, including its generated id
     */
    ContactResponse create(User user, CreateContactRequest request);
}