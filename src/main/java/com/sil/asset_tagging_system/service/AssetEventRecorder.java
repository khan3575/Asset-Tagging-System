package com.sil.asset_tagging_system.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sil.asset_tagging_system.dao.AssetHistoryDao;
import com.sil.asset_tagging_system.dao.AuditLogDao;
import com.sil.asset_tagging_system.model.Approval;
import com.sil.asset_tagging_system.model.Asset;
import com.sil.asset_tagging_system.model.User;
import com.sil.asset_tagging_system.model.enums.AssetStatus;
import com.sil.asset_tagging_system.model.enums.HistoryAction;
import com.sil.asset_tagging_system.util.FacesUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AssetEventRecorder {
    private final AuditLogDao auditLogDao;
    private final AssetHistoryDao assetHistoryDao;

    @Transactional
    public void record(HistoryAction action, Asset asset, User performedBy, User previousHolder, User newHolder,
                    AssetStatus previousStatus, AssetStatus newStatus, Approval relatedApproval, String notes)
        {
            Long previousHolderId = previousHolder == null ? null : previousHolder.getId();
            Long newHolderId = newHolder == null ? null : newHolder.getId();
            Long approvalId = relatedApproval == null ? null : relatedApproval.getId();

            assetHistoryDao.insert(asset.getId(), action, performedBy.getId(), previousHolderId
                , newHolderId, previousStatus, newStatus, approvalId, notes);
            auditLogDao.log(performedBy.getId()
                , action.name(), "Asset", asset.getId(), "", FacesUtil.getRemoteAddress()
                );
        }

}
