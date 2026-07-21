package com.sil.asset_tagging_system.repository;

import com.sil.asset_tagging_system.model.AssetCategory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetCategoryRepository extends JpaRepository<AssetCategory, Long> {



    Optional<AssetCategory> findByNameIgnoreCase(
            String name
    );

    boolean existsByNameIgnoreCase(String name);

}