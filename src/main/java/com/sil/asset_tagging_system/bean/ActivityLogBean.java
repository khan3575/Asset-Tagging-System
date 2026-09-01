package com.sil.asset_tagging_system.bean;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sil.asset_tagging_system.dto.ActivityLogRow;
import com.sil.asset_tagging_system.model.enums.ActivityAction;
import com.sil.asset_tagging_system.model.enums.ActivityEntityType;
import com.sil.asset_tagging_system.model.enums.ActivityOutcome;
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

    private List<ActivityLogRow> entries;

    @Setter
    private Integer currentPage;

    private int offset;
    private int totalPages;
    private long totalRecords;
    private final int pageSize = 20;

    @Setter
    private String entityType;
    @Setter
    private String action;
    @Setter
    private String outcome;
    @Setter
    private String search;
    @Setter
    private LocalDate from;
    @Setter
    private LocalDate to;

    @Inject
    public ActivityLogBean(ActivityLogService activityLogService)
    {
        this.activityLogService = activityLogService;
    }


    public void load()
    {
        currentPage = PageParams.clamp(currentPage);
        offset = PageParams.offset(currentPage, pageSize);

        totalRecords = activityLogService.countPage(entityType, action, outcome, search, from, to);
        totalPages = PageParams.totalPages(totalRecords, pageSize);

        entries = activityLogService.findPage(entityType, action, outcome, search, from, to, pageSize, offset);
        log.info("ActivityLogBean init -- page {} of {}, {} entries total", currentPage, totalPages, totalRecords);
    }

    public String applyFilters()
    {
        currentPage = 1;
        return "/activity/log?faces-redirect=true&includeViewParams=true";
    }

    public String clearFilters()
    {
        return "/activity/log?faces-redirect=true";
    }

    public Map<String, String> getExtraParams()
    {
        Map<String, String> params = new LinkedHashMap<>();
        if (entityType != null && !entityType.isBlank()) params.put("entityType", entityType);
        if (action != null && !action.isBlank())         params.put("action", action);
        if (outcome != null && !outcome.isBlank())       params.put("outcome", outcome);
        if (search != null && !search.isBlank())         params.put("search", search);
        if (from != null)                                params.put("from", from.toString());
        if (to != null)                                  params.put("to", to.toString());
        return params;
    }

    public ActivityEntityType[] getEntityTypeOptions()
    {
        return ActivityEntityType.values();
    }

    public ActivityAction[] getActionOptions()
    {
        return ActivityAction.values();
    }

    public ActivityOutcome[] getOutcomeOptions()
    {
        return ActivityOutcome.values();
    }
}
