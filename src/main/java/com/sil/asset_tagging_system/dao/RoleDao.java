package com.sil.asset_tagging_system.dao;

import com.sil.asset_tagging_system.model.Role;
import com.sil.asset_tagging_system.model.enums.RoleName;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RoleDao {

    EntityManager entityManager;
    RoleDao(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

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
        Number count = (Number) entityManager.createNativeQuery(sql)
                .setParameter("name", name.name())
                .getSingleResult();

        return count.longValue() > 0;
    }


}
