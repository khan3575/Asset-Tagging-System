package com.sil.asset_tagging_system.util;

public final class PageParams {

    private PageParams() {}

    public static int clamp(Integer page) {
        return (page == null || page < 1) ? 1 : page;
    }

    public static int offset(int page, int pageSize) {
        return (page - 1) * pageSize;
    }

    public static int totalPages(long totalRecords, int pageSize) {
        return (int) Math.ceil((double) totalRecords / pageSize);
    }
}
