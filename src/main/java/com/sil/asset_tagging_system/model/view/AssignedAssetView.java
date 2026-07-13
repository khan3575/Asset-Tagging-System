package com.sil.asset_tagging_system.model.view;

import com.sil.asset_tagging_system.model.enums.CustodyStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "assigned_assets_view")
@Getter
@NoArgsConstructor
public class AssignedAssetView {

    @Id

    @Column(
            name = "asset_id",
            nullable = false
    )

    private Long assetId;

    @Column(
            name = "asset_tag",
            nullable = false,
            length = 50
    )

    private String assetTag;

    @Column(
            name = "asset_name",
            nullable = false,
            length = 100
    )

    private String assetName;

    @Column(
            name = "category",
            nullable = false,
            length = 50
    )

    private String category;

    @Column(
            name = "employee_id",
            nullable = false
    )

    private Long employeeId;

    @Column(
            name = "employee_name",
            length = 121
    )

    private String employeeName;

    @Column(
            name = "email",
            nullable = false,
            length = 100
    )

    private String email;

    @Column(
            name = "department",
            length = 50
    )

    private String department;

    @Column(
            name = "custody_start",
            nullable = false
    )

    private LocalDateTime custodyStart;

    @Enumerated(EnumType.STRING)

    @Column(
            name = "custody_status",
            nullable = false,
            length = 50
    )

    private CustodyStatus custodyStatus;

}