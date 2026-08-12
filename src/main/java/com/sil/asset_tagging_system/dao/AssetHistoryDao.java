package com.sil.asset_tagging_system.dao;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;

import com.sil.asset_tagging_system.model.AssetHistory;

@Repository
public class AssetHistoryDao {
    private final EntityManager entityManager;

    public AssetHistoryDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    public List<AssetHistory> findByAssetIdOrderByActionDateDesc(Long assetId) {
        String sql = """
                SELECT id, asset_id, action, previous_status, new_status, previous_holder_id,
                       new_holder_id, action_date, performed_by_user_id, approval_id, notes
                FROM asset_history
                WHERE asset_id = :assetId
                ORDER BY action_date DESC
                """;
        return entityManager.createNativeQuery(sql, AssetHistory.class)
                .setParameter("assetId", assetId)
                .getResultList();
    }
}
