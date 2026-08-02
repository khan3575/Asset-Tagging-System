package com.sil.asset_tagging_system.dao;

import com.sil.asset_tagging_system.model.Department;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class DepartmentDao {
    private final EntityManager entityManager;


    DepartmentDao(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    public Optional<Department> findByName(String name)
    {
        String sql = """
                SELECT * FROM departments WHERE name = :name
                """;
        List<Department> results = entityManager.createNativeQuery(sql,Department.class)
                .setParameter("name", name)
                .getResultList();
        return results.stream().findFirst();
    }

    public Optional<Department> findByNameAndEnabled(String name, Boolean enabled)
    {
        String sql = """
                SELECT * FROM departments WHERE name = :name AND enabled = :enabled 
                """;
        List<Department> results = entityManager.createNativeQuery(sql,Department.class)
                .setParameter("name", name)
                .setParameter("enabled", enabled)
                .getResultList();
        return results.stream().findFirst();
    }

    public Boolean existsByNameIgnoreCase(String name)
    {
        String sql = """
                SELECT COUNT(*) FROM departments WHERE LOWER(name) = LOWER(:name)
                """;
        Number count = (Number) entityManager.createNativeQuery(sql)
                .setParameter("name", name)
                .getSingleResult();
        return count.longValue() > 0;
    }

}
