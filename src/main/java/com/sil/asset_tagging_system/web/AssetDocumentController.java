package com.sil.asset_tagging_system.web;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.sil.asset_tagging_system.dto.StoredDocument;
import com.sil.asset_tagging_system.service.AssetDocumentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AssetDocumentController {

    private final AssetDocumentService assetDocumentService;

    @GetMapping("/asset/document/{assetId}/image")
    public ResponseEntity<byte[]> image(@PathVariable Long assetId) {
        return assetDocumentService.findImage(assetId)
                .map(doc -> respond(doc, "asset-" + assetId + "-image", ContentDisposition.inline()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/asset/document/{assetId}/invoice")
    public ResponseEntity<byte[]> invoice(@PathVariable Long assetId) {
        return assetDocumentService.findInvoice(assetId)
                .map(doc -> respond(doc, "asset-" + assetId + "-invoice.pdf", ContentDisposition.attachment()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ResponseEntity<byte[]> respond(StoredDocument doc, String fileName,
                                           ContentDisposition.Builder disposition) {
        MediaType mediaType = doc.mimeType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(doc.mimeType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.filename(fileName).build().toString())
                .body(doc.content());
    }
}
