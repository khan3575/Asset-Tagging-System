package com.sil.asset_tagging_system.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sil.asset_tagging_system.dao.ActivityLogDao;
import com.sil.asset_tagging_system.model.ActivityLog;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ActivityLogService {
    private final ActivityLogDao activityLogDao;

    public List<ActivityLog> findRecent(int limit, int offset) {
        return activityLogDao.findRecent(limit, offset);
    }

    public long countAll() {
        return activityLogDao.countAll();
    }
}
