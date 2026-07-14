package com.sil.asset_tagging_system.repository;

import com.sil.asset_tagging_system.model.AssetHistory;

import com.sil.asset_tagging_system.model.enums.HistoryAction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AssetHistoryRepository extends JpaRepository<AssetHistory, Long> {

    List<AssetHistory>
    findByAssetIdOrderByActionDateDesc(
            Long assetId
    );

    Page<AssetHistory>
    findByAssetIdOrderByActionDateDesc(
            Long assetId,
            Pageable pageable
    );

    Page<AssetHistory> findByAction(
            HistoryAction action,
            Pageable pageable
    );

    Page<AssetHistory>
    findByPerformedByIdOrderByActionDateDesc(
            Long userId,
            Pageable pageable
    );

    Page<AssetHistory>
    findByActionDateBetween(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

}