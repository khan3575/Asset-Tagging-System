package com.sil.asset_tagging_system.dao;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sil.asset_tagging_system.model.ActivityLog;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ActivityLogDao {

    private final EntityManager entityManager;

    private static final String INSERT_LOG_SQL = """
                INSERT INTO activity_log ( correlation_id, sequence_in_action
                , actor_user_id, actor_roles, ip_address, action, entity_type
                , outcome, failure_reason, asset_id, approval_id, subject_user_id
                , previous_holder_id, new_holder_id, previous_condition, new_condition, summary, details
                )
                VALUES ( UUID_TO_BIN(:correlationId), :sequenceInAction, :actorUserId, :actorRoles, :ipAddress
                , :action, :entityType, :outcome, :failureReason, :assetId, :approvalId, :subjectUserId, :previousHolderId
                , :newHolderId, :previousCondition, :newCondition, :summary, :details )
                """;

    private static final String FIND_RECENT_LOG = """
            SELECT id, occurred_at, correlation_id, sequence_in_action
                , actor_user_id, actor_roles, ip_address, action, entity_type
                , outcome, failure_reason, asset_id, approval_id, subject_user_id
                , previous_holder_id, new_holder_id, previous_condition, new_condition, summary, details
            FROM activity_log
            LIMIT :limit
            OFFSET :offset
            """;

    @Transactional
    public void log(ActivityLog activityLog)
    {   
        entityManager.createNativeQuery(INSERT_LOG_SQL)
            .setParameter("correlationId", activityLog.getCorrelationId().toString())
            .setParameter("sequenceInAction", activityLog.getSequenceInAction())
            .setParameter("actorUserId", activityLog.getActorUserId())
            .setParameter("actorRoles", nameOf(activityLog.getActorRoles()))
            .setParameter("ipAddress",activityLog.getIpAddress())
            .setParameter("action", nameOf(activityLog.getAction()))
            .setParameter("entityType", nameOf(activityLog.getEntityType()))
            .setParameter("outcome", nameOf(activityLog.getOutcome()))
            .setParameter("failureReason", activityLog.getFailureReason())
            .setParameter("assetId", activityLog.getAssetId())
            .setParameter("approvalId", activityLog.getApprovalId())
            .setParameter("subjectUserId",activityLog.getSubjectUserId())
            .setParameter("previousHolderId",activityLog.getPreviousHolderId())
            .setParameter("newHolderId", activityLog.getNewHolderId())
            .setParameter("previousCondition", nameOf(activityLog.getPreviousCondition()))
            .setParameter("newCondition", nameOf(activityLog.getNewCondition()))
            .setParameter("summary", activityLog.getSummary())
            .setParameter("details", activityLog.getDetails())
            .executeUpdate();        
    }



    // Native queries bind plain values, so enums have to be handed over as their
    // stored text. Null stays null — most of these columns are optional.
    private static String nameOf(Enum<?> value)
    {
        return (value == null) ? null : value.name();
    }


    public List<ActivityLog> findRecent(int limit, int offset){
        List<ActivityLog> resultList = entityManager.createNativeQuery(FIND_RECENT_LOG, ActivityLog.class)
                                        .setParameter("offset", offset)
                                        .setParameter("limit", limit)
                                        .getResultStream()
                                        .map(ActivityLog.class::cast)
                                        .toList();
        return resultList;
    }


    public long countAll(){
        String sql = """
                SELECT COUNT(*) 
                FROM activity_log
                """;
        return ((Number)entityManager.createNativeQuery(sql).getSingleResult()).longValue();
    }



}
