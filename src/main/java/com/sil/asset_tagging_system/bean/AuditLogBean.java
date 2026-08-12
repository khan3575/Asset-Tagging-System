package com.sil.asset_tagging_system.bean;


import com.sil.asset_tagging_system.dao.AuditLogDao;
import com.sil.asset_tagging_system.dao.AuditLogEntry;
import com.sil.asset_tagging_system.util.FacesUtil;
import com.sil.asset_tagging_system.util.PageParams;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
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
        Map<String, String> params = FacesUtil.getRequestParams();

        PageParams pageParams = PageParams.parse(params, pageSize);
        page = pageParams.page;
        offset = pageParams.offset;

        entries = auditLogDao.findRecent(pageSize, offset);
        totalCount = auditLogDao.countAll();
        totalPageCount = (int) Math.ceil((double) totalCount / pageSize);
    }
}
