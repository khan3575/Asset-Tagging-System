package com.sil.asset_tagging_system.model;

import com.sil.asset_tagging_system.model.enums.AssetStatus;
import com.sil.asset_tagging_system.model.enums.HistoryAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "asset_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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


    @Column(
            name = "action_date",
            insertable = false,
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