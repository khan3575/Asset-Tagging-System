package com.sil.asset_tagging_system.model;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(
            name = "first_name",
            nullable = false,
            length = 60
    )
    private String firstName;


    @Column(
            name = "last_name",
            nullable = false,
            length = 60
    )
    private String lastName;


    @Column(
            name = "email",
            nullable = false,
            unique = true,
            length = 100
    )
    private String email;


    @Column(
            name = "password",
            nullable = false,
            length = 255
    )
    private String password;


    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )

    @JoinColumn(
            name = "dept_id",
            nullable = false
    )
    private Department department;


    @Builder.Default

    @Column(
            name = "enabled",
            nullable = false
    )
    private Boolean enabled = true;


    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;


    @Builder.Default

    @ManyToMany(fetch = FetchType.LAZY)

    @JoinTable(
            name = "user_role",

            joinColumns = @JoinColumn(
                    name = "user_id"
            ),

            inverseJoinColumns = @JoinColumn(
                    name = "role_id"
            )
    )
    private Set<Role> roles = new HashSet<>();

    public void addRole(Role role) {

        roles.add(role);

    }

    public void removeRole(Role role) {

        roles.remove(role);

    }

    @Transient
    public String getFullName() {

        return firstName + " " + lastName;

    }

    public Boolean verifyPassword(String password)
    {
        return this.password.equals(password);
    }
}