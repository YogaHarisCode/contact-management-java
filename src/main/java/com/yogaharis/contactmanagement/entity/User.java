package com.yogaharis.contactmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Persistable;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User implements Persistable<String> {

    @Id
    @Column(length = 100, nullable = false)
    private String username;

    @Column(length = 100, nullable = false)
    private String password;

    @Column(length = 100, nullable = false)
    private String name;

    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean newEntity = true;

    @Override
    public @Nullable String getId() {
        return getUsername();
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostPersist
    @PostLoad
    void markNotNew(){
        newEntity = false;
    }
}
