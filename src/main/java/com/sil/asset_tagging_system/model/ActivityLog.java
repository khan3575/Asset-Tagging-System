package com.sil.asset_tagging_system.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sil.asset_tagging_system.model.enums.ActivityAction;
import com.sil.asset_tagging_system.model.enums.ActivityEntityType;
import com.sil.asset_tagging_system.model.enums.ActivityOutcome;
import com.sil.asset_tagging_system.model.enums.AssetCondition;
import com.sil.asset_tagging_system.model.enums.RoleName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name="activity_log")
public class ActivityLog {

    @Id
    @Column(name="id")
    private Long id;

    @Column(name="occurred_at")
    private LocalDateTime occurredAt;

    @Column(name="correlation_id")
    private UUID correlationId;

    @Column(name="sequence_in_action")
    private Byte sequenceInAction;

    @Column(name="actor_user_id")
    private Long actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name="actor_roles")
    private RoleName actorRoles;

    @Column(name="ip_address")
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(name="action")
    private ActivityAction action;

    @Enumerated(EnumType.STRING)
    @Column(name="entity_type")
    private ActivityEntityType entityType;

    @Enumerated(EnumType.STRING)
    @Column(name="outcome")
    private ActivityOutcome outcome;

    @Column(name="failure_reason")
    private String failureReason;

    @Column(name="asset_id")
    private Long assetId;

    @Column(name="approval_id")
    private Long approvalId;

    @Column(name="subject_user_id")
    private Long subjectUserId;

    @Column(name="previous_holder_id")
    private Long previousHolderId;

    @Column(name="new_holder_id")
    private Long newHolderId;

    @Enumerated(EnumType.STRING)
    @Column(name="previous_condition")
    private AssetCondition previousCondition;

    @Enumerated(EnumType.STRING)
    @Column(name="new_condition")
    private AssetCondition newCondition;

    @Column(name="summary")
    private String summary;

    @Column(name="details")
    private String details;

}
