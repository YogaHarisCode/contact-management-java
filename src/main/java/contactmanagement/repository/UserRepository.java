package contactmanagement.repository;

import contactmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link User} entities.
 *
 * <p>The identifier type is {@code String} because the username is the primary key. The standard
 * CRUD methods inherited from {@link JpaRepository} are sufficient for the current features
 * (for example {@code existsById} and {@code saveAndFlush} used during registration).
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
}