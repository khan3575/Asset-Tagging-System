package com.sil.asset_tagging_system.model;

import com.sil.asset_tagging_system.model.enums.AssetStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(
            name = "asset_tag",
            nullable = false,
            unique = true,
            length = 50
    )
    private String assetTag;


    @Column(
            name = "name",
            nullable = false,
            length = 100
    )
    private String name;


    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )

    @JoinColumn(
            name = "category_id",
            nullable = false
    )
    private AssetCategory category;


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


    @Builder.Default

    @Enumerated(EnumType.STRING)

    @Column(
            name = "status",
            nullable = false,
            length = 50
    )
    private AssetStatus status = AssetStatus.AVAILABLE;


    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )

    @JoinColumn(
            name = "created_by_user_id",
            nullable = false
    )
    private User createdBy;


    @Column(
            name = "created_at",
            insertable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

}