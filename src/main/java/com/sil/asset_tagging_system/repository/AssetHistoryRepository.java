package com.sil.asset_tagging_system.repository;

import com.sil.asset_tagging_system.model.AssetHistory;

import com.sil.asset_tagging_system.model.enums.HistoryAction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AssetHistoryRepository extends JpaRepository<AssetHistory, Long> {

    @EntityGraph(attributePaths = {"asset", "performedBy"})
    List<AssetHistory>
    findByAssetIdOrderByActionDateDesc(
            Long assetId
    );

    @EntityGraph(attributePaths = {"asset", "performedBy"})
    Page<AssetHistory>
    findByAssetIdOrderByActionDateDesc(
            Long assetId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"asset", "performedBy"})
    Page<AssetHistory> findByAction(
            HistoryAction action,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"asset", "performedBy"})
    Page<AssetHistory>
    findByPerformedByIdOrderByActionDateDesc(
            Long userId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"asset", "performedBy"})
    Page<AssetHistory>
    findByActionDateBetween(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

}