package com.sil.asset_tagging_system.model;

import com.sil.asset_tagging_system.model.enums.AssetStatus;
import com.sil.asset_tagging_system.model.enums.HistoryAction;
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
@Table(name = "asset_history", indexes = {@Index(name = "idx_history_asset", columnList = "asset_id"), @Index(name = "idx_history_performed_by", columnList = "performed_by_user_id"), @Index(name = "idx_history_action_date", columnList = "action_date")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AssetHistory {

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


    @Enumerated(EnumType.STRING)

    @Column(
            name = "action",
            nullable = false,
            length = 100
    )
    private HistoryAction action;


    @Enumerated(EnumType.STRING)

    @Column(
            name = "previous_status",
            length = 50
    )
    private AssetStatus previousStatus;


    @Enumerated(EnumType.STRING)

    @Column(
            name = "new_status",
            length = 50
    )
    private AssetStatus newStatus;


    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(
            name = "previous_holder_id"
    )
    private User previousHolder;


    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(
            name = "new_holder_id"
    )
    private User newHolder;


    @CreationTimestamp
    @Column(
            name = "action_date",
            updatable = false
    )
    private LocalDateTime actionDate;


    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )

    @JoinColumn(
            name = "performed_by_user_id",
            nullable = false
    )
    private User performedBy;


    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(
            name = "approval_id"
    )
    private Approval approval;


    @Column(
            name = "notes",
            columnDefinition = "TEXT"
    )
    private String notes;

}