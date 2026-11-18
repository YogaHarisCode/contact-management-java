package com.yogaharis.contactmanagement.repository;

import com.yogaharis.contactmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link User} entities.
 *
 * <p>The entity ID is the username, so lookups such as {@code existsById} and
 * {@code findById} take a username.</p>
 *
 * @author Yoga Harisistianugroho
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
}