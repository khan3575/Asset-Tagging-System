package com.sil.asset_tagging_system.bean;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;

import com.sil.asset_tagging_system.dto.Actor;
import com.sil.asset_tagging_system.exception.BusinessRuleException;
import com.sil.asset_tagging_system.exception.DuplicateAssetTagException;
import com.sil.asset_tagging_system.service.AssetDocumentService;
import com.sil.asset_tagging_system.service.AssetService;
import com.sil.asset_tagging_system.util.FacesMessages;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@Named
@RequestScoped
public class AssetFormBean {

    private final AssetService assetService;
    private final AssetDocumentService assetDocumentService;

    private String assetTag;
    private String name;
    private Long categoryId;
    private LocalDate purchaseDate;
    private BigDecimal value;
    private Part imageUpload;
    private Part invoiceUpload;

    @Inject
    public AssetFormBean(AssetService assetService, AssetDocumentService assetDocumentService)
    {
        this.assetService = assetService;
        this.assetDocumentService = assetDocumentService;
    }
    public String save()
    {
        Long newAssetId;
        try
        {
            newAssetId = assetService.register(assetTag, name, categoryId, purchaseDate, value, Actor.current());
        }
        catch (DuplicateAssetTagException e)
        {
            FacesMessages.error("Asset tag already exists");
            return null;
        }
        catch (BusinessRuleException e)
        {
            FacesMessages.error(e.getMessage());
            return null;
        }

        boolean imageFailed = !uploadIfPresent(newAssetId, imageUpload, true);
        boolean invoiceFailed = !uploadIfPresent(newAssetId, invoiceUpload, false);

        if (imageFailed || invoiceFailed)
        {
            // Asset is already saved; stay on this page so the warning message is visible
            // instead of losing it across the redirect to the asset list.
            return null;
        }
        return "/asset/list?faces-redirect=true";
    }

    private boolean uploadIfPresent(Long assetId, Part part, boolean isImage)
    {
        if (part == null || part.getSize() <= 0)
        {
            return true;
        }
        try (InputStream in = part.getInputStream())
        {
            byte[] content = in.readAllBytes();
            String fileName = part.getSubmittedFileName();
            if (isImage)
            {
                assetDocumentService.storeImage(assetId, content, part.getContentType(), fileName, Actor.current());
            }
            else
            {
                assetDocumentService.storeInvoice(assetId, content, part.getContentType(), fileName, Actor.current());
            }
        }
        catch (BusinessRuleException e)
        {
            log.warn("document upload refused for new asset {}: {}", assetId, e.getMessage());
            FacesMessages.warn("Asset was created, but the " + (isImage ? "photograph" : "invoice")
                    + " could not be uploaded -- " + e.getMessage());
            return false;
        }
        catch (IOException e)
        {
            log.error("failed to read uploaded file for new asset {}", assetId, e);
            FacesMessages.warn("Asset was created, but the " + (isImage ? "photograph" : "invoice")
                    + " could not be read");
            return false;
        }
        return true;
    }
}
