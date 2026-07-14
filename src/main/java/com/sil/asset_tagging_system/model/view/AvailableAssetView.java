package com.sil.asset_tagging_system.model.view;

import com.sil.asset_tagging_system.model.enums.AssetStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Immutable
@Table(name = "available_assets_view")
@Getter
@NoArgsConstructor
public class AvailableAssetView {

    @Id

    @Column(
            name = "id",
            nullable = false
    )

    private Long id;

    @Column(
            name = "asset_tag",
            nullable = false,
            length = 50
    )

    private String assetTag;

    @Column(
            name = "name",
            nullable = false,
            length = 100
    )

    private String name;

    @Column(
            name = "category",
            nullable = false,
            length = 50
    )

    private String category;

    @Column(
            name = "purchase_date",
            nullable = false
    )

    private LocalDate purchaseDate;

    @Column(
            name = "value",
            nullable = false,
            precision = 10,
            scale = 2
    )

    private BigDecimal value;

    @Enumerated(EnumType.STRING)

    @Column(
            name = "status",
            nullable = false,
            length = 50
    )

    private AssetStatus status;

}