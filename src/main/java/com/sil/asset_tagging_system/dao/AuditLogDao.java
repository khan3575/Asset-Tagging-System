package com.sil.asset_tagging_system.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AuditLogDao {

    // this persistence context caches the same id. if we use the same id in same query it use the cache.
    @PersistenceContext
    EntityManager entityManager;

    @Transactional
    public void log(Long actorUserId, String action, String entityType, Long entityId, String description, String ipAddress){
        String sql= """
                INSERT INTO audit_log(actor_user_id, action, entity_type, entity_id, description, ip_address)
                VALUES(:actorUserId, :action, :entityType, :entityId, :description, :ipAddress) 
                """;

        entityManager.createNativeQuery(sql).setParameter("actorUserId", actorUserId)
                .setParameter("action", action)
                .setParameter("entityType", entityType)
                .setParameter("entityId", entityId)
                .setParameter("description", description)
                .setParameter("ipAddress", ipAddress)
                .executeUpdate();
    }


    @SuppressWarnings("unchecked")
    public List<AuditLogEntry> findRecent(int limit)
    {
        String sql = """
                SELECT id, actor_user_id, action, entity_type, entity_id, description, ip_address, created_at
                FROM audit_log ORDER BY created_at DESC
                """;

        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setMaxResults(limit).getResultList();

        List<AuditLogEntry> entries = new ArrayList<>();

        for(Object[] row : rows)
        {
            entries.add(AuditLogEntry.builder()
                    .id(((Number) row[0]).longValue())
                    .actorUserId( row[1] == null ? null: ((Number)row[1]).longValue())
                    .action((String) row[2])
                    .entityType((String) row[3])
                    .entityId( (row[4] == null) ? null :  ((Number)row[4]).longValue())
                    .description((String) row[5])
                    .ipAddress((String)row[6])
                    .createdAt(((Timestamp)row[7]).toLocalDateTime())
                    .build());
        }
        return entries;
    }

}
