package com.sil.asset_tagging_system.model.view;

import com.sil.asset_tagging_system.model.enums.ApprovalStatus;
import com.sil.asset_tagging_system.model.enums.RequestType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity

@Immutable

@Table(name = "pending_approvals_view")

@Getter

@NoArgsConstructor

public class PendingApprovalView {

    @Id

    @Column(
            name = "approval_id",
            nullable = false
    )

    private Long approvalId;

    @Enumerated(EnumType.STRING)

    @Column(
            name = "request_type",
            nullable = false,
            length = 30
    )

    private RequestType requestType;

    @Enumerated(EnumType.STRING)

    @Column(
            name = "status",
            nullable = false,
            length = 50
    )

    private ApprovalStatus status;

    @Column(
            name = "required_approval_count",
            nullable = false
    )

    private Byte requiredApprovalCount;

    @Column(
            name = "asset_id",
            nullable = false
    )

    private Long assetId;

    @Column(
            name = "asset_tag",
            nullable = false,
            length = 50
    )

    private String assetTag;

    @Column(
            name = "asset_name",
            nullable = false,
            length = 100
    )

    private String assetName;

    @Column(
            name = "requester_name",
            length = 121
    )

    private String requesterName;

    @Column(
            name = "requester_email",
            nullable = false,
            length = 100
    )

    private String requesterEmail;

    @Column(
            name = "previous_holder_name",
            length = 121
    )

    private String previousHolderName;

    @Column(
            name = "request_reason",
            columnDefinition = "TEXT"
    )

    private String requestReason;

    @Column(
            name = "request_date"
    )

    private LocalDateTime requestDate;

}