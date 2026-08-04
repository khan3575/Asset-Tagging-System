package com.sil.asset_tagging_system.bean;


import com.sil.asset_tagging_system.dao.AuditLogDao;
import com.sil.asset_tagging_system.dao.AuditLogEntry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;

import java.util.List;


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

    @PostConstruct
    public void init()
    {
        entries = auditLogDao.findRecent(50);
    }
}
