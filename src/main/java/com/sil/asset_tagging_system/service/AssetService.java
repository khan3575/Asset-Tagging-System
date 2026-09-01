package com.sil.asset_tagging_system.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sil.asset_tagging_system.dao.AssetCustodyDao;
import com.sil.asset_tagging_system.dao.AssetDao;
import com.sil.asset_tagging_system.dto.Actor;
import com.sil.asset_tagging_system.dto.AssetRow;
import com.sil.asset_tagging_system.exception.BusinessRuleException;
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

        String violation = validateForRegistration(assetTag, name, categoryId, purchaseDate, value);
        if (violation != null) {
            log.warn("AssetService.register -> rejected '{}': {}", assetTag, violation);
            auditTrail.record(ActivityAction.ASSET_REGISTERED, ActivityEntityType.ASSET)
                    .by(actor)
                    .refused(violation)
                    .summary("Registration refused -- " + violation)
                    .save();
            throw new BusinessRuleException(violation);
        }

        if (assetDao.existsByAssetTagIgnoreCase(assetTag)) {
            log.warn("AssetService.register -> duplicate asset tag '{}' rejected (actor {})",
                    assetTag, actor.userId());
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

    @Transactional
    public void updateAsset(Long assetId, String assetTag, String name, Long categoryId,
                            LocalDate purchaseDate, BigDecimal purchaseValue, Actor actor)
    {
        Asset before = getAsset(assetId);

        String violation = validateForRegistration(assetTag, name, categoryId, purchaseDate, purchaseValue);
        if (violation == null && assetDao.existsByAssetTagIgnoreCaseAndIdNot(assetTag, assetId)) {
            violation = "Asset tag already exists: " + assetTag;
        }
        if (violation != null) {
            auditTrail.record(ActivityAction.ASSET_UPDATED, ActivityEntityType.ASSET)
                    .by(actor)
                    .asset(assetId)
                    .refused(violation)
                    .summary("Update refused for asset " + before.getAssetTag() + " -- " + violation)
                    .save();
            throw new BusinessRuleException(violation);
        }

        assetDao.updateAsset(assetId, assetTag, name, categoryId, purchaseDate, purchaseValue);

        String changes = describeChanges(before, assetTag, name, categoryId, purchaseDate, purchaseValue);
        auditTrail.record(ActivityAction.ASSET_UPDATED, ActivityEntityType.ASSET)
                .by(actor)
                .asset(assetId)
                .summary(changes == null
                        ? "Asset " + assetTag + " saved with no changes"
                        : "Updated asset " + assetTag)
                .details(changes)
                .save();

        log.info("AssetService.updateAsset -> asset {} updated by actor {}", assetId, actor.userId());
    }
    
    private String describeChanges(Asset before, String assetTag, String name, Long categoryId,
                                   LocalDate purchaseDate, BigDecimal purchaseValue)
    {
        StringBuilder json = new StringBuilder();
        appendChange(json, "assetTag", before.getAssetTag(), assetTag);
        appendChange(json, "name", before.getName(), name);
        appendChange(json, "categoryId",
                before.getCategory() == null ? null : String.valueOf(before.getCategory().getId()),
                String.valueOf(categoryId));
        appendChange(json, "purchaseDate", String.valueOf(before.getPurchaseDate()), String.valueOf(purchaseDate));
        boolean valueMoved = (before.getPurchaseValue() == null) != (purchaseValue == null)
                || (before.getPurchaseValue() != null && purchaseValue != null
                    && before.getPurchaseValue().compareTo(purchaseValue) != 0);
        if (valueMoved) {
            appendChange(json, "purchaseValue",
                    String.valueOf(before.getPurchaseValue()), String.valueOf(purchaseValue));
        }

        return json.isEmpty() ? null : "{\"changed\":{" + json + "}}";
    }

    private void appendChange(StringBuilder json, String field, String before, String after)
    {
        if (java.util.Objects.equals(before, after)) {
            return;
        }
        if (!json.isEmpty()) {
            json.append(",");
        }
        json.append("\"").append(field).append("\":{\"from\":\"").append(escape(before))
            .append("\",\"to\":\"").append(escape(after)).append("\"}");
    }

    private static String escape(String value)
    {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String validateForRegistration(String assetTag, String name, Long categoryId,
                                           LocalDate purchaseDate, BigDecimal value)
    {
        if (assetTag == null || assetTag.isBlank()) {
            return "Asset tag is required";
        }
        if (name == null || name.isBlank()) {
            return "Asset name is required";
        }
        if (categoryId == null) {
            return "Category is required";
        }
        if (value != null && value.compareTo(BigDecimal.ZERO) <= 0) {
            return "Purchase value must be greater than zero";
        }
        if (purchaseDate != null && purchaseDate.isAfter(LocalDate.now())) {
            return "Purchase date cannot be in the future";
        }
        return null;
    }

    public Asset getAsset(Long id)
    {
        return OptionalUtils.orThrowDbFetch(assetDao.findById(id), "Asset");
    }

    public List<AssetRow> findPage(String search , int limit, int offset)
    {
        return assetDao.findPage(search, limit, offset);
    }
    public Map<AssetCondition, Long> countByCondition()
    {
        return assetDao.countByCondition();
    }

    public List<AssetRow> findAssetsHeldBy(Long custodianId)
    {
        return assetCustodyDao.findAssetsHeldBy(custodianId);
    }

    public long countAssets(String search)
    {
        return assetDao.countAssets(search);
    }

    @Transactional
    public void updateCondition(Long assetId, Actor actor, AssetCondition assetCondition)
        {
            
            if(assetCondition == null)
            {
                log.warn("AssetService.updateCondition() -> asset condition is null");
                throw new IllegalArgumentException("asset condition cant be null");
            }
            Asset asset = getAsset(assetId);
            AssetCondition previousCondition = asset.getConditionStatus();
            
            boolean forcesRelease = assetCondition == AssetCondition.DAMAGED
                    || assetCondition == AssetCondition.UNDER_MAINTENANCE
                    || assetCondition == AssetCondition.BEYOND_REPAIR
                    || assetCondition == AssetCondition.RETIRED;
            Long releasedHolderId = null;
            if(forcesRelease)
            {
                log.info("Asset is damaged or under maintenance force release executed");
                releasedHolderId = assetCustodyDao.findActiveCustodianId(assetId).orElse(null);
                assetCustodyDao.releaseActiveCustody(assetId);
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
