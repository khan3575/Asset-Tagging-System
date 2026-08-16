package com.sil.asset_tagging_system.util;

import java.util.Map;

public final class PageParams {

    public final int page;
    public final int offset;

    private PageParams(int page, int offset) {
        this.page = page;
        this.offset = offset;
    }

    public static PageParams parse(Map<String, String> params, int pageSize) {
        String pageParam = params.get("page");
        if (pageParam == null || pageParam.isBlank() || !pageParam.matches("\\d+")) {
            return new PageParams(1, 0);
        }
        int page = Integer.parseInt(pageParam);
        if (page < 1) {
            return new PageParams(1, 0);
        }
        return new PageParams(page, (page - 1) * pageSize);
    }
}
