package com.sil.asset_tagging_system.dao;

import java.util.List;
import java.util.Map;
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
    @SuppressWarnings("unchecked")
    public Optional<AssetCategory> findByNameIgnoreCase(String name)
    {
        String query = """
                SELECT id, name, depreciation_rate_percentage
                FROM asset_categories
                WHERE LOWER(name) = LOWER(:name)
                """;
        return entityManager.createNativeQuery(query, AssetCategory.class)
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

        return DaoUtils.exists(entityManager, sql, Map.of("name", name));
    }

    @SuppressWarnings("unchecked")
    public List<AssetCategory> findAll() {
    String sql = """
            SELECT id, name, depreciation_rate_percentage
            FROM asset_categories
            """;
    return entityManager.createNativeQuery(sql, AssetCategory.class).getResultList();
}

}
