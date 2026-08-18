package com.sil.asset_tagging_system.dao;

import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ActivityLogDao {

    private final EntityManager entityManager;

    private static final String INSERT_LOG_SQL = """
                INSERT INTO activity_log (correlation_id, sequence_in_action
                , actor_user_id, actor_roles, ip_address, action, entity_type
                , outcome, failure_reason, asset_id, approval_id, subject_user_id
                , previous_holder_id, new_holder_id, previous_condition, new_condition, summary
                )
                VALUES (UUID_TO_BIN(:correlationId), :sequenceInAction, :actorUserId, :actorRoles, :ipAddress, :action, :entityType, :outcome
                    , :failureReason, :assetId, :approvalId, :subjectUserId, :previousHolderId, :newHolderId, :previousCondition, :newCondition, :summary)
                """;

    @Transactional
    public void log(UUID correlationId, int sequenceInAction, Long actorUserId, String actorRoles
        , String ipAddress, String action, String entityType, String outcome, String failureReason
        , Long assetId, Long approvalId, Long subjectUserId, Long previousHolderId, Long newHolderId , String previousCondition, String newCondition
        , String summary)
    {   
        entityManager.createNativeQuery(INSERT_LOG_SQL)
            .setParameter("correlationId", correlationId.toString())
            .setParameter("sequenceInAction", sequenceInAction)
            .setParameter("actorUserId", actorUserId)
            .setParameter("actorRoles",actorRoles)
            .setParameter("ipAddress",ipAddress)
            .setParameter("action",action)
            .setParameter("entityType",entityType)
            .setParameter("outcome", outcome)
            .setParameter("failureReason", failureReason)
            .setParameter("assetId", assetId)
            .setParameter("approvalId", approvalId)
            .setParameter("subjectUserId",subjectUserId)
            .setParameter("previousHolderId",previousHolderId)
            .setParameter("newHolderId", newHolderId)
            .setParameter("previousCondition", previousCondition)
            .setParameter("newCondition", newCondition)
            .setParameter("summary", summary)
            .executeUpdate();        
    }

    // uuid to byte array

}
