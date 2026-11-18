package com.yogaharis.contactmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Persistable;

/**
 * JPA entity mapped to the {@code users} table.
 *
 * <p>The username is assigned by the application and used as the primary key.
 * The entity implements {@link Persistable} so Spring Data can tell a new user
 * from an existing one without relying on a null ID.</p>
 *
 * <p>Login state lives on the same row: {@code token} and {@code tokenExpiredAt} hold the
 * user's current API token and its expiry. The entity is annotated with {@link DynamicUpdate}
 * so Hibernate updates only the changed columns, and a login never overwrites other columns
 * with stale values.</p>
 *
 * @author Yoga Harisistianugroho
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
@DynamicUpdate
public class User implements Persistable<String> {

    /** Unique login name of the user. Also the primary key (max 100 characters). */
    @Id
    @Column(length = 100, nullable = false)
    private String username;

    /** BCrypt hash of the user's password. The raw password is never stored. */
    @Column(length = 100, nullable = false)
    private String password;

    /** Display name of the user (max 100 characters). */
    @Column(length = 100, nullable = false)
    private String name;

    /**
     * Current API token issued at login (a UUID string, max 100 characters).
     * Unique across users and {@code null} until the first successful login.
     * Each successful login replaces the previous value.
     */
    @Column(length = 100, unique = true)
    private String token;

    /**
     * Expiry time of {@code token} as epoch milliseconds.
     * {@code null} until the first successful login.
     */
    @Column(name = "token_expired_at")
    private Long tokenExpiredAt;

    /**
     * Not persisted. {@code true} until the entity has been saved or loaded.
     *
     * <p>Because the ID is assigned manually, Spring Data would otherwise treat every
     * non-null ID as an existing row and call {@code merge()} (extra SELECT, possible
     * silent overwrite). Reporting a new instance as new makes it use {@code persist()}.</p>
     */
    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean newEntity = true;

    /**
     * Returns the entity ID required by {@link Persistable}.
     *
     * @return the username, which serves as the entity ID
     */
    @Override
    public @Nullable String getId() {
        return getUsername();
    }

    /**
     * Tells Spring Data whether this instance has not been persisted yet.
     *
     * @return {@code true} if the entity is new, {@code false} after it was saved or loaded
     */
    @Override
    public boolean isNew() {
        return newEntity;
    }

    /**
     * JPA lifecycle callback that marks the entity as no longer new after it has
     * been persisted or loaded from the database.
     */
    @PostPersist
    @PostLoad
    void markNotNew(){
        newEntity = false;
    }
}