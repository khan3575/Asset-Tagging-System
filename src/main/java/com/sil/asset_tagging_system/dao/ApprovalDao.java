package com.sil.asset_tagging_system.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;

import com.sil.asset_tagging_system.dto.ApprovalRow;
import com.sil.asset_tagging_system.model.enums.ApprovalActionType;
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
        return DaoUtils.exists(entityManager, sql, Map.of(
                "assetId", assetId,
                "requestType", RequestType.TRANSFER.name(),
                "openStatuses", List.of(ApprovalStatus.PENDING.name(), ApprovalStatus.PARTIALLY_APPROVED.name())
        ));
    }

    // previousHolderId may be null if the asset has no current custodian yet
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
                .setParameter("requestType", RequestType.TRANSFER.name())
                .setParameter("status", ApprovalStatus.PENDING.name())
                .executeUpdate();

        return DaoUtils.getLastInsertId(entityManager);
    }

    public int recordAction(long approvalId, long actorUserId, ApprovalActionType action, String notes)
    {
        String nextSeq = """
                SELECT COALESCE(MAX(sequence_no),0) + 1
                FROM approval_actions
                WHERE approval_id = :approvalId
                """;
        int sequenceNo = ((Number) entityManager.createNativeQuery(nextSeq)
                                    .setParameter("approvalId", approvalId)
                                    .getSingleResult()).intValue();

        String newAction = """
                INSERT INTO approval_actions(approval_id, actor_user_id, action, sequence_no, notes)
                VALUES (:approvalId, :actorUserId, :action, :sequenceNo, :notes)
                """;
        entityManager.createNativeQuery(newAction)
            .setParameter("approvalId", approvalId)
            .setParameter("actorUserId", actorUserId)
            .setParameter("action", action.name())
            .setParameter("sequenceNo", sequenceNo)
            .setParameter("notes", notes)
            .executeUpdate();

        return sequenceNo;
    }

    public long countApprovedActions(long approvalId)
    {
        String sql = """
                SELECT COUNT(*)
                FROM approval_actions
                WHERE approval_id = :approvalId AND action = :action
                """;
        return ((Number) entityManager.createNativeQuery(sql)
                .setParameter("approvalId", approvalId)
                .setParameter("action", ApprovalActionType.APPROVED.name())
                .getSingleResult()).longValue();
    }

    public ApprovalSnapshot findApprovalSnapshot(long approvalId)
    {
        // check approval count
        String sql = """
                SELECT asset_id, requester_id, required_approval_count
                FROM approvals
                WHERE id = :approvalId
                """;
        
        Object[] row = (Object[]) entityManager.createNativeQuery(sql)
                .setParameter("approvalId", approvalId)
                .getSingleResult();


        
        return new ApprovalSnapshot(
                ((Number) row[0]).longValue(),
                ((Number) row[1]).longValue(),
                ((Number) row[2]).byteValue()
        );
    }

    public void markPartiallyApproved(long approvalId)
    {
        String sql = """
                UPDATE approvals
                SET status = :status
                WHERE id = :approvalId
                """;
        entityManager.createNativeQuery(sql)
                .setParameter("status", ApprovalStatus.PARTIALLY_APPROVED.name())
                .setParameter("approvalId", approvalId)
                .executeUpdate();
    }

    public void closeApproval(long approvalId, ApprovalStatus finalStatus)
    {
        String sql = """
                UPDATE approvals
                SET status = :status, closed_at = CURRENT_TIMESTAMP
                WHERE id = :approvalId
                """;
        entityManager.createNativeQuery(sql)
                .setParameter("status", finalStatus.name())
                .setParameter("approvalId", approvalId)
                .executeUpdate();
    }

    public record ApprovalSnapshot(Long assetId, Long requesterId, byte requiredApprovalCount) {}

    @SuppressWarnings("unchecked")
    public Optional<ApprovalRow> findApprovalDetail(Long approvalId)
    {
        // LEFT JOIN on users -- a RETURN request has no incoming holder, so
        // requester_id is legitimately NULL (see DESIGN.md SS4). An inner join here
        // would silently drop those rows instead of returning them with a null requester.
        String sql = """
            SELECT a.id, ast.asset_tag, ast.name, u.id, u.first_name
            , u.last_name, a.status, a.required_approval_count, a.requested_at
            FROM approvals a
            JOIN assets ast ON a.asset_id = ast.id
            LEFT JOIN users u ON a.requester_id = u.id
            WHERE a.id = :approvalId
            """;

        return entityManager.createNativeQuery(sql)
            .setParameter("approvalId", approvalId)
            .getResultStream()
            .findFirst()
            .map(result -> {
                Object[] row = (Object[]) result;
                return new ApprovalRow(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (String) row[2],
                    row[3] != null ? ((Number) row[3]).longValue() : null,
                    (String) row[4],
                    (String) row[5],
                    (String) row[6],
                    ((Number) row[7]).byteValue(),
                    ((LocalDateTime)row[8])
                );
            });
    }

    public Boolean hasActorRecordedAction(long approvalId, long actorUserId)
    {
        String sql = """
                SELECT COUNT(*)
                FROM approval_actions
                WHERE approval_id = :approvalId
                AND actor_user_id = :actorUserId
                """;
        return DaoUtils.exists(entityManager, sql, Map.of(
                "approvalId", approvalId,
                "actorUserId", actorUserId
        ));
    }
    
    @SuppressWarnings("unchecked")
    public List<ApprovalRow> findOpenApprovals(int limit,int offset)
    {
        String sql = """
            SELECT a.id, ast.asset_tag, ast.name, u.id, u.first_name
            , u.last_name, a.status, a.required_approval_count, a.requested_at
            FROM approvals a
            JOIN assets ast ON a.asset_id = ast.id
            LEFT JOIN users u ON a.requester_id = u.id
            WHERE a.status IN ('PENDING', 'PARTIALLY_APPROVED')
            ORDER BY a.requested_at ASC
            LIMIT :limit OFFSET :offset
            """;
        List<ApprovalRow> results = (List<ApprovalRow>) entityManager.createNativeQuery(sql)
                .setParameter("limit", limit)
                .setParameter("offset", offset)
                .getResultStream() // Returns Stream<Object>
                 .map(result -> {
                        Object[] row = (Object[]) result;
                        return new ApprovalRow(
                                ((Number) row[0]).longValue(),
                                (String) row[1],
                                (String) row[2],
                                row[3] != null ? ((Number) row[3]).longValue() : null,
                                (String) row[4],
                                (String) row[5],
                                (String) row[6],
                                ((Number) row[7]).byteValue(),
                                ((java.time.LocalDateTime) row[8])
                        );
                        })
                        .collect(Collectors.toList());
        return results;
    }

    public long countOpenApprovals()
    {
        String sql = """
            SELECT COUNT(*)
            FROM approvals a
            WHERE a.status IN (:status)
            """;

        List<String> statusList = List.of(ApprovalStatus.PENDING.name(), ApprovalStatus.PARTIALLY_APPROVED.name());
        long count = ((Number)entityManager.createNativeQuery(sql)
                .setParameter("status",statusList).getSingleResult()).longValue();
        return count;
    }


}
