package com.sil.asset_tagging_system.dao;

import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;

import com.sil.asset_tagging_system.model.Department;

@Repository
public class DepartmentDao {
    private final EntityManager entityManager;


    DepartmentDao(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    public Boolean existsByNameIgnoreCase(String name)
    {
        String sql = """
                SELECT COUNT(*) FROM departments WHERE LOWER(name) = LOWER(:name)
                """;
        return DaoUtils.exists(entityManager, sql, Map.of("name", name));
    }

    @SuppressWarnings("unchecked")
    public List<Department> findAllDepartments()
    {
        String sql = """
                SELECT id, name, closed_at
                FROM departments
                WHERE closed_at IS NULL
                """;

        List<Department> list = entityManager.createNativeQuery(sql,Department.class).getResultList();
        return list;
    }

}
