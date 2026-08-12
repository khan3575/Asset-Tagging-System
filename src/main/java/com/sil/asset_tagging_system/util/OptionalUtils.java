package com.sil.asset_tagging_system.util;

import com.sil.asset_tagging_system.exception.DbFetchException;

import java.util.Optional;

public final class OptionalUtils {

    private OptionalUtils() {
    }

    public static <T> T orThrowDbFetch(Optional<T> optional, String entityName) {
        return optional.orElseThrow(() -> new DbFetchException(entityName + " fetching error from DB"));
    }
}
