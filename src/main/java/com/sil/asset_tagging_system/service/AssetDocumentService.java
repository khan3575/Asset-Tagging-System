package com.sil.asset_tagging_system.service;

import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sil.asset_tagging_system.dao.AssetDocumentDao;
import com.sil.asset_tagging_system.dto.Actor;
import com.sil.asset_tagging_system.dto.StoredDocument;
import com.sil.asset_tagging_system.exception.BusinessRuleException;
import com.sil.asset_tagging_system.model.enums.ActivityAction;
import com.sil.asset_tagging_system.model.enums.ActivityEntityType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetDocumentService {

    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    public static final long MAX_INVOICE_BYTES = 10L * 1024 * 1024;

    private static final Set<String> IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/gif", "image/webp");
    private static final Set<String> INVOICE_TYPES = Set.of("application/pdf");

    private final AssetDocumentDao assetDocumentDao;
    private final AuditTrail auditTrail;

    @Transactional
    public void storeImage(Long assetId, byte[] content, String mimeType, String fileName, Actor actor) {
        store(assetId, content, mimeType, fileName, actor, IMAGE_TYPES, MAX_IMAGE_BYTES, "image",
                () -> assetDocumentDao.upsertImage(assetId, content, mimeType));
    }

    @Transactional
    public void storeInvoice(Long assetId, byte[] content, String mimeType, String fileName, Actor actor) {
        store(assetId, content, mimeType, fileName, actor, INVOICE_TYPES, MAX_INVOICE_BYTES, "invoice",
                () -> assetDocumentDao.upsertInvoice(assetId, content, mimeType));
    }

    private void store(Long assetId, byte[] content, String mimeType, String fileName, Actor actor,
                       Set<String> allowedTypes, long maxBytes, String kind, Runnable write) {

        String violation = null;
        if (content == null || content.length == 0) {
            violation = "No file was selected";
        } else if (content.length > maxBytes) {
            violation = "File is larger than the " + (maxBytes / (1024 * 1024)) + " MB limit for an " + kind;
        } else if (mimeType == null || !allowedTypes.contains(mimeType.toLowerCase())) {
            violation = "Unsupported file type for an " + kind + ": " + mimeType;
        }

        if (violation != null) {
            auditTrail.record(ActivityAction.ASSET_DOCUMENT_UPLOADED, ActivityEntityType.ASSET)
                    .by(actor)
                    .asset(assetId)
                    .refused(violation)
                    .summary("Document upload refused for asset " + assetId + " -- " + violation)
                    .save();
            throw new BusinessRuleException(violation);
        }

        write.run();

        auditTrail.record(ActivityAction.ASSET_DOCUMENT_UPLOADED, ActivityEntityType.ASSET)
                .by(actor)
                .asset(assetId)
                .summary("Uploaded " + kind + " for asset " + assetId)
                .details("{\"kind\":\"" + kind + "\",\"fileName\":\"" + escape(fileName)
                        + "\",\"mimeType\":\"" + escape(mimeType) + "\",\"bytes\":" + content.length + "}")
                .save();

        log.info("AssetDocumentService -> {} stored for asset {} by actor {} ({} bytes)",
                kind, assetId, actor.userId(), content.length);
    }

    public Optional<StoredDocument> findImage(Long assetId) {
        return assetDocumentDao.findImage(assetId);
    }

    public Optional<StoredDocument> findInvoice(Long assetId) {
        return assetDocumentDao.findInvoice(assetId);
    }

    public boolean hasImage(Long assetId) {
        return assetDocumentDao.hasImage(assetId);
    }

    public boolean hasInvoice(Long assetId) {
        return assetDocumentDao.hasInvoice(assetId);
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
