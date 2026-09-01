package com.sil.asset_tagging_system.dto;

import com.sil.asset_tagging_system.security.SecurityUtil;
import com.sil.asset_tagging_system.util.WebUtil;

public record Actor(Long userId, String role, String ipAddress) {
    public static Actor current() {
        return new Actor(SecurityUtil.currentUserId(), SecurityUtil.primaryRole(), WebUtil.getRemoteAddress());
    }
}
