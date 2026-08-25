package com.sil.asset_tagging_system.dao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;

import com.sil.asset_tagging_system.model.Asset;
import com.sil.asset_tagging_system.model.enums.AssetCondition;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class AssetDao {
    private final EntityManager entityManager;

    public AssetDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    public Optional<Asset> findByAssetTagIgnoreCase(String assetTag) {
        String sql = """
                SELECT id , asset_tag, name, category_id, purchase_date, purchase_value, condition_status, created_by_user_id, created_at
                FROM assets
                WHERE LOWER(asset_tag) = LOWER(:assetTag)
                """;

        return entityManager.createNativeQuery(sql, Asset.class)
                .setParameter("assetTag", assetTag)
                .getResultStream()
                .findFirst();
    }

    @SuppressWarnings("unchecked")
    public Optional<Asset> findById(Long id)
    {
        String sql = """
                        SELECT id, asset_tag, name, category_id, purchase_date
                                , purchase_value, condition_status, created_by_user_id, created_at
                        FROM assets
                        WHERE id = :id
                        """;
        return entityManager.createNativeQuery(sql,Asset.class)
                .setParameter("id",id)
                .getResultStream()
                .map(Asset.class::cast)
                .findFirst();
    }


    @SuppressWarnings("unchecked")
    public List<Asset> findAll() {
        String sql = """
                SELECT id, asset_tag, name, category_id, purchase_date, purchase_value, condition_status, created_by_user_id, created_at
                FROM assets
                """;
        return entityManager.createNativeQuery(sql, Asset.class).getResultList();
    }

    /*
        Create Asset Method

    */

    
    public Long createAsset(String assetTag, String name, Long categoryId, LocalDate purchaseDate,
                             BigDecimal purchaseValue, Long createdByUserId) {
        String insertSql = """
                INSERT INTO assets (asset_tag, name, category_id, purchase_date
                    , purchase_value, condition_status, created_by_user_id)
                VALUES (:assetTag, :name, :categoryId, :purchaseDate, :purchaseValue, :conditionStatus, :createdByUserId)
                """;
        entityManager.createNativeQuery(insertSql)
                .setParameter("assetTag", assetTag)
                .setParameter("name", name)
                .setParameter("categoryId", categoryId)
                .setParameter("purchaseDate", purchaseDate)
                .setParameter("purchaseValue", purchaseValue)
                .setParameter("conditionStatus", AssetCondition.IN_SERVICE.name())
                .setParameter("createdByUserId", createdByUserId)
                .executeUpdate();

        return DaoUtils.getLastInsertId(entityManager);
    }



    public Boolean existsByAssetTagIgnoreCase(String assetTag) {
        String sql = """
                SELECT count(*)
                FROM assets
                WHERE LOWER(asset_tag) = LOWER(:assetTag)
                """;
        return DaoUtils.exists(entityManager, sql, Map.of("assetTag", assetTag));
    }



    public Boolean existsByAssetTagIgnoreCaseAndIdNot(String assetTag, Long id) {
        String sql = """
                SELECT count(*)
                FROM assets
                WHERE LOWER(asset_tag) = LOWER(:assetTag)
                AND id != :id
                """;
        return DaoUtils.exists(entityManager, sql, Map.of("assetTag", assetTag, "id", id));
    }


    /* 
        UPDATE ASSET METHOD
    */
    
    public void updateAsset(Long id, AssetCondition assetCondition, BigDecimal purchaseValue)
    {
        if (assetCondition == null) {
            throw new IllegalArgumentException("ConditionStaus must not be null");
        }
        String sql = """
                        UPDATE assets SET condition_status = :conditionStatus, purchase_value = :purchaseValue
                        WHERE id = :id
                        """;
        entityManager.createNativeQuery(sql)
        .setParameter("conditionStatus", assetCondition.name())
        .setParameter("purchaseValue", purchaseValue)
        .setParameter("id", id)
        .executeUpdate();
    }

    

}
