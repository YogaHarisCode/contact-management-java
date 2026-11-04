package contactmanagement.service;

import contactmanagement.entity.User;
import contactmanagement.model.ContactResponse;
import contactmanagement.model.ContactRequest;

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
    ContactResponse create(User user, ContactRequest request);

    /**
     * Returns a single contact of the given user.
     *
     * <p>Only contacts owned by {@code user} are visible. A contact that does not exist and a
     * contact owned by someone else are treated the same way, so the response never reveals
     * whether an id belongs to another user.
     *
     * @param user      the authenticated user, resolved from the {@code X-API-TOKEN} header
     * @param contactId the id (UUID string) of the contact to return
     * @return the contact, without any owner information
     * @throws org.springframework.web.server.ResponseStatusException with status 404 when no
     *         contact with this id exists for this user
     */
    ContactResponse get(User user, String contactId);

    ContactResponse update(User user, String contactId, ContactRequest request);
}