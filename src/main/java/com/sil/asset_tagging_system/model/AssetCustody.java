package com.sil.asset_tagging_system.model;

import com.sil.asset_tagging_system.model.enums.CustodyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "asset_custody")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetCustody {

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


    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )

    @JoinColumn(
            name = "custodian_id",
            nullable = false
    )
    private User custodian;


    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(
            name = "approval_id"
    )
    private Approval approval;


    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(
            name = "assigned_by_user_id"
    )
    private User assignedBy;


    @Column(
            name = "custody_start",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private LocalDateTime custodyStart;


    @Column(name = "custody_end")
    private LocalDateTime custodyEnd;


    @Builder.Default

    @Enumerated(EnumType.STRING)

    @Column(
            name = "status",
            nullable = false,
            length = 50
    )
    private CustodyStatus status = CustodyStatus.ACTIVE;


    /*
     * MySQL generated column.
     *
     * Hibernate can read this value,
     * but it must not insert or update it.
     */
    @Column(
            name = "active_asset_id",
            insertable = false,
            updatable = false
    )
    private Long activeAssetId;

}