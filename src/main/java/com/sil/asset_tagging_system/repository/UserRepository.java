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

import java.util.Optional;


@Repository
public interface UserRepository
        extends JpaRepository<User, Long> {

    @EntityGraph(
            attributePaths = {
                    "department",
                    "roles"
            }
    )
    Optional<User> findByEmailIgnoreCase(
            String email
    );

    boolean existsByEmailIgnoreCase(
            String email
    );

    boolean existsByEmailIgnoreCaseAndIdNot(
            String email,
            Long id
    );


    @EntityGraph(
            attributePaths = {
                    "department",
                    "roles"
            }
    )
    @Query("""
            SELECT DISTINCT user
            FROM User user
            JOIN user.roles role
            WHERE user.id = :userId
              AND role.name = :roleName
            """)
    Optional<User> findByIdAndRoleName(

            @Param("userId")
            Long userId,

            @Param("roleName")
            RoleName roleName
    );

    @Query(
            value = """
        SELECT DISTINCT user
        FROM User user
        JOIN user.roles role
        JOIN FETCH user.department department
        WHERE role.name = :roleName
        
          AND (
                :search IS NULL
                OR LOWER(user.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(user.lastName)  LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(CONCAT(user.firstName, ' ', user.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(user.email)     LIKE LOWER(CONCAT('%', :search, '%'))
          )
        
          AND (
                :departmentId IS NULL
                OR department.id = :departmentId
          )
        
          AND (
                :enabled IS NULL
                OR user.enabled = :enabled
          )
    """,
            countQuery = """
        SELECT COUNT(DISTINCT user.id)
        FROM User user
        JOIN user.roles role
        JOIN user.department department
        WHERE role.name = :roleName
        
          AND (
                :search IS NULL
                OR LOWER(user.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(user.lastName)  LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(CONCAT(user.firstName, ' ', user.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(user.email)     LIKE LOWER(CONCAT('%', :search, '%'))
          )
        
          AND (
                :departmentId IS NULL
                OR department.id = :departmentId
          )
        
          AND (
                :enabled IS NULL
                OR user.enabled = :enabled
          )
    """
    )
    Page<User> findEmployees(
            @Param("roleName") RoleName roleName,
            @Param("search") String search,
            @Param("departmentId") Long departmentId,
            @Param("enabled") Boolean enabled,
            Pageable pageable
    );

}