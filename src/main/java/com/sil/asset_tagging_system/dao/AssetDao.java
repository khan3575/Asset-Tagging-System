package com.sil.asset_tagging_system.dao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

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

}
