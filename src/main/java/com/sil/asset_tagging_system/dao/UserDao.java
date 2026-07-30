package com.sil.asset_tagging_system.dao;

import com.sil.asset_tagging_system.model.Department;
import com.sil.asset_tagging_system.model.Role;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.RoleName;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Repository
public class UserDao {

    private final EntityManager entityManager;

    UserDao(EntityManager entityManager)
    {
        this.entityManager= entityManager;
    }

    public Optional<User> findByEmailIgnoreCase(String email)
    {
        String sql = """
                SELECT u.id , u.first_name, u.last_name, u.email, u.password, u.enabled, u.created_at, d.id as dept_id
                       ,d.name as dept_name, d.enabled as dept_enabled
                FROM users u
                JOIN departments d
                ON u.dept_id = d.id
                WHERE LOWER(email) = LOWER(:email)
                """;
        List<Object[]> userRows = entityManager.createNativeQuery(sql)
                .setParameter("email", email)
                .getResultList();

        if(userRows.isEmpty())
        {
            return Optional.empty();
        }
        Object[] rows = userRows.getFirst();
        Long userId = ((Number) rows[0]).longValue();
        Department dept = Department.builder().id(((Number) rows[7]).longValue())
                .name((String) rows[8]).enabled((Boolean)rows[9]).build();
        User user = User.builder().id(userId)
                .firstName((String)rows[1])
                .lastName((String)rows[2])
                .email((String) rows[3])
                .password((String) rows[4])
                .department(dept)
                .enabled((Boolean) rows[5])
                .createdAt((LocalDateTime)rows[6])
                .roles(new HashSet<>(findRolesForUser(userId)))
                .build();

        return Optional.of(user);
    }

    public List<Role> findRolesForUser(Long userId)
    {
        String sql = """
                SELECT r.id , r.name
                FROM roles r
                JOIN user_role u ON r.id = u.role_id
                WHERE u.user_id = :userId
                """;
        List<Object[]> roleRows = entityManager.createNativeQuery(sql)
                .setParameter("userId", userId).getResultList();

        List<Role> roles = new ArrayList<>();
        for(Object[] rows : roleRows)
        {
            Role role = Role.builder().id(((Number)rows[0]).longValue())
                    .name(RoleName.valueOf((String)rows[1])).build();
            roles.add(role);
        }
        return roles;
    }


}
