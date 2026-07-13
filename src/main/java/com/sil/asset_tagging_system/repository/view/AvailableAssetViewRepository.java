package com.sil.asset_tagging_system.repository.view;

import com.sil.asset_tagging_system.model.view.AvailableAssetView;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AvailableAssetViewRepository extends JpaRepository<AvailableAssetView, Long> {

    Page<AvailableAssetView>
    findByAssetTagContainingIgnoreCaseOrNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(
            String assetTag,

            String name,

            String category,

            Pageable pageable
    );

    Page<AvailableAssetView>
    findByCategoryIgnoreCase(
            String category,

            Pageable pageable
    );

}