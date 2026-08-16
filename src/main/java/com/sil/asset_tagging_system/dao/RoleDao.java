package com.sil.asset_tagging_system.dao;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;

import com.sil.asset_tagging_system.model.Role;
import com.sil.asset_tagging_system.model.enums.RoleName;

@Repository
public class RoleDao {

    EntityManager entityManager;
    RoleDao(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    public Optional<Role> findByName(RoleName name)
    {
        String sql = """
                SELECT * FROM roles WHERE name = :name
                """;

        List<Role> results = entityManager.createNativeQuery(sql, Role.class)
                .setParameter("name", name.name())
                .getResultList();

        return results.stream().findFirst();
    }

    public Boolean existsByName(RoleName name)
    {
        String sql = """
                SELECT COUNT(*) FROM roles WHERE name = :name
                """;
        return DaoUtils.exists(entityManager, sql, Map.of("name", name.name()));
    }

    @SuppressWarnings("unchecked")
    public List<Role> findAllRoles()
    {
        String sql = """
                SELECT id, name
                FROM roles
                """;
        return entityManager.createNativeQuery(sql,Role.class).getResultList();
    }


}
