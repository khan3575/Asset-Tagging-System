package com.sil.asset_tagging_system.dao;

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
        Number count = (Number) entityManager.createNativeQuery(sql)
                .setParameter("assetId", assetId)
                .getSingleResult();

        return count.longValue() > 0;
    }
}
