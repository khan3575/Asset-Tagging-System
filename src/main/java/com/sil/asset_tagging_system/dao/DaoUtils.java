package com.sil.asset_tagging_system.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.Map;

final class DaoUtils {

    private DaoUtils() {
    }

    static boolean exists(EntityManager entityManager, String sql, Map<String, Object> params) {
        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);
        Number count = (Number) query.getSingleResult();
        return count.longValue() > 0;
    }

    static long getLastInsertId(EntityManager entityManager) {
        Number generatedId = (Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult();
        return generatedId.longValue();
    }
}
