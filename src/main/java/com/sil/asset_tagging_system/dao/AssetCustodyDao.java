package com.sil.asset_tagging_system.dao;

import java.time.LocalDateTime;
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


    public void releaseActiveCustody(Long assetId, LocalDateTime endTime)
    {
        String sql = """
                UPDATE asset_custody
                SET custody_end = :endTime, status = 'RELEASED'
                WHERE  asset_id = :assetId AND status = 'ACTIVE'
                """;

        entityManager.createNativeQuery(sql)
            .setParameter("assetId", assetId)
            .setParameter("endTime", endTime)
            .executeUpdate();
    }

    public void initiateNewCustody(long assetId, long newCustodianId, long approvalId, long assignedByUserId)
    {
        String sql = """
            INSERT INTO asset_custody (asset_id, custodian_id, approval_id, assigned_by_user_id, status) 
            VALUES (:assetId, :newCustodianId, :approvalId, :assignedByUserId, 'ACTIVE')
            """;

        entityManager.createNativeQuery(sql)
            .setParameter("assetId", assetId)
            .setParameter("newCustodianId", newCustodianId)
            .setParameter("approvalId", approvalId)
            .setParameter("assignedByUserId", assignedByUserId)
            .executeUpdate();
    }
    
    public void transferCustody(long assetId, long newCustodianId, long approvalId,long assignedByUserId, LocalDateTime endTime)
    {
        releaseActiveCustody(assetId, endTime);
        initiateNewCustody(assetId, newCustodianId, approvalId, assignedByUserId);
    }
}
