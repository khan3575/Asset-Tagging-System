package com.sil.asset_tagging_system.dao;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;

import com.sil.asset_tagging_system.model.AssetCategory;

@Repository

public class AssetCategoryDao {
    private final EntityManager entityManager;
    public AssetCategoryDao(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }
    public Optional<AssetCategory> findByNameIgnoreCase(String name)
    {
        String query = """
                SELECT id, name, depreciation_rate_percentage
                FROM asset_categories
                WHERE LOWER(name) = LOWER(:name)
                """;
        return entityManager.createNativeQuery(query)
                .setParameter("name", name)
                .getResultStream().findFirst();           
    }

    public boolean existsByNameIgnoreCase(String name)
    {
        String sql = """
                SELECT COUNT(*)
                FROM asset_categories
                WHERE LOWER(name) = LOWER(:name)
                    """;
        
        return (((Number) entityManager.createNativeQuery(sql)
                .setParameter("name", name)
                .getSingleResult()
                ).longValue() > 0);
    
    }

    public List<AssetCategory> findAll() {
    String sql = """
            SELECT id, name, depreciation_rate_percentage
            FROM asset_categories
            """;
    return entityManager.createNativeQuery(sql, AssetCategory.class).getResultList();
}

}
