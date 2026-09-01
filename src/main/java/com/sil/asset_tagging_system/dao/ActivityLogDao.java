package com.sil.asset_tagging_system.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sil.asset_tagging_system.dto.ActivityLogRow;
import com.sil.asset_tagging_system.model.ActivityLog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
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

        private static final String AUDIT_SELECT = """
                SELECT l.id, l.occurred_at, BIN_TO_UUID(l.correlation_id), l.sequence_in_action
                        , l.actor_user_id, CONCAT_WS(' ', ua.first_name, ua.last_name), l.actor_roles, l.ip_address
                        , l.action, l.entity_type, l.outcome, l.failure_reason
                        , l.asset_id, a.asset_tag, l.approval_id
                        , l.subject_user_id, CONCAT_WS(' ', us.first_name, us.last_name)
                        , CONCAT_WS(' ', up.first_name, up.last_name)
                        , CONCAT_WS(' ', un.first_name, un.last_name)
                        , l.previous_condition, l.new_condition, l.summary, l.details
                FROM activity_log l
                LEFT JOIN users  ua ON l.actor_user_id      = ua.id
                LEFT JOIN users  us ON l.subject_user_id    = us.id
                LEFT JOIN users  up ON l.previous_holder_id = up.id
                LEFT JOIN users  un ON l.new_holder_id      = un.id
                LEFT JOIN assets a  ON l.asset_id           = a.id
                WHERE 1=1
                """;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRefusal(ActivityLog activityLog)
    {
        log(activityLog);
    }

    @Transactional
    public void log(ActivityLog activityLog)
    {
        UUID correlationId = activityLog.getCorrelationId();
        if(correlationId == null)
        {
            log.warn("Activity log written with no correlation id; action {} will not be correlated",activityLog.getAction());
            correlationId = UUID.randomUUID();
        }
        entityManager.createNativeQuery(INSERT_LOG_SQL)
            .setParameter("correlationId", correlationId.toString())
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

    private static String nameOf(Enum<?> value)
    {
        return (value == null) ? null : value.name();
    }


    @SuppressWarnings("unchecked")
    public List<ActivityLogRow> findPage(String entityType, String action, String outcome, String search,
                                         LocalDate from, LocalDate to, int limit, int offset) {

        StringBuilder sql = new StringBuilder(AUDIT_SELECT);
        appendAuditFilters(sql, entityType, action, outcome, search, from, to);
        sql.append(" ORDER BY l.occurred_at DESC, l.id DESC LIMIT :limit OFFSET :offset");

        Query query = entityManager.createNativeQuery(sql.toString());
        bindAuditFilters(query, entityType, action, outcome, search, from, to);
        query.setParameter("limit", limit);
        query.setParameter("offset", offset);

        return (List<ActivityLogRow>) query.getResultStream()
                .map(result -> {
                    Object[] r = (Object[]) result;
                    return new ActivityLogRow(
                            ((Number) r[0]).longValue(),
                            (LocalDateTime) r[1],
                            (String) r[2],
                            r[3] != null ? ((Number) r[3]).shortValue() : null,
                            r[4] != null ? ((Number) r[4]).longValue() : null,
                            blankToNull((String) r[5]),
                            (String) r[6],
                            (String) r[7],
                            (String) r[8],
                            (String) r[9],
                            (String) r[10],
                            (String) r[11],
                            r[12] != null ? ((Number) r[12]).longValue() : null,
                            (String) r[13],
                            r[14] != null ? ((Number) r[14]).longValue() : null,
                            r[15] != null ? ((Number) r[15]).longValue() : null,
                            blankToNull((String) r[16]),
                            blankToNull((String) r[17]),
                            blankToNull((String) r[18]),
                            (String) r[19],
                            (String) r[20],
                            (String) r[21],
                            (String) r[22]
                    );
                })
                .collect(Collectors.toList());
    }

    public long countPage(String entityType, String action, String outcome, String search,
                          LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM activity_log l WHERE 1=1 ");
        appendAuditFilters(sql, entityType, action, outcome, search, from, to);

        Query query = entityManager.createNativeQuery(sql.toString());
        bindAuditFilters(query, entityType, action, outcome, search, from, to);
        return ((Number) query.getSingleResult()).longValue();
    }

    private void appendAuditFilters(StringBuilder sql, String entityType, String action, String outcome,
                                    String search, LocalDate from, LocalDate to) {
        if (hasText(entityType)) sql.append(" AND l.entity_type = :entityType ");
        if (hasText(action))    sql.append(" AND l.action = :action ");
        if (hasText(outcome))   sql.append(" AND l.outcome = :outcome ");
        if (hasText(search))    sql.append(" AND (LOWER(l.summary) LIKE :search OR LOWER(l.ip_address) LIKE :search) ");
        if (from != null)   sql.append(" AND l.occurred_at >= :from ");
        if (to != null) sql.append(" AND l.occurred_at < :to ");
    }

    private void bindAuditFilters(Query query, String entityType, String action, String outcome,
                                  String search, LocalDate from, LocalDate to) {
        if (hasText(entityType)) query.setParameter("entityType", entityType);
        if (hasText(action))    query.setParameter("action", action);
        if (hasText(outcome))   query.setParameter("outcome", outcome);
        if (hasText(search))    query.setParameter("search", "%" + search.trim().toLowerCase() + "%");
        if (from != null)   query.setParameter("from", from.atStartOfDay());
        if (to != null) query.setParameter("to", to.plusDays(1).atStartOfDay());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
