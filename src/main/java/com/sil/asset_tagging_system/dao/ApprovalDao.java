package com.sil.asset_tagging_system.dao;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sil.asset_tagging_system.model.enums.ApprovalStatus;
import com.sil.asset_tagging_system.model.enums.RequestType;

@Repository
public class ApprovalDao {
    private final EntityManager entityManager;

    public ApprovalDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public boolean existsOpenTransferRequest(Long assetId) {
        String sql = """
                SELECT COUNT(*)
                FROM approvals
                WHERE asset_id = :assetId
                AND request_type = :requestType
                AND status IN (:openStatuses)
                """;
        Number count = (Number) entityManager.createNativeQuery(sql)
                .setParameter("assetId", assetId)
                .setParameter("requestType", RequestType.TRANSFER_REQUEST.name())
                .setParameter("openStatuses", List.of(ApprovalStatus.PENDING.name(), ApprovalStatus.FIRST_APPROVED.name()))
                .getSingleResult();

        return count.longValue() > 0;
    }

    // previousHolderId may be null if the asset has no current custodian yet
    @Transactional
    public Long createTransferRequest(Long assetId, Long initiatedByUserId, Long requesterId, Long previousHolderId) {
        String sql = """
                INSERT INTO approvals (asset_id, initiated_by_user_id, requester_id, previous_holder_id, request_type, status)
                VALUES (:assetId, :initiatedByUserId, :requesterId, :previousHolderId, :requestType, :status)
                """;
        entityManager.createNativeQuery(sql)
                .setParameter("assetId", assetId)
                .setParameter("initiatedByUserId", initiatedByUserId)
                .setParameter("requesterId", requesterId)
                .setParameter("previousHolderId", previousHolderId)
                .setParameter("requestType", RequestType.TRANSFER_REQUEST.name())
                .setParameter("status", ApprovalStatus.PENDING.name())
                .executeUpdate();

        Number generatedId = (Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult();
        return generatedId.longValue();
    }
}
