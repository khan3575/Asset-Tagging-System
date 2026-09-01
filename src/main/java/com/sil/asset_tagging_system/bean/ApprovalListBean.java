package com.sil.asset_tagging_system.bean;

import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sil.asset_tagging_system.dto.ApprovalRow;
import com.sil.asset_tagging_system.service.ApprovalService;
import com.sil.asset_tagging_system.util.PageParams;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Named
@RequestScoped
public class ApprovalListBean {
    private final ApprovalService approvalService;

    private List<ApprovalRow> approvalList;

    @Setter
    private Integer currentPage;

    private int offset;
    private int totalPages;
    private int totalRecords;
    private final int pageSize = 10;

    @Inject
    public ApprovalListBean(ApprovalService approvalService)
    {
        this.approvalService = approvalService;
    }

    public void load()
    {
        currentPage = PageParams.clamp(currentPage);
        offset = PageParams.offset(currentPage, pageSize);

        approvalList = approvalService.findOpenApprovals(pageSize, offset);

        totalRecords = (int) approvalService.countOpenApprovals();
        totalPages = PageParams.totalPages(totalRecords, pageSize);

        log.info("ApprovalListBean init -- page {} of {}, {} open approvals total", currentPage, totalPages, totalRecords);
    }
}
