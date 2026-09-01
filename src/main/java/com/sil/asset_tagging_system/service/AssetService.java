package com.sil.asset_tagging_system.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sil.asset_tagging_system.dao.AssetCustodyDao;
import com.sil.asset_tagging_system.dao.AssetDao;
import com.sil.asset_tagging_system.dto.Actor;
import com.sil.asset_tagging_system.dto.AssetRow;
import com.sil.asset_tagging_system.exception.DuplicateAssetTagException;
import com.sil.asset_tagging_system.model.Asset;
import com.sil.asset_tagging_system.model.enums.ActivityAction;
import com.sil.asset_tagging_system.model.enums.ActivityEntityType;
import com.sil.asset_tagging_system.model.enums.AssetCondition;
import com.sil.asset_tagging_system.util.OptionalUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetDao assetDao;
    private final AuditTrail auditTrail;
    private final AssetCustodyDao assetCustodyDao;
    
    @Transactional
    public Long register(String assetTag, String name, Long categoryId,
                         LocalDate purchaseDate, BigDecimal value, Actor actor) {

        if (assetDao.existsByAssetTagIgnoreCase(assetTag)) {
            log.warn("AssetService.register -> duplicate asset tag '{}' rejected (actor {})",
                    assetTag, actor.userId());
            // refused(...) also routes this through the REQUIRES_NEW path, so the row
            // survives the rollback the throw below triggers.
            auditTrail.record(ActivityAction.ASSET_REGISTERED, ActivityEntityType.ASSET)
                    .by(actor)
                    .refused("Asset tag already exists: " + assetTag)
                    .summary("Registration refused -- duplicate asset tag " + assetTag)
                    .save();
            throw new DuplicateAssetTagException("Asset tag already exists: " + assetTag);
        }
        Long newAssetId = assetDao.createAsset(assetTag, name, categoryId,
                purchaseDate, value, actor.userId());

        auditTrail.record(ActivityAction.ASSET_REGISTERED, ActivityEntityType.ASSET)
                .by(actor)
                .asset(newAssetId)
                .summary("Registered asset " + assetTag)
                .save();

        log.info("AssetService.register -> asset '{}' created id {} by actor {}",
                assetTag, newAssetId, actor.userId());
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
    public void updateCondition(Long assetId, Actor actor, LocalDateTime endTime, AssetCondition assetCondition)
        {
            
            if(assetCondition == null)
            {
                log.warn("AssetService.updateCondition() -> asset condition is null");
                throw new IllegalArgumentException("asset condition cant be null");
            }
            Asset asset = getAsset(assetId);
            AssetCondition previousCondition = asset.getConditionStatus();

            boolean forcesRelease = assetCondition == AssetCondition.DAMAGED || assetCondition == AssetCondition.UNDER_MAINTENANCE;
            Long releasedHolderId = null;
            if(forcesRelease)
            {
                log.info("Asset is damaged or under maintenance force release executed");
                releasedHolderId = assetCustodyDao.findActiveCustodianId(assetId).orElse(null);
                assetCustodyDao.releaseActiveCustody(assetId, endTime);
            }
            log.info("AssetService.updateCondition -> executing update ");
            assetDao.updateCondition(assetId, assetCondition);

            auditTrail.record(ActivityAction.ASSET_CONDITION_CHANGED, ActivityEntityType.ASSET)
                .by(actor)
                .asset(assetId)
                .condition(previousCondition, assetCondition)
                .summary("Condition of asset " + asset.getAssetTag() + " changed from "
                        + previousCondition + " to " + assetCondition.name())
                .save();

            if (forcesRelease && releasedHolderId != null)
            {
                auditTrail.record(ActivityAction.CUSTODY_RELEASED, ActivityEntityType.ASSET)
                        .by(actor)
                        .sequence(2)
                        .asset(assetId)
                        .holder(releasedHolderId, null)
                        .condition(previousCondition, assetCondition)
                        .summary("Custody of asset " + asset.getAssetTag() + " force-released -- condition set to " + assetCondition.name())
                        .save();
            }
        }
}
