package com.sil.asset_tagging_system.bean;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sil.asset_tagging_system.exception.BusinessRuleException;
import com.sil.asset_tagging_system.model.Asset;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.AssetCondition;
import com.sil.asset_tagging_system.model.enums.RoleName;
import com.sil.asset_tagging_system.security.SecurityUtil;
import com.sil.asset_tagging_system.service.ApprovalService;
import com.sil.asset_tagging_system.service.AssetService;
import com.sil.asset_tagging_system.service.UserService;
import com.sil.asset_tagging_system.util.WebUtil;

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

    private Long id;
    private Asset asset;
    @Setter
    private AssetCondition conditionStatus;
    private BigDecimal purchaseValue;
    private User currentHolder;
    private boolean transferPending;
    private List<User> availableHolders;
    private List<AssetCondition> conditionStatusList;
    @Setter
    private Long selectedHolderId;

    @Inject
    public AssetDetailBean(AssetService assetService
        , UserService userService
        , ApprovalService approvalService
    )
    {
        this.assetService = assetService;
        this.userService = userService;
        this.approvalService = approvalService;
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

        transferPending = approvalService.hasOpenTransferRequest(id);

        conditionStatusList = List.of(AssetCondition.values());

    }
    public void setId(Long id)
    {
        this.id = id;
    }
    public String transfer()
    {
        Long actorUserId = SecurityUtil.currentUserId();
        String primaryRole = SecurityUtil.primaryRole();
        Long previousHolderId = currentHolder != null ? currentHolder.getId() : null;

        try{
            log.info("AssetDetailBean.transfer initiated transfer {}, {}, {}", asset.getId(), actorUserId, selectedHolderId);
            approvalService.initiateTransfer(asset.getId(), actorUserId, selectedHolderId, previousHolderId,
                    primaryRole, WebUtil.getRemoteAddress());
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

    public String changeCondition()
    {
        Long actorUserId = SecurityUtil.currentUserId();
        String primaryRole = SecurityUtil.primaryRole();
        String ipAddress = WebUtil.getRemoteAddress();

        try{
            log.info("AssetDetailBean.changeCondition initiated condition change {}, {}, {}", asset.getId(), actorUserId, conditionStatus);
            assetService.updateCondition(asset.getId(), actorUserId, ipAddress, primaryRole, LocalDateTime.now(), conditionStatus);
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
