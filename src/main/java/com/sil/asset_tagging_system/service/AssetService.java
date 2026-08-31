package com.sil.asset_tagging_system.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sil.asset_tagging_system.dao.ActivityLogDao;
import com.sil.asset_tagging_system.dao.AssetCustodyDao;
import com.sil.asset_tagging_system.dao.AssetDao;
import com.sil.asset_tagging_system.dto.AssetRow;
import com.sil.asset_tagging_system.exception.DuplicateAssetTagException;
import com.sil.asset_tagging_system.model.ActivityLog;
import com.sil.asset_tagging_system.model.Asset;
import com.sil.asset_tagging_system.model.enums.ActivityAction;
import com.sil.asset_tagging_system.model.enums.ActivityEntityType;
import com.sil.asset_tagging_system.model.enums.ActivityOutcome;
import com.sil.asset_tagging_system.model.enums.AssetCondition;
import com.sil.asset_tagging_system.model.enums.RoleName;
import com.sil.asset_tagging_system.security.CorrelationFilter;
import com.sil.asset_tagging_system.util.OptionalUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetDao assetDao;
    private final ActivityLogDao activityLogDao;
    private final AssetCustodyDao assetCustodyDao;
    
    @Transactional
    public Long register(String assetTag, String name, Long categoryId,
                         LocalDate purchaseDate, BigDecimal value, Long actorUserId, String actorRole, String ipAddress) {

        if (assetDao.existsByAssetTagIgnoreCase(assetTag)) {
            log.warn("AssetService.register -> duplicate asset tag '{}' rejected (actor {})",
                    assetTag, actorUserId);
            throw new DuplicateAssetTagException("Asset tag already exists: " + assetTag);
        }
        Long newAssetId = assetDao.createAsset(assetTag, name, categoryId,
                purchaseDate, value, actorUserId);

        //activity log needs ip and actor roles
        ActivityLog act = ActivityLog.builder()
                .correlationId(CorrelationFilter.getCurrentCorrelationId())
                .sequenceInAction((short) 1)
                .actorUserId(actorUserId)
                .entityType(ActivityEntityType.ASSET)
                .action(ActivityAction.ASSET_REGISTERED)
                .outcome(ActivityOutcome.SUCCEEDED)
                .assetId(newAssetId)
                .summary("Registered asset " + assetTag)
                .ipAddress(ipAddress)
                .actorRoles((actorRole == null)? null : RoleName.valueOf(actorRole))
                .build();
        activityLogDao.log(act);

        log.info("AssetService.register -> asset '{}' created id {} by actor {}",
                assetTag, newAssetId, actorUserId);
        return newAssetId;
    }

    public Asset getAsset(Long id)
    {
        return OptionalUtils.orThrowDbFetch(assetDao.findById(id), "Asset");
    }

    public List<AssetRow> findPage(String search , int limit, int offset)
    {
        return assetDao.findPage(search, limit, offset);
    }
    public long countAssets(String search)
    {
        return assetDao.countAssets(search);
    }

    @Transactional
    public void updateCondition(Long assetId, Long actorUserId, String ipAddress, String actorRole, LocalDateTime endTime, AssetCondition assetCondition)
        {
            
            if(assetCondition == null)
            {
                
                log.warn("AssetService.updateCondition() -> asset condition is null");
                throw new IllegalArgumentException("asset condition cant be null");
            }
            if(assetCondition == AssetCondition.DAMAGED || assetCondition == AssetCondition.UNDER_MAINTENANCE)
            {
                log.info("Asset is damaged or under maintenance force release executed");
                assetCustodyDao.releaseActiveCustody(assetId, endTime);
            }
            log.info("AssetService.updateCondition -> executing update ");
            assetDao.updateCondition(assetId, assetCondition);
            
            ActivityLog act = ActivityLog.builder()
                .correlationId(CorrelationFilter.getCurrentCorrelationId())
                .sequenceInAction((short) 1)
                .actorUserId(actorUserId)
                .entityType(ActivityEntityType.ASSET)
                .action(ActivityAction.ASSET_CONDITION_CHANGED)
                .outcome(ActivityOutcome.SUCCEEDED)
                .assetId(assetId)
                .summary("update asset condition for tag : "+getAsset(assetId).getAssetTag() + "Condition : "+ assetCondition.name())
                .ipAddress(ipAddress)
                .actorRoles((actorRole == null)? null : RoleName.valueOf(actorRole))
                .build();
            activityLogDao.log(act);
        }
}
