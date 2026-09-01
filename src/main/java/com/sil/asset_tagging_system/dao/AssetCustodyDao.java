package com.sil.asset_tagging_system.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;

import com.sil.asset_tagging_system.dto.AssetRow;
import com.sil.asset_tagging_system.model.enums.CustodyStatus;

@Repository
public class AssetCustodyDao {
    private final EntityManager entityManager;
    public AssetCustodyDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    public Optional<Long> findActiveCustodianId(Long assetId) {
        String sql = """
                SELECT custodian_id
                FROM asset_custody
                WHERE asset_id = :assetId AND status = :status
                """;

        return entityManager.createNativeQuery(sql)
                .setParameter("assetId", assetId)
                .setParameter("status", CustodyStatus.ACTIVE.name())
                .getResultStream()
                .map(row -> ((Number) row).longValue())
                .findFirst();
    }
    public void releaseActiveCustody(Long assetId)
    {
        String sql = """
                UPDATE asset_custody
                SET custody_end = CURRENT_TIMESTAMP, status = 'RELEASED'
                WHERE  asset_id = :assetId AND status = 'ACTIVE'
                """;
        entityManager.createNativeQuery(sql)
            .setParameter("assetId", assetId)
            .executeUpdate();
    }

    public void initiateNewCustody(long assetId, long newCustodianId, long approvalId, long assignedByUserId)
    {
        String sql = """
            INSERT INTO asset_custody (asset_id, custodian_id, approval_id, assigned_by_user_id, status) 
            VALUES (:assetId, :newCustodianId, :approvalId, :assignedByUserId, 'ACTIVE')
            """;

        entityManager.createNativeQuery(sql)
            .setParameter("assetId", assetId)
            .setParameter("newCustodianId", newCustodianId)
            .setParameter("approvalId", approvalId)
            .setParameter("assignedByUserId", assignedByUserId)
            .executeUpdate();
    }
    
    public void transferCustody(long assetId, Long newCustodianId, long approvalId, long assignedByUserId)
    {
        releaseActiveCustody(assetId);
        if (newCustodianId != null) {
            initiateNewCustody(assetId, newCustodianId, approvalId, assignedByUserId);
        }
    }

    public List<AssetRow> findAssetsHeldBy(Long custodianId)
    {
        String sql = """
                SELECT a.id, a.asset_tag, a.name, c.name AS category_name
                     , a.purchase_date, a.purchase_value, a.condition_status
                FROM asset_custody ac
                JOIN assets a ON ac.asset_id = a.id
                JOIN asset_categories c ON a.category_id = c.id
                WHERE ac.custodian_id = :custodianId AND ac.status = :status
                ORDER BY a.asset_tag
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("custodianId", custodianId)
                .setParameter("status", CustodyStatus.ACTIVE.name())
                .getResultList();

        List<AssetRow> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(AssetRow.fromRow(row));
        }
        return result;
    }
}
