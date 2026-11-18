package com.yogaharis.contactmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Persistable;

/**
 * JPA entity mapped to the {@code users} table.
 *
 * <p>The username is assigned by the application and used as the primary key.
 * The entity implements {@link Persistable} so Spring Data can tell a new user
 * from an existing one without relying on a null ID.</p>
 *
 * @author Yoga Harisistianugroho
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
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