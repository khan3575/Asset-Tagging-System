package com.sil.asset_tagging_system.bean;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sil.asset_tagging_system.dto.ApprovalRow;
import com.sil.asset_tagging_system.exception.BusinessRuleException;
import com.sil.asset_tagging_system.model.enums.ApprovalActionType;
import com.sil.asset_tagging_system.security.SecurityUtil;
import com.sil.asset_tagging_system.service.ApprovalService;

import lombok.Getter;
import lombok.Setter;

@Getter
@Named
@ViewScoped
public class ApprovalDetailBean {
    private final ApprovalService approvalService;

    @Setter
    private Long id;

    private ApprovalRow detail;
    private long approvedCount;
    private boolean alreadyActed;

    @Setter
    private String notes;

    @Inject
    public ApprovalDetailBean(ApprovalService approvalService)
    {
        this.approvalService = approvalService;
    }

    public void load()
    {
        detail = approvalService.getApprovalDetail(id);
        approvedCount = approvalService.countApprovedActions(id);
        alreadyActed = approvalService.hasActorRecordedAction(id, SecurityUtil.currentUserId());
    }

    public String approve() { return act(ApprovalActionType.APPROVED); }
    public String reject() { return act(ApprovalActionType.REJECTED); }

    private String act(ApprovalActionType action) {
        Long actorUserId = SecurityUtil.currentUserId();
        try {
            approvalService.recordAction(id, actorUserId, action, notes);
        } catch (BusinessRuleException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
            return null;
        }
        return "/approval/detail?id="+id+"&faces-redirect=true&includeViewParams=true";
    }

}
