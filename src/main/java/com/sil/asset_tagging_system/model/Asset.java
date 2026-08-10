package com.sil.asset_tagging_system.model;

import com.sil.asset_tagging_system.model.enums.AssetStatus;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "assets", indexes = {@Index(name = "idx_asset_category", columnList = "category_id"), @Index(name = "idx_asset_status", columnList = "status"), @Index(name = "idx_asset_created_by", columnList = "created_by_user_id")})
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
            name = "\"value\"",
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


    @CreationTimestamp
    @Column(
            name = "created_at",
            updatable = false
    )
    private LocalDateTime createdAt;

    @Builder.Default
    @Column(
            name = "enabled",
            nullable = false
    )
    private Boolean enabled = true;
}