package com.sil.asset_tagging_system.repository.view;

import com.sil.asset_tagging_system.model.view.AssetSummaryView;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetSummaryViewRepository extends JpaRepository<AssetSummaryView, Long> {

}