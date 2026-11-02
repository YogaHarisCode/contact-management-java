package contactmanagement.repository;

import contactmanagement.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Contact} entities.
 *
 * <p>The identifier type is {@code String} because the contact id is a UUID string. Only the
 * standard CRUD methods inherited from {@link JpaRepository} are used so far (create contact
 * uses {@code save}).
 */
@Repository
public interface ContactRepository extends JpaRepository<Contact, String> {
}