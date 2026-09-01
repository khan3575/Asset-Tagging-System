package com.sil.asset_tagging_system.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sil.asset_tagging_system.dao.ActivityLogDao;
import com.sil.asset_tagging_system.dto.ActivityLogRow;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ActivityLogService {
    private final ActivityLogDao activityLogDao;

    public List<ActivityLogRow> findPage(String entityType, String action, String outcome, String search,
                                         LocalDate from, LocalDate to, int limit, int offset) {
        return activityLogDao.findPage(entityType, action, outcome, search, from, to, limit, offset);
    }

    public long countPage(String entityType, String action, String outcome, String search,
                          LocalDate from, LocalDate to) {
        return activityLogDao.countPage(entityType, action, outcome, search, from, to);
    }
}
