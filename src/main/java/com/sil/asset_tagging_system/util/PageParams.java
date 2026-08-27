package com.sil.asset_tagging_system.util;

public final class PageParams {

    private PageParams() {}

    public static int clamp(Integer page) {
        return (page == null || page < 1) ? 1 : page;
    }

    public static int offset(int page, int pageSize) {
        return (page - 1) * pageSize;
    }
}
