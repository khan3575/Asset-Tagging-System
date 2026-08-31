package com.sil.asset_tagging_system.dao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;

import com.sil.asset_tagging_system.dto.AssetRow;
import com.sil.asset_tagging_system.exception.UnauthorizedOperationException;
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

    @SuppressWarnings("unchecked")
    public List<AssetRow> findPage(String search, int limit, int offset)
    {
        // currently asset searching is done (name, category_name, asset_tag)
        String sql = """
                SELECT a.id, a.asset_tag, a.name, c.name AS category_name, a.purchase_date, a.purchase_value, a.condition_status
                FROM assets a
                JOIN asset_categories c ON a.category_id = c.id
                WHERE (:search= '' OR LOWER(a.asset_tag) LIKE :search OR LOWER(a.name) LIKE :search OR LOWER(c.name) LIKE :search)
                ORDER BY a.id
                LIMIT :limit
                OFFSET   :offset
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                            .setParameter("search", "%" + search + "%")
                            .setParameter("limit", limit)
                            .setParameter("offset", offset)
                            .getResultList();

        List<AssetRow> result = new ArrayList<>();
        for(Object[] row : rows)
        {
            result.add(new AssetRow(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (String) row[2],
                    (String) row[3],
                    (LocalDate) row[4],
                    (BigDecimal) row[5],
                   AssetCondition.valueOf((String)row[6])
                )
            );
        }
        return result;
    }
    public long countAssets(String search) {
    String sql = """
            SELECT count(*)
            FROM assets a
            JOIN asset_categories c ON a.category_id = c.id
            WHERE (:search = '' OR LOWER(a.asset_tag) LIKE :search OR LOWER(a.name) LIKE :search OR LOWER(c.name) LIKE :search)
            """;
    return ((Number) entityManager.createNativeQuery(sql)
            .setParameter("search", "%" + search + "%")
            .getSingleResult()).longValue();
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

    public void updateCondition(Long id, AssetCondition assetCondition)
    {
        String sql = """
                        UPDATE assets SET condition_status = :conditionStatus
                        WHERE id = :id
                        """;
        if(assetCondition == null)
        {
            log.warn("asset condition is null throwing exception");
            throw new UnauthorizedOperationException("asset condition cant be null");
        }

        log.info("AssetDao.updateCondition -> executing the query");
        entityManager.createNativeQuery(sql)
            .setParameter("conditionStatus",assetCondition.name())
            .setParameter("id", id)
            .executeUpdate();
    }
    

}
