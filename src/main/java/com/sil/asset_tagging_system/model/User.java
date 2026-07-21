package com.sil.asset_tagging_system.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
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
        String first = firstName != null ? firstName : "";
        String last = lastName != null ? lastName : "";
        return (first + " " + last).trim();
    }

}