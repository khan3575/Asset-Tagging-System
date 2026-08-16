package com.sil.asset_tagging_system.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Table(name="audit_log")
public class AuditLogEntry {
    @Column(name="id")
    private Long id;

    @Column(name="actor_user_id")
    private Long actorUserId;

    @Column(name="action")
    private String action;

    @Column(name="entity_type")
    private String entityType;

    @Column(name="entity_id")
    private Long entityId;

    @Column(name="description")
    private String description;

    @Column(name="ip_address")
    private String ipAddress;

    @Column(name="created_at")
    private LocalDateTime createdAt;


}
