package com.sil.asset_tagging_system.dao;

import java.util.Map;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;

@Repository
public class AssetDocumentDao {
    private final EntityManager entityManager;

    public AssetDocumentDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public boolean existsByAssetId(Long assetId) {
        String sql = """
                SELECT COUNT(*)
                FROM asset_documents
                WHERE asset_id = :assetId
                """;
        return DaoUtils.exists(entityManager, sql, Map.of("assetId", assetId));
    }
}
