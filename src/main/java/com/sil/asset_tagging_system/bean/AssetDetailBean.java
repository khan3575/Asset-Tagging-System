package com.sil.asset_tagging_system.bean;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sil.asset_tagging_system.dao.ApprovalDao;
import com.sil.asset_tagging_system.dao.AssetCustodyDao;
import com.sil.asset_tagging_system.dao.AssetDao;
import com.sil.asset_tagging_system.dao.UserDao;
import com.sil.asset_tagging_system.model.Asset;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.AssetCondition;
import com.sil.asset_tagging_system.model.enums.RoleName;
import com.sil.asset_tagging_system.util.OptionalUtils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Named
@RequestScoped
public class AssetDetailBean {
    private final AssetDao assetDao;
    private final AssetCustodyDao assetCustodyDao;
    private final ApprovalDao approvalDao;
    private final UserDao userDao;

    private Long id;
    private Asset asset;
    private AssetCondition conditionStatus;
    private BigDecimal purchaseValue;
    private User currentHolder;
    private boolean transferPending;
    private List<User> availableHolders;

    @Inject
    public AssetDetailBean(AssetDao assetDao
        , AssetCustodyDao assetCustodyDao
        , ApprovalDao approvalDao
        , UserDao userDao
    )
    {
        this.assetDao = assetDao;
        this.assetCustodyDao = assetCustodyDao;
        this.approvalDao = approvalDao;
        this.userDao = userDao;
    }

    public void load()
    {
        
        
        this.asset = OptionalUtils.orThrowDbFetch(assetDao.findById(id), "Asset");
        log.info("Asset is loaded - asset id : "+id);

        this.conditionStatus = asset.getConditionStatus();
        this.purchaseValue = asset.getPurchaseValue();

        this.currentHolder = assetCustodyDao.findActiveCustodianId(id)
                .flatMap(userDao::findById)
                .orElse(null);

        List<User> eligibleHolders = userDao.findUsers(RoleName.ROLE_EMPLOYEE, null, null, true, 1000, 0);
        availableHolders = currentHolder == null
                ? eligibleHolders
                : eligibleHolders.stream()
                    .filter(user -> !user.getId().equals(currentHolder.getId()))
                    .collect(Collectors.toList());

        transferPending = approvalDao.existsOpenTransferRequest(id);
        
    }
    public void setId(Long id)
    {
        this.id = id;
    }
}
