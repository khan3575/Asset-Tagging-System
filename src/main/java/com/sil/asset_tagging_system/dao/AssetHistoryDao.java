package com.sil.asset_tagging_system.dao;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sil.asset_tagging_system.model.AssetHistory;
import com.sil.asset_tagging_system.model.enums.AssetStatus;
import com.sil.asset_tagging_system.model.enums.HistoryAction;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    @Transactional
    public void insert(Long assetId, HistoryAction action, Long performedByUserId, Long previousHolderId,
                    Long newHolderId, AssetStatus previousStatus, AssetStatus newStatus, Long approvalId, String notes)
        {
            String sql = """
                    INSERT INTO asset_history (asset_id, action, previous_status, new_status
                    ,previous_holder_id, new_holder_id, performed_by_user_id, approval_id, notes)
                    VALUES(:assetId, :action, :previousStatus, :newStatus, :previousHolderId
                    , :newHolderId, :performedByUserId, :approvalId, :notes)
                    """;
            try{
                entityManager.createNativeQuery(sql)
                    .setParameter("assetId",assetId)
                    .setParameter("action",action.name())
                    .setParameter("previousStatus", (previousStatus == null)? null : previousStatus.name())
                    .setParameter("newStatus", (newStatus == null) ? null : newStatus.name())
                    .setParameter("previousHolderId", previousHolderId)
                    .setParameter("newHolderId", newHolderId)
                    .setParameter("performedByUserId", performedByUserId)
                    .setParameter("approvalId",approvalId)
                    .setParameter("notes", notes)
                    .executeUpdate();

                log.info("AssetHistory inserted ");
            }
            catch(Exception ex)
            {
                log.error("AssetHistoryDao insertion failed",ex);
                throw ex;
            }
        }
}
