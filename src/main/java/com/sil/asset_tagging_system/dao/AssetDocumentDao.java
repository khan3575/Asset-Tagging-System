package com.sil.asset_tagging_system.dao;

import java.util.Map;
import java.util.Optional;

import com.sil.asset_tagging_system.dto.StoredDocument;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;

@Repository
public class AssetDocumentDao {
    private final EntityManager entityManager;

    public AssetDocumentDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void upsertImage(Long assetId, byte[] content, String mimeType) {
        String sql = """
                INSERT INTO asset_documents (asset_id, asset_image, image_mime_type)
                VALUES (:assetId, :content, :mimeType)
                ON DUPLICATE KEY UPDATE asset_image = :content, image_mime_type = :mimeType
                """;
        entityManager.createNativeQuery(sql)
                .setParameter("assetId", assetId)
                .setParameter("content", content)
                .setParameter("mimeType", mimeType)
                .executeUpdate();
    }

    public void upsertInvoice(Long assetId, byte[] content, String mimeType) {
        String sql = """
                INSERT INTO asset_documents (asset_id, invoice_pdf, invoice_mime_type)
                VALUES (:assetId, :content, :mimeType)
                ON DUPLICATE KEY UPDATE invoice_pdf = :content, invoice_mime_type = :mimeType
                """;
        entityManager.createNativeQuery(sql)
                .setParameter("assetId", assetId)
                .setParameter("content", content)
                .setParameter("mimeType", mimeType)
                .executeUpdate();
    }

    public Optional<StoredDocument> findImage(Long assetId) {
        return findDocument("SELECT asset_image, image_mime_type FROM asset_documents WHERE asset_id = :assetId", assetId);
    }

    public Optional<StoredDocument> findInvoice(Long assetId) {
        return findDocument("SELECT invoice_pdf, invoice_mime_type FROM asset_documents WHERE asset_id = :assetId", assetId);
    }

    @SuppressWarnings("unchecked")
    private Optional<StoredDocument> findDocument(String sql, Long assetId) {
        return (Optional<StoredDocument>) entityManager.createNativeQuery(sql)
                .setParameter("assetId", assetId)
                .getResultStream()
                .findFirst()
                .map(result -> {
                    Object[] row = (Object[]) result;
                    return row[0] == null ? null : new StoredDocument((byte[]) row[0], (String) row[1]);
                });
    }

    public boolean hasImage(Long assetId) {
        return DaoUtils.exists(entityManager,
                "SELECT COUNT(*) FROM asset_documents WHERE asset_id = :assetId AND asset_image IS NOT NULL",
                Map.of("assetId", assetId));
    }

    public boolean hasInvoice(Long assetId) {
        return DaoUtils.exists(entityManager,
                "SELECT COUNT(*) FROM asset_documents WHERE asset_id = :assetId AND invoice_pdf IS NOT NULL",
                Map.of("assetId", assetId));
    }

    public boolean existsByAssetId(Long assetId) {
        String sql = """
                SELECT COUNT(*)
                FROM asset_documents
                WHERE asset_id = :assetId
                """;
        return DaoUtils.exists(entityManager, sql, Map.of("assetId", assetId));
    }
}
