package com.sil.asset_tagging_system.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sil.asset_tagging_system.dao.ApprovalDao;
import com.sil.asset_tagging_system.dao.ApprovalDao.ApprovalSnapshot;
import com.sil.asset_tagging_system.dao.AssetCustodyDao;
import com.sil.asset_tagging_system.dto.ApprovalRow;
import com.sil.asset_tagging_system.exception.BusinessRuleException;
import com.sil.asset_tagging_system.model.enums.ApprovalActionType;
import com.sil.asset_tagging_system.model.enums.ApprovalStatus;
import com.sil.asset_tagging_system.model.enums.RoleName;
import com.sil.asset_tagging_system.util.OptionalUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {
    private final AssetCustodyDao assetCustodyDao;
    private final ApprovalDao approvalDao;

    public boolean hasOpenTransferRequest(Long assetId) {
        return approvalDao.existsOpenTransferRequest(assetId);
    }

    // custody dao and approval doa both managed by approval service.
    // i didnt create custody service yet.
    public Optional<Long> findActiveCustodianId(Long assetId) {
        return assetCustodyDao.findActiveCustodianId(assetId);
    }
    
    // if admin created transfer then approve already counts 1
    // if employee request transfer then approve count 0 two admin approval needed.
    @Transactional
    public Long initiateTransfer(Long assetId, Long initiatedByUserId, Long requesterId,Long previousHolderId, String initiatorRole) {
        if(approvalDao.existsOpenTransferRequest(assetId))
        {
            throw new BusinessRuleException("A transfer is already pending for this asset");
        }
        Long approvalId = approvalDao.createTransferRequest(assetId, initiatedByUserId, requesterId, previousHolderId);

        // An admin requesting the asset for themselves is still a self-approval risk --
        // skip the auto-approve in that case and leave it at zero signatures, same as
        // an employee's own self-request.
        if (RoleName.ROLE_ADMIN.name().equals(initiatorRole) && !initiatedByUserId.equals(requesterId)) {
            recordAction(approvalId, initiatedByUserId, ApprovalActionType.APPROVED, "Auto-approved on initiation");
        }
        return approvalId;
    }

 
    // checking logic. approval count = 1? if 1 then wont execute the transfer process. 
    // if approval count is 2 it will execute the transfer process
    // if one rejected then it immediately cancels the transfer process.
    @Transactional
    public void recordAction(long approvalId, long actorUserId, ApprovalActionType action, String notes) {

        ApprovalRow current = getApprovalDetail(approvalId);
        if (!ApprovalStatus.PENDING.name().equals(current.status())
                && !ApprovalStatus.PARTIALLY_APPROVED.name().equals(current.status())) {
            throw new BusinessRuleException("This approval has already been closed");
        }

        if (action == ApprovalActionType.APPROVED
                && current.requesterId() != null
                && current.requesterId() == actorUserId) {
            throw new BusinessRuleException("You may not approve your own request");
        }

        approvalDao.recordAction(approvalId, actorUserId, action, notes);
        // checking if employee cancled or admin rejected ?
        if (action == ApprovalActionType.REJECTED || action == ApprovalActionType.CANCELLED) {
            ApprovalStatus closingStatus = (action == ApprovalActionType.REJECTED) ? ApprovalStatus.REJECTED : ApprovalStatus.CANCELLED;
            approvalDao.closeApproval(approvalId, closingStatus);
            log.info("ApprovalService.recordAction -> approval {} closed as {} by actor {}",
                    approvalId, closingStatus, actorUserId);
            return;
        }


        long approvedCount = approvalDao.countApprovedActions(approvalId);
        ApprovalSnapshot snapshot = approvalDao.findApprovalSnapshot(approvalId);

        // check count 1 or 2 
        if (approvedCount < snapshot.requiredApprovalCount()) {
            approvalDao.markPartiallyApproved(approvalId);
            log.info("ApprovalService.recordAction -> approval {} partially approved ({} of {})", approvalId, approvedCount, snapshot.requiredApprovalCount());
            return;
        }

        // else fully approved.
        approvalDao.closeApproval(approvalId, ApprovalStatus.APPROVED);
        assetCustodyDao.transferCustody(snapshot.assetId(), snapshot.requesterId(), approvalId, actorUserId, LocalDateTime.now());
        log.info("ApprovalService.recordAction -> approval {} fully approved by actor {}, custody transferred to user {}", approvalId, actorUserId, snapshot.requesterId());
    }

    public ApprovalRow getApprovalDetail(Long approvalId) {
        return OptionalUtils.orThrowDbFetch(approvalDao.findApprovalDetail(approvalId), "Approval");
    }

    public long countApprovedActions(Long approvalId) {
        return approvalDao.countApprovedActions(approvalId);
    }

    public boolean hasActorRecordedAction(Long approvalId, Long actorUserId) {
        return approvalDao.hasActorRecordedAction(approvalId, actorUserId);
    }


}
