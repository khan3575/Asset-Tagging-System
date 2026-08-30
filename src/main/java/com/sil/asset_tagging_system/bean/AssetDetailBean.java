package com.sil.asset_tagging_system.bean;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sil.asset_tagging_system.model.Asset;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.AssetCondition;
import com.sil.asset_tagging_system.model.enums.RoleName;
import com.sil.asset_tagging_system.service.ApprovalService;
import com.sil.asset_tagging_system.service.AssetService;
import com.sil.asset_tagging_system.service.UserService;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Named
@RequestScoped
public class AssetDetailBean {
    private final AssetService assetService;
    private final UserService userService;
    private final ApprovalService approvalService;

    private Long id;
    private Asset asset;
    private AssetCondition conditionStatus;
    private BigDecimal purchaseValue;
    private User currentHolder;
    private boolean transferPending;
    private List<User> availableHolders;

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

    }
    public void setId(Long id)
    {
        this.id = id;
    }
}
