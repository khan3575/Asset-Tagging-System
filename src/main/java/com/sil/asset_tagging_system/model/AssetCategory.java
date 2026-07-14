package com.sil.asset_tagging_system.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "asset_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(
            name = "name",
            nullable = false,
            unique = true,
            length = 50
    )
    private String name;


    @Builder.Default

    @Column(
            name = "depreciation_rate_percentage",
            precision = 5,
            scale = 2
    )
    private BigDecimal depreciationRatePercentage = BigDecimal.ZERO;

}