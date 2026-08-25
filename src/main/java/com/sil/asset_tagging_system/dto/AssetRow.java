package com.sil.asset_tagging_system.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.sil.asset_tagging_system.model.enums.AssetCondition;

public record AssetRow(
    Long id,
    String assetTag,
    String name,
    String categoryName,
    LocalDate purchaseDate,
    BigDecimal purchaseValue,
    AssetCondition conditionStatus
){}
