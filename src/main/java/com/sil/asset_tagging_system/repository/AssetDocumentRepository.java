package com.sil.asset_tagging_system.repository;

import com.sil.asset_tagging_system.model.AssetDocument;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetDocumentRepository extends JpaRepository<AssetDocument, Long> {

    Optional<AssetDocument> findByAssetId(
            Long assetId
    );
    boolean existsByAssetId(Long assetId);

}