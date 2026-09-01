package com.sil.asset_tagging_system.bean;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sil.asset_tagging_system.dto.ActivityLogRow;
import com.sil.asset_tagging_system.dto.ApprovalRow;
import com.sil.asset_tagging_system.dto.AssetRow;
import com.sil.asset_tagging_system.model.enums.AssetCondition;
import com.sil.asset_tagging_system.model.enums.RoleName;
import com.sil.asset_tagging_system.security.SecurityUtil;
import com.sil.asset_tagging_system.service.ActivityLogService;
import com.sil.asset_tagging_system.service.ApprovalService;
import com.sil.asset_tagging_system.service.AssetService;
import com.sil.asset_tagging_system.service.UserService;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Named
@RequestScoped
public class DashboardBean {

    private static final int RECENT_ACTIVITY_ROWS = 5;

    private final transient AssetService assetService;
    private final transient UserService userService;
    private final transient ApprovalService approvalService;
    private final transient ActivityLogService activityLogService;

    private boolean admin;

    private long totalAssets;
    private long openApprovals;
    private long totalUsers;
    private Map<AssetCondition, Long> assetsByCondition;
    private List<ActivityLogRow> recentActivity;

    private List<AssetRow> myAssets;
    private List<ApprovalRow> myRequests;

    @Inject
    public DashboardBean(AssetService assetService, UserService userService,
                         ApprovalService approvalService, ActivityLogService activityLogService)
    {
        this.assetService = assetService;
        this.userService = userService;
        this.approvalService = approvalService;
        this.activityLogService = activityLogService;
    }

    public void load()
    {
        admin = RoleName.ROLE_ADMIN.name().equals(SecurityUtil.primaryRole());
        Long userId = SecurityUtil.currentUserId();

        if (admin) {
            totalAssets = assetService.countAssets("");
            openApprovals = approvalService.countOpenApprovals();
            totalUsers = userService.countUsers(null, null, null, null);
            assetsByCondition = assetService.countByCondition();
            recentActivity = activityLogService.findPage(null, null, null, null, null, null,
                    RECENT_ACTIVITY_ROWS, 0);
        } else {
            myAssets = assetService.findAssetsHeldBy(userId);
            myRequests = approvalService.findOpenRequestsFor(userId);
        }

        log.info("DashboardBean init -- admin={} user={}", admin, userId);
    }

    public long getInServiceCount()
    {
        return assetsByCondition == null ? 0L : assetsByCondition.getOrDefault(AssetCondition.IN_SERVICE, 0L);
    }
}
