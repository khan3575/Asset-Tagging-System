package com.sil.asset_tagging_system.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import com.sil.asset_tagging_system.model.enums.AssetCondition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "assets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Asset {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
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


    // Note: 'value' is a SQL reserved keyword; JPA dialect-aware quoting is enforced with escaped quotes
    @Column(
            name = "purchase_value",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal purchaseValue;


    @Builder.Default

    @Enumerated(EnumType.STRING)

    @Column(
            name = "condition_status",
            nullable = false,
            length = 50
    )
    private AssetCondition conditionStatus = AssetCondition.IN_SERVICE;


    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )

    @JoinColumn(
            name = "created_by_user_id",
            nullable = false
    )
    private User createdBy;


    @CreationTimestamp
    @Column(
            name = "created_at",
            updatable = false
    )
    private LocalDateTime createdAt;

    
}