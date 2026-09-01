package com.sil.asset_tagging_system.bean;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;

import com.sil.asset_tagging_system.dto.Actor;
import com.sil.asset_tagging_system.exception.BusinessRuleException;
import com.sil.asset_tagging_system.exception.DuplicateAssetTagException;
import com.sil.asset_tagging_system.model.Asset;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.AssetCondition;
import com.sil.asset_tagging_system.model.enums.RoleName;
import com.sil.asset_tagging_system.security.SecurityUtil;
import com.sil.asset_tagging_system.service.ApprovalService;
import com.sil.asset_tagging_system.service.AssetDocumentService;
import com.sil.asset_tagging_system.service.AssetService;
import com.sil.asset_tagging_system.service.UserService;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Named
@ViewScoped
public class AssetDetailBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private final transient AssetService assetService;
    private final transient UserService userService;
    private final transient ApprovalService approvalService;
    private final transient AssetDocumentService assetDocumentService;

    private Long id;
    private Asset asset;
    @Setter
    private AssetCondition conditionStatus;
    private BigDecimal purchaseValue;
    private User currentHolder;
    private boolean transferPending;
    private boolean heldByCurrentUser;

    private boolean hasImage;
    private boolean hasInvoice;
    @Setter
    private Part imageUpload;
    @Setter
    private Part invoiceUpload;

    private boolean editing;
    @Setter
    private String editorAssetTag;
    @Setter
    private String editorName;
    @Setter
    private Long editorCategoryId;
    @Setter
    private LocalDate editorPurchaseDate;
    @Setter
    private BigDecimal editorPurchaseValue;
    private List<User> availableHolders;
    private List<AssetCondition> conditionStatusList;
    @Setter
    private Long selectedHolderId;

    @Inject
    public AssetDetailBean(AssetService assetService
        , UserService userService
        , ApprovalService approvalService
        , AssetDocumentService assetDocumentService
    )
    {
        this.assetService = assetService;
        this.userService = userService;
        this.approvalService = approvalService;
        this.assetDocumentService = assetDocumentService;
    }

    public void load()
    {


        this.asset = assetService.getAsset(id);
        log.info("Asset is loaded - asset id : "+id);

        this.conditionStatus = asset.getConditionStatus();
        this.purchaseValue = asset.getPurchaseValue();

        this.currentHolder = approvalService.findActiveCustodianId(id)
                .flatMap(userService::findUser)
                .orElse(null);

        List<User> eligibleHolders = userService.findUsers(RoleName.ROLE_EMPLOYEE, null, null, true, 1000, 0);
        availableHolders = currentHolder == null
                ? eligibleHolders
                : eligibleHolders.stream()
                    .filter(user -> !user.getId().equals(currentHolder.getId()))
                    .collect(Collectors.toList());

        transferPending = approvalService.hasOpenRequest(id);
        heldByCurrentUser = currentHolder != null
                && currentHolder.getId().equals(SecurityUtil.currentUserId());

        hasImage = assetDocumentService.hasImage(id);
        hasInvoice = assetDocumentService.hasInvoice(id);

        conditionStatusList = List.of(AssetCondition.values());

    }
    public void setId(Long id)
    {
        this.id = id;
    }
    public String transfer()
    {
        Actor actor = Actor.current();
        Long previousHolderId = currentHolder != null ? currentHolder.getId() : null;

        try{
            log.info("AssetDetailBean.transfer initiated transfer {}, {}, {}", asset.getId(), actor.userId(), selectedHolderId);
            approvalService.initiateTransfer(asset.getId(), actor, selectedHolderId, previousHolderId);
        }
        catch(BusinessRuleException e)
        {
            log.warn("transfer error", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
            return null;
        }
        return "/asset/detail?id="+id+"&faces-redirect=true&includeViewParams=true";
    }

    public String uploadImage()
    {
        return upload(imageUpload, true);
    }

    public String uploadInvoice()
    {
        return upload(invoiceUpload, false);
    }

    private String upload(Part part, boolean isImage)
    {
        if (part == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "No file was selected", null));
            return null;
        }
        try (InputStream in = part.getInputStream()) {
            byte[] content = in.readAllBytes();
            String fileName = part.getSubmittedFileName();
            if (isImage) {
                assetDocumentService.storeImage(asset.getId(), content, part.getContentType(), fileName, Actor.current());
            } else {
                assetDocumentService.storeInvoice(asset.getId(), content, part.getContentType(), fileName, Actor.current());
            }
        }
        catch (BusinessRuleException e) {
            log.warn("document upload refused: {}", e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
            return null;
        }
        catch (IOException e) {
            log.error("failed to read uploaded file", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "The file could not be read", null));
            return null;
        }
        return "/asset/detail?id="+id+"&faces-redirect=true&includeViewParams=true";
    }

    public void edit()
    {
        editorAssetTag = asset.getAssetTag();
        editorName = asset.getName();
        editorCategoryId = asset.getCategory() == null ? null : asset.getCategory().getId();
        editorPurchaseDate = asset.getPurchaseDate();
        editorPurchaseValue = asset.getPurchaseValue();
        editing = true;
    }

    public void cancel()
    {
        editing = false;
    }

    public String save()
    {
        try {
            assetService.updateAsset(asset.getId(), editorAssetTag, editorName, editorCategoryId,
                    editorPurchaseDate, editorPurchaseValue, Actor.current());
        }
        catch (BusinessRuleException | DuplicateAssetTagException e) {
            log.warn("asset update refused: {}", e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
            return null;
        }
        editing = false;
        return "/asset/detail?id="+id+"&faces-redirect=true&includeViewParams=true";
    }

    public String requestForSelf()
    {
        Actor actor = Actor.current();
        Long previousHolderId = currentHolder != null ? currentHolder.getId() : null;
        try {
            approvalService.requestForSelf(asset.getId(), actor, previousHolderId);
        }
        catch (BusinessRuleException e) {
            log.warn("requestForSelf error", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
            return null;
        }
        return "/asset/detail?id="+id+"&faces-redirect=true&includeViewParams=true";
    }

    public String requestReturn()
    {
        Actor actor = Actor.current();
        try {
            approvalService.requestReturn(asset.getId(), actor);
        }
        catch (BusinessRuleException e) {
            log.warn("requestReturn error", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
            return null;
        }
        return "/asset/detail?id="+id+"&faces-redirect=true&includeViewParams=true";
    }

    public String changeCondition()
    {
        Actor actor = Actor.current();

        try{
            log.info("AssetDetailBean.changeCondition initiated condition change {}, {}, {}", asset.getId(), actor.userId(), conditionStatus);
            assetService.updateCondition(asset.getId(), actor, conditionStatus);
        }
        catch(IllegalArgumentException e)
        {
            log.warn("changeCondition error", e);
            FacesContext.getCurrentInstance().addMessage(null,new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
            return null;
        }

        return "/asset/detail?id="+id+"&faces-redirect=true&includeViewParams=true";
    }
}
