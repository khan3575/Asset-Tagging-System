package com.sil.asset_tagging_system.repository;

import com.sil.asset_tagging_system.model.AssetCustody;

import com.sil.asset_tagging_system.model.enums.CustodyStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetCustodyRepository extends JpaRepository<AssetCustody, Long> {

    @EntityGraph(attributePaths = {"asset", "custodian", "assignedBy"})
    Optional<AssetCustody> findByAssetIdAndStatus(
            Long assetId,
            CustodyStatus status
    );

    boolean existsByAssetIdAndStatus(
            Long assetId,
            CustodyStatus status
    );

    @EntityGraph(attributePaths = {"asset", "custodian", "assignedBy"})
    List<AssetCustody> findByCustodianIdAndStatus(
            Long custodianId,
            CustodyStatus status
    );

    @EntityGraph(attributePaths = {"asset", "custodian", "assignedBy"})
    Page<AssetCustody> findByCustodianIdAndStatus(
            Long custodianId,
            CustodyStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"asset", "custodian", "assignedBy"})
    Page<AssetCustody>
    findByCustodianIdOrderByCustodyStartDesc(
            Long custodianId,
            Pageable pageable
    );
    @EntityGraph(attributePaths = {"asset", "custodian", "assignedBy"})
    List<AssetCustody>
    findByAssetIdOrderByCustodyStartDesc(
            Long assetId
    );

}