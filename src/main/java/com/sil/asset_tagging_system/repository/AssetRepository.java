package com.sil.asset_tagging_system.repository;

import com.sil.asset_tagging_system.model.Asset;
import com.sil.asset_tagging_system.model.enums.AssetStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    @EntityGraph(attributePaths = {"category", "createdBy"})
    Optional<Asset> findByAssetTagIgnoreCase(
            String assetTag
    );

    boolean existsByAssetTagIgnoreCase(
            String assetTag
    );

    boolean existsByAssetTagIgnoreCaseAndIdNot(
            String assetTag,
            Long id
    );

    @EntityGraph(attributePaths = {"category", "createdBy"})
    Page<Asset> findByStatus(
            AssetStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"category", "createdBy"})
    Page<Asset> findByEnabled(
            Boolean enabled,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"category", "createdBy"})
    Page<Asset> findByStatusAndEnabled(
            AssetStatus status,
            Boolean enabled,
            Pageable pageable
    );

    List<Asset> findAllByStatusOrderByNameAsc(
            AssetStatus status
    );

    long countByStatus(AssetStatus status);

    long countByStatusAndEnabled(AssetStatus status, Boolean enabled);

    @EntityGraph(attributePaths = {"category", "createdBy"})
    Page<Asset> findByCategoryId(
            Long categoryId,
            Pageable pageable
    );

    // Note: Leading wildcard LIKE ('%keyword%') prevents B-tree index usage — consider full-text search at scale
    @Query(
        value = """
            SELECT asset
            FROM Asset asset
            JOIN FETCH asset.category category
            JOIN FETCH asset.createdBy createdBy
            WHERE
                LOWER(asset.assetTag)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR
                LOWER(asset.name)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR
                LOWER(category.name)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
            """,
        countQuery = """
            SELECT COUNT(asset)
            FROM Asset asset
            JOIN asset.category category
            WHERE
                LOWER(asset.assetTag)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR
                LOWER(asset.name)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR
                LOWER(category.name)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
            """
    )
    Page<Asset> searchAssets(
            @Param("keyword")
            String keyword,

            Pageable pageable
    );

    @Query(
        value = """
            SELECT asset
            FROM Asset asset
            JOIN FETCH asset.category category
            JOIN FETCH asset.createdBy createdBy
            WHERE asset.status = :status
            AND
            (
                LOWER(asset.assetTag)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR
                LOWER(asset.name)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR
                LOWER(category.name)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """,
        countQuery = """
            SELECT COUNT(asset)
            FROM Asset asset
            JOIN asset.category category
            WHERE asset.status = :status
            AND
            (
                LOWER(asset.assetTag)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR
                LOWER(asset.name)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR
                LOWER(category.name)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """
    )
    Page<Asset> searchAssetsByStatus(
            @Param("keyword")
            String keyword,

            @Param("status")
            AssetStatus status,

            Pageable pageable
    );

}