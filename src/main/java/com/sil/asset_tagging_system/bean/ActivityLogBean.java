package com.sil.asset_tagging_system.bean;

import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sil.asset_tagging_system.model.ActivityLog;
import com.sil.asset_tagging_system.service.ActivityLogService;
import com.sil.asset_tagging_system.util.PageParams;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Named
@RequestScoped
public class ActivityLogBean {
    private final ActivityLogService activityLogService;
    private List<ActivityLog> entries;
    private long totalCount;
    private int totalPageCount;
    @Setter
    private int page;
    @Setter
    private int offset;
    private final int pageSize = 20;

    @Inject
    public ActivityLogBean(ActivityLogService activityLogService)
    {
        this.activityLogService = activityLogService;
    }


    public void load()
    {
        page = PageParams.clamp(page);
        offset = PageParams.offset(page,pageSize);

        totalCount = activityLogService.countAll();
        totalPageCount = (int) Math.ceil((double) totalCount/ pageSize);

        entries = activityLogService.findRecent(pageSize, offset);
        log.info("ActivityLogBean init -- page {} of {}, {} entries total", page, totalPageCount, totalCount);
    }

    public Long entityIdOf(ActivityLog entry)
    {
        if (entry.getAssetId() != null) {
            return entry.getAssetId();
        }
        if (entry.getApprovalId() != null) {
            return entry.getApprovalId();
        }
        if (entry.getSubjectUserId() != null) {
            return entry.getSubjectUserId();
        }
        return null;
    }
   
}
