package com.sil.asset_tagging_system.bean;


import com.sil.asset_tagging_system.dao.AuditLogDao;
import com.sil.asset_tagging_system.dao.AuditLogEntry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;

import java.util.List;
import java.util.Map;


@Named
@RequestScoped
@Getter

public class AuditLogBean {

    private final AuditLogDao auditLogDao;
    @Inject
    public AuditLogBean(AuditLogDao auditLogDao)
    {
        this.auditLogDao = auditLogDao;
    }

    private List<AuditLogEntry> entries;
    private Long totalCount;
    private Integer totalPageCount;
    private Integer page;
    private final Integer pageSize = 50;
    private Integer offset;

    @PostConstruct
    public void init()
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        String pageParam = params.get("page");
        if (pageParam == null || pageParam.isBlank() || !pageParam.matches("\\d+")) {
            page = 1;
            offset = 0;
        } else {
            page = Integer.valueOf(pageParam);
            offset = (page - 1) * pageSize;
        }

        entries = auditLogDao.findRecent(pageSize, offset);
        totalCount = auditLogDao.countAll();
        totalPageCount = (int) Math.ceil((double) totalCount / pageSize);
    }
}
