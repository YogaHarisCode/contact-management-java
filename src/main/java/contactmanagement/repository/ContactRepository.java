package contactmanagement.repository;

import contactmanagement.entity.Contact;
import contactmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Contact} entities.
 *
 * <p>The identifier type is {@code String} because the contact id is a UUID string. Besides the
 * standard CRUD methods inherited from {@link JpaRepository}, it declares owner-scoped lookups,
 * so a contact is only reachable through the user who owns it.
 */
@Repository
public interface ContactRepository extends JpaRepository<Contact, String> {

    /**
     * Finds a contact by its id, but only if it belongs to the given user.
     *
     * <p>Owner and id are matched in one query ({@code WHERE username = ? AND id = ?}). This is
     * what prevents IDOR (Insecure Direct Object Reference): a contact that exists but belongs
     * to another user gives the same empty result as an id that does not exist, so callers
     * cannot tell the two cases apart. {@code id} is the primary key, so at most one row can
     * match.
     *
     * @param user the owner of the contact, usually the authenticated user
     * @param id   the contact id (UUID string)
     * @return the contact, or an empty {@link Optional} when no contact with this id exists for
     *         this user
     */
    Optional<Contact> findByUserAndId(User user, String id);
}