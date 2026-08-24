package com.sil.asset_tagging_system.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sil.asset_tagging_system.dao.ActivityLogDao;
import com.sil.asset_tagging_system.dao.AssetDao;
import com.sil.asset_tagging_system.model.ActivityLog;
import com.sil.asset_tagging_system.model.enums.ActivityAction;
import com.sil.asset_tagging_system.model.enums.ActivityEntityType;
import com.sil.asset_tagging_system.model.enums.ActivityOutcome;
import com.sil.asset_tagging_system.security.CorrelationFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetDao assetDao;
    private final ActivityLogDao activityLogDao;
    @Transactional
    public Long register(String assetTag, String name, Long categoryId,
                         LocalDate purchaseDate, BigDecimal value, Long actorUserId) {

        if (assetDao.existsByAssetTagIgnoreCase(assetTag)) {
            log.warn("AssetService.register -> duplicate asset tag '{}' rejected (actor {})",
                    assetTag, actorUserId);
            throw new DuplicateAssetTagException("Asset tag already exists: " + assetTag);
        }
        Long newAssetId = assetDao.createAsset(assetTag, name, categoryId,
                purchaseDate, value, actorUserId);
                
        ActivityLog act = ActivityLog.builder()
                .correlationId(CorrelationFilter.CURRENT.get())
                .sequenceInAction((short) 1)
                .actorUserId(actorUserId)
                .entityType(ActivityEntityType.ASSET)
                .action(ActivityAction.ASSET_REGISTERED)
                .outcome(ActivityOutcome.SUCCEEDED)
                .assetId(newAssetId)
                .summary("Registered asset " + assetTag)
                .build();
        activityLogDao.log(act);

        log.info("AssetService.register -> asset '{}' created id {} by actor {}",
                assetTag, newAssetId, actorUserId);
        return newAssetId;
    }
}
