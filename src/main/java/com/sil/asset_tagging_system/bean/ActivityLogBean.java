package com.sil.asset_tagging_system.bean;

import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sil.asset_tagging_system.dao.ActivityLogDao;
import com.sil.asset_tagging_system.model.ActivityLog;
import com.sil.asset_tagging_system.util.FacesUtil;
import com.sil.asset_tagging_system.util.PageParams;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@Named
@RequestScoped
public class ActivityLogBean {
    private final ActivityLogDao activityLogDao;
    private List<ActivityLog> entries;
    private long totalCount;
    private int totalPageCount;
    private int page;
    private int offset;
    private final int pageSize = 20;

    @Inject
    ActivityLogBean(ActivityLogDao activityLogDao)
    {
        this.activityLogDao = activityLogDao;
    }

    
    @PostConstruct
    public void init()
    {
        Map<String, String> params = FacesUtil.getRequestParams();
        PageParams pageParam = PageParams.parse(params,pageSize);
        
        page = pageParam.page;
        offset = pageParam.offset;

        long totalCount = activityLogDao.countAll();
        totalPageCount = (int) Math.ceil((double) totalCount/ pageSize);

        entries = activityLogDao.findRecent(pageSize, offset);
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
