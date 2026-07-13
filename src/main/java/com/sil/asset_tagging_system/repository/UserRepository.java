package com.sil.asset_tagging_system.repository;

import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.RoleName;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(
            attributePaths = {
                    "roles",
                    "department"
            }
    )

    Optional<User> findByEmail(String email);

    @EntityGraph(
            attributePaths = {
                    "roles",
                    "department"
            }
    )

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Page<User> findByEnabled(
            Boolean enabled,
            Pageable pageable
    );

    Page<User> findByDepartmentId(
            Long departmentId,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT user
            FROM User user
            JOIN user.roles role
            WHERE role.name = :roleName
            """)

    Page<User> findByRoleName(
            @Param("roleName")
            RoleName roleName,

            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT user
            FROM User user
            JOIN user.roles role
            WHERE role.name =
                  com.sil.asset_tagging_system.model.enums.RoleName.ROLE_EMPLOYEE
            AND user.enabled = true
            ORDER BY user.firstName, user.lastName
            """)

    List<User> findAllEnabledEmployees();
    @Query("""
            SELECT DISTINCT user
            FROM User user
            WHERE
                LOWER(user.firstName)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))

                OR

                LOWER(user.lastName)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))

                OR

                LOWER(user.email)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)

    Page<User> searchUsers(
            @Param("keyword")
            String keyword,

            Pageable pageable
    );

}