package com.sil.asset_tagging_system.model;

import com.sil.asset_tagging_system.model.enums.CustodyStatus;
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
@Table(name = "asset_custody", indexes = {@Index(name = "idx_custody_asset", columnList = "asset_id"), @Index(name = "idx_custody_custodian", columnList = "custodian_id"), @Index(name = "idx_custody_status", columnList = "status")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AssetCustody {

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


    @CreationTimestamp
    @Column(
            name = "custody_start",
            nullable = false,
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


    // Warning: MySQL-specific virtual generated column — not portable to H2 or PostgreSQL
    @Column(
            name = "active_asset_id",
            insertable = false,
            updatable = false
    )
    private Long activeAssetId;

}