package com.sil.asset_tagging_system.model;

import com.sil.asset_tagging_system.model.enums.ApprovalStatus;
import com.sil.asset_tagging_system.model.enums.RequestType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "approvals", indexes = {@Index(name = "idx_approval_asset", columnList = "asset_id"), @Index(name = "idx_approval_requester", columnList = "requester_id"), @Index(name = "idx_approval_status", columnList = "status"), @Index(name = "idx_approval_request_type", columnList = "request_type")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)

public class Approval {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;


    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )

    @JoinColumn(
            name = "asset_id",
            nullable = false
    )
    private Asset asset;


    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )

    @JoinColumn(
            name = "initiated_by_user_id",
            nullable = false
    )
    private User initiatedBy;


    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )

    @JoinColumn(
            name = "requester_id",
            nullable = false
    )
    private User requester;


    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(
            name = "previous_holder_id"
    )
    private User previousHolder;


    @Enumerated(EnumType.STRING)

    @Column(
            name = "request_type",
            nullable = false,
            length = 30
    )
    private RequestType requestType;


    @Builder.Default

    @Column(
            name = "required_approval_count",
            nullable = false
    )
    private Byte requiredApprovalCount = (byte) 2;


    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(
            name = "first_approver_id"
    )
    private User firstApprover;


    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(
            name = "final_approver_id"
    )
    private User finalApprover;


    @Builder.Default

    @Enumerated(EnumType.STRING)

    @Column(
            name = "status",
            nullable = false,
            length = 50
    )
    private ApprovalStatus status = ApprovalStatus.PENDING;


    @Column(
            name = "request_reason",
            columnDefinition = "TEXT"
    )
    private String requestReason;


    @Column(
            name = "first_approver_notes",
            columnDefinition = "TEXT"
    )
    private String firstApproverNotes;


    @Column(
            name = "final_approver_notes",
            columnDefinition = "TEXT"
    )
    private String finalApproverNotes;


    @Column(
            name = "rejection_reason",
            columnDefinition = "TEXT"
    )
    private String rejectionReason;


    @CreationTimestamp
    @Column(
            name = "request_date",
            updatable = false
    )
    private LocalDateTime requestDate;


    @Column(name = "first_action_date")
    private LocalDateTime firstActionDate;


    @Column(name = "final_action_date")
    private LocalDateTime finalActionDate;


    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

}