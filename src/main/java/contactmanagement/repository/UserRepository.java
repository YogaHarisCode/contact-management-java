package contactmanagement.repository;

import contactmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} entities.
 *
 * <p>The identifier type is {@code String} because the username is the primary key. The standard
 * CRUD methods inherited from {@link JpaRepository} cover registration and login (for example
 * {@code existsById}, {@code findById} and {@code saveAndFlush}); {@link #findFirstByToken(String)}
 * adds the lookup needed to authenticate requests by token.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Finds the user that currently holds the given authentication token.
     *
     * <p>Used to authenticate requests that carry the {@code X-API-TOKEN} header. The
     * {@code token} column has a unique constraint, so at most one user can match.
     *
     * @param token the authentication token issued at login
     * @return the matching user, or an empty {@link Optional} when no user holds this token
     */
    Optional<User> findFirstByToken(String token);
}