package com.sil.asset_tagging_system.model.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

@Entity
@Immutable
@Table(name = "asset_summary_view")
@Getter
@NoArgsConstructor
public class AssetSummaryView {

    @Id

    @Column(
            name = "total_assets",
            nullable = false
    )

    private Long totalAssets;

    @Column(
            name = "assigned_assets",
            precision = 23,
            scale = 0
    )

    private BigDecimal assignedAssets;

    @Column(
            name = "available_assets",
            precision = 23,
            scale = 0
    )

    private BigDecimal availableAssets;

    @Column(
            name = "damaged_assets",
            precision = 23,
            scale = 0
    )

    private BigDecimal damagedAssets;

    @Column(
            name = "retired_assets",
            precision = 23,
            scale = 0
    )

    private BigDecimal retiredAssets;

    @Column(
            name = "total_purchase_value",
            precision = 32,
            scale = 2
    )

    private BigDecimal totalPurchaseValue;

}