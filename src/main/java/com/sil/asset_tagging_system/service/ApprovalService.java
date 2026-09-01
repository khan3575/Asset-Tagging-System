package com.sil.asset_tagging_system.service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sil.asset_tagging_system.dao.ApprovalDao;
import com.sil.asset_tagging_system.dao.ApprovalDao.ApprovalSnapshot;
import com.sil.asset_tagging_system.dao.AssetCustodyDao;
import com.sil.asset_tagging_system.dto.Actor;
import com.sil.asset_tagging_system.dto.ApprovalRow;
import com.sil.asset_tagging_system.exception.BusinessRuleException;
import com.sil.asset_tagging_system.model.enums.ActivityAction;
import com.sil.asset_tagging_system.model.enums.ActivityEntityType;
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
    private final AuditTrail auditTrail;

    public boolean hasOpenTransferRequest(Long assetId) {
        return approvalDao.existsOpenTransferRequest(assetId);
    }
    public Optional<Long> findActiveCustodianId(Long assetId) {
        return assetCustodyDao.findActiveCustodianId(assetId);
    }

    @Transactional
    public Long initiateTransfer(Long assetId, Actor initiator, Long requesterId, Long previousHolderId) {
        if (approvalDao.existsOpenTransferRequest(assetId)) {
            auditTrail.record(ActivityAction.REQUEST_SUBMITTED, ActivityEntityType.APPROVAL)
                    .by(initiator)
                    .asset(assetId)
                    .holder(null, requesterId)
                    .refused("A transfer is already pending for this asset")
                    .summary("Transfer request refused -- one is already pending for asset " + assetId)
                    .save();
            throw new BusinessRuleException("A transfer is already pending for this asset");
        }
        Long approvalId = approvalDao.createTransferRequest(assetId, initiator.userId(), requesterId, previousHolderId);

        auditTrail.record(ActivityAction.REQUEST_SUBMITTED, ActivityEntityType.APPROVAL)
                .by(initiator)
                .asset(assetId)
                .approval(approvalId)
                .holder(previousHolderId, requesterId)
                .summary("Transfer requested for asset " + assetId + " to user " + requesterId)
                .save();

        if (RoleName.ROLE_ADMIN.name().equals(initiator.role()) && !Objects.equals(initiator.userId(), requesterId)) {
            recordAction(approvalId, initiator, ApprovalActionType.APPROVED, "Auto-approved on initiation");
        }
        return approvalId;
    }

    @Transactional
    public void recordAction(long approvalId, Actor actor, ApprovalActionType action, String notes) {

        ApprovalRow current = getApprovalDetail(approvalId);
        if (!ApprovalStatus.PENDING.name().equals(current.status())
                && !ApprovalStatus.PARTIALLY_APPROVED.name().equals(current.status())) {
            refuse(actor, action, approvalId, current, "This approval has already been closed");
            throw new BusinessRuleException("This approval has already been closed");
        }

        if (action == ApprovalActionType.APPROVED
                && current.requesterId() != null
                && current.requesterId().equals(actor.userId())) {
            refuse(actor, action, approvalId, current, "You may not approve your own request");
            throw new BusinessRuleException("You may not approve your own request");
        }

        approvalDao.recordAction(approvalId, actor.userId(), action, notes);
        if (action == ApprovalActionType.REJECTED || action == ApprovalActionType.CANCELLED) {
            ApprovalStatus closingStatus = (action == ApprovalActionType.REJECTED) ? ApprovalStatus.REJECTED : ApprovalStatus.CANCELLED;
            approvalDao.closeApproval(approvalId, closingStatus);

            auditTrail.record(action == ApprovalActionType.REJECTED
                                    ? ActivityAction.REQUEST_REJECTED
                                    : ActivityAction.REQUEST_CANCELLED,
                              ActivityEntityType.APPROVAL)
                    .by(actor)
                    .approval(approvalId)
                    .asset(assetIdOf(approvalId))
                    .summary("Request " + approvalId + " closed as " + closingStatus + " for asset " + current.assetTag())
                    .details(notesJson(notes))
                    .save();

            log.info("ApprovalService.recordAction -> approval {} closed as {} by actor {}",
                    approvalId, closingStatus, actor.userId());
            return;
        }

        long approvedCount = approvalDao.countApprovedActions(approvalId);
        ApprovalSnapshot snapshot = approvalDao.findApprovalSnapshot(approvalId);

        if (approvedCount < snapshot.requiredApprovalCount()) {
            approvalDao.markPartiallyApproved(approvalId);

            auditTrail.record(ActivityAction.REQUEST_APPROVED, ActivityEntityType.APPROVAL)
                    .by(actor)
                    .approval(approvalId)
                    .asset(snapshot.assetId())
                    .summary("Signature " + approvedCount + " of " + snapshot.requiredApprovalCount()
                            + " on request " + approvalId + " for asset " + current.assetTag())
                    .details(notesJson(notes))
                    .save();

            log.info("ApprovalService.recordAction -> approval {} partially approved ({} of {})", approvalId, approvedCount, snapshot.requiredApprovalCount());
            return;
        }

        approvalDao.closeApproval(approvalId, ApprovalStatus.APPROVED);

        auditTrail.record(ActivityAction.REQUEST_APPROVED, ActivityEntityType.APPROVAL)
                .by(actor)
                .approval(approvalId)
                .asset(snapshot.assetId())
                .holder(snapshot.previousHolderId(), snapshot.requesterId())
                .summary("Final signature on request " + approvalId + " for asset " + current.assetTag()
                        + " (" + approvedCount + " of " + snapshot.requiredApprovalCount() + ")")
                .details(notesJson(notes))
                .save();

        assetCustodyDao.transferCustody(snapshot.assetId(), snapshot.requesterId(), approvalId, actor.userId(), LocalDateTime.now());

        boolean isReturn = snapshot.requesterId() == null;
        auditTrail.record(isReturn ? ActivityAction.CUSTODY_RELEASED : ActivityAction.CUSTODY_TRANSFERRED,
                          ActivityEntityType.ASSET)
                .by(actor)
                .sequence(2)
                .approval(approvalId)
                .asset(snapshot.assetId())
                .holder(snapshot.previousHolderId(), snapshot.requesterId())
                .summary(isReturn
                        ? "Custody of asset " + current.assetTag() + " released on approved return"
                        : "Custody of asset " + current.assetTag() + " transferred to user " + snapshot.requesterId())
                .save();

        if (!isReturn) {
            log.info("ApprovalService.recordAction -> approval {} fully approved by actor {}, custody transferred to user {}", approvalId, actor.userId(), snapshot.requesterId());
        } else {
            log.info("ApprovalService.recordAction -> approval {} fully approved by actor {}, custody released (return)", approvalId, actor.userId());
        }
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

    public java.util.List<ApprovalRow> findOpenApprovals(int limit, int offset)
    {
        return approvalDao.findOpenApprovals(limit , offset);
    }

    public long countOpenApprovals()
    {
       return approvalDao.countOpenApprovals();
    }

    private void refuse(Actor actor, ApprovalActionType action, Long approvalId, ApprovalRow current, String reason) {
        auditTrail.record(action == ApprovalActionType.REJECTED ? ActivityAction.REQUEST_REJECTED : ActivityAction.REQUEST_APPROVED, ActivityEntityType.APPROVAL)
                .by(actor)
                .approval(approvalId)
                .refused(reason)
                .summary("Refused " + action + " on request " + approvalId + " for asset " + current.assetTag())
                .save();
    }
    private Long assetIdOf(long approvalId) {
        return approvalDao.findApprovalSnapshot(approvalId).assetId();
    }
    private String notesJson(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        return "{\"notes\":\"" + notes.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
    }
}
