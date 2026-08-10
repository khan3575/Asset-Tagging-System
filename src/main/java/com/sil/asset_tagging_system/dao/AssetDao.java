package com.sil.asset_tagging_system.dao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sil.asset_tagging_system.model.Asset;
import com.sil.asset_tagging_system.model.enums.AssetStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class AssetDao {
    private final EntityManager entityManager;

    public AssetDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<Asset> findByAssetTagIgnoreCase(String assetTag) {
        String sql = """
                SELECT id , asset_tag, name, category_id, purchase_date, value, status, created_by_user_id, created_at, enabled
                FROM assets
                WHERE LOWER(asset_tag) = LOWER(:assetTag)
                """;

        return entityManager.createNativeQuery(sql, Asset.class)
                .setParameter("assetTag", assetTag)
                .getResultStream()
                .findFirst();
    }

    public List<Asset> findAll() {
        String sql = """
                SELECT id, asset_tag, name, category_id, purchase_date, value, status, created_by_user_id, created_at, enabled
                FROM assets
                """;
        return entityManager.createNativeQuery(sql, Asset.class).getResultList();
    }

    @Transactional
    public Long createAsset(String assetTag, String name, Long categoryId, LocalDate purchaseDate,
                             BigDecimal value, Long createdByUserId) {
        String insertSql = """
                INSERT INTO assets (asset_tag, name, category_id, purchase_date, value, status, created_by_user_id, enabled)
                VALUES (:assetTag, :name, :categoryId, :purchaseDate, :value, :status, :createdByUserId, true)
                """;
        entityManager.createNativeQuery(insertSql)
                .setParameter("assetTag", assetTag)
                .setParameter("name", name)
                .setParameter("categoryId", categoryId)
                .setParameter("purchaseDate", purchaseDate)
                .setParameter("value", value)
                .setParameter("status", AssetStatus.AVAILABLE.name())
                .setParameter("createdByUserId", createdByUserId)
                .executeUpdate();

        Number generatedId = (Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult();
        return generatedId.longValue();
    }

    public Boolean existsByAssetTagIgnoreCase(String assetTag) {
        String sql = """
                SELECT count(*)
                FROM assets
                WHERE LOWER(asset_tag) = LOWER(:assetTag)
                """;
        Number count = (Number) entityManager.createNativeQuery(sql).setParameter("assetTag", assetTag).getSingleResult();

        return count.longValue() > 0;
    }

    public Boolean existsByAssetTagIgnoreCaseAndIdNot(String assetTag, Long id) {
        String sql = """
                SELECT count(*)
                FROM assets
                WHERE LOWER(asset_tag) = LOWER(:assetTag)
                AND id != :id
                """;
        Number count = (Number) entityManager.createNativeQuery(sql)
                .setParameter("assetTag", assetTag)
                .setParameter("id", id)
                .getSingleResult();
        return count.longValue() > 0;
    }


    public Page<Asset> findByStatus(AssetStatus status, Pageable pageable) {
        String totalCountSql = """
                SELECT COUNT(*)
                FROM assets
                WHERE status = :status
                """;
        long totalCount = ((Number)entityManager.createNativeQuery(totalCountSql).setParameter("status", status.name()).getSingleResult()).longValue();

        String dataSql = """
                SELECT id, asset_tag, name, category_id, purchase_date, value, status, created_by_user_id, created_at, enabled
                FROM assets
                WHERE status = :status
                """;
        List<Asset> assetList = entityManager.createNativeQuery(dataSql, Asset.class)
                .setParameter("status", status.name())
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
        return new PageImpl<>(assetList, pageable, totalCount);
    }

    public Page<Asset> findByEnabled(Boolean enabled, Pageable pageable) {
        String totalCountSql = """
                SELECT COUNT(*)
                FROM assets
                WHERE enabled = :enabled
                """;
        Long totalCount = ((Number) entityManager.createNativeQuery(totalCountSql).setParameter("enabled", enabled).getSingleResult()).longValue();

        String dataSql = """
                SELECT id, asset_tag, name, category_id, purchase_date, value, status, created_by_user_id, created_at, enabled
                FROM assets
                WHERE enabled = :enabled
                """;

        List<Asset> assets = entityManager.createNativeQuery(dataSql,Asset.class)
                .setParameter("enabled", enabled)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        return new PageImpl<>(assets, pageable, totalCount);
    }


    public Page<Asset> findByStatusAndEnabled(AssetStatus status, Boolean enabled, Pageable pageable) {
        String totalCountSql = """
                SELECT count(*)
                FROM assets
                WHERE status = :status AND enabled = :enabled
                """;
        Long totalCount = ((Number) entityManager.createNativeQuery(totalCountSql)
                .setParameter("status", status.name())
                .setParameter("enabled", enabled)
                .getSingleResult()).longValue();

        String dataSql = """
                SELECT id, asset_tag, name, category_id, purchase_date, value, status, created_by_user_id, created_at, enabled
                FROM assets
                WHERE status = :status AND enabled = :enabled
                """;
        List<Asset> assets = entityManager.createNativeQuery(dataSql, Asset.class)
                .setParameter("status", status.name())
                .setParameter("enabled", enabled)
                .setFirstResult((int)pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        return new PageImpl<>(assets, pageable, totalCount);
    }

    public List<Asset> findAllByStatusOrderByNameAsc(AssetStatus status) {
        String sql = """
                SELECT id, asset_tag, name, category_id, purchase_date, value, status, created_by_user_id, created_at, enabled
                FROM assets
                WHERE status = :status
                ORDER BY name ASC
                """;

        return entityManager.createNativeQuery(sql,Asset.class)
                .setParameter("status", status.name())
                .getResultList();
    }

    public long countByStatus(AssetStatus status)
    {
        String sql = """
                SELECT COUNT(*)
                FROM assets
                WHERE status = :status
                """;
        return ((Number)entityManager.createNativeQuery(sql)
                .setParameter("status", status.name())
                .getSingleResult())
                .longValue();
    }

    public long countByStatusAndEnabled(AssetStatus status, Boolean enabled)
    {
        String sql = """
                SELECT COUNT(*)
                FROM assets
                WHERE status = :status AND enabled = :enabled
                """;
        return ((Number)entityManager.createNativeQuery(sql)
                .setParameter("status", status.name())
                .setParameter("enabled", enabled)
                .getSingleResult())
                .longValue();
    }

    public Page<Asset> findByCategoryId(Long categoryId, Pageable pageable)
    {
        String totalCountSql = """
                SELECT COUNT(*)
                FROM assets
                WHERE category_id = :categoryId
                """;
        long totalCount = ((Number)entityManager.createNativeQuery(totalCountSql)
                .setParameter("categoryId", categoryId)
                .getSingleResult()).longValue();

        String sql = """
                SELECT id, asset_tag, name, category_id, purchase_date, value, status, created_by_user_id, created_at, enabled
                FROM assets
                WHERE category_id = :categoryId
                """;

        List<Asset> assets = entityManager.createNativeQuery(sql,Asset.class)
                .setParameter("categoryId", categoryId)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
        return new PageImpl<>(assets, pageable, totalCount);
    }

}
