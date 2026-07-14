package com.sil.asset_tagging_system.repository.view;

import com.sil.asset_tagging_system.model.view.AssignedAssetView;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignedAssetViewRepository extends JpaRepository<AssignedAssetView, Long> {

    Page<AssignedAssetView> findByEmployeeId(
            Long employeeId,
            Pageable pageable
    );

    Page<AssignedAssetView>
    findByAssetTagContainingIgnoreCaseOrAssetNameContainingIgnoreCaseOrEmployeeNameContainingIgnoreCase(
            String assetTag,

            String assetName,

            String employeeName,

            Pageable pageable
    );

}