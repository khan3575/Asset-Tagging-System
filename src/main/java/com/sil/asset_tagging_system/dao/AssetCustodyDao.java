package com.sil.asset_tagging_system.dao;

import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;

import com.sil.asset_tagging_system.model.enums.CustodyStatus;

@Repository
public class AssetCustodyDao {
    private final EntityManager entityManager;

    public AssetCustodyDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    public Optional<Long> findActiveCustodianId(Long assetId) {
        String sql = """
                SELECT custodian_id
                FROM asset_custody
                WHERE asset_id = :assetId AND status = :status
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("assetId", assetId)
                .setParameter("status", CustodyStatus.ACTIVE.name())
                .getResultStream()
                .map(row -> ((Number) row).longValue())
                .findFirst();
    }
}
