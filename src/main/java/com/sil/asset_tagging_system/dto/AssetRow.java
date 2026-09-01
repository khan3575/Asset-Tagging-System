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
){
    public static AssetRow fromRow(Object[] row)
    {
        return new AssetRow(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (LocalDate) row[4],
                (BigDecimal) row[5],
                AssetCondition.valueOf((String) row[6])
        );
    }
}
