package com.podcast.collab.aspect;

import java.util.Map;

public class AuditLogContext {

    private static final ThreadLocal<Long> ENTITY_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, Object>> DETAILS_HOLDER = new ThreadLocal<>();

    public static void setEntityId(Long entityId) {
        ENTITY_ID_HOLDER.set(entityId);
    }

    public static void setDetails(Map<String, Object> details) {
        DETAILS_HOLDER.set(details);
    }

    public static Long getEntityId() {
        return ENTITY_ID_HOLDER.get();
    }

    public static Map<String, Object> getDetails() {
        return DETAILS_HOLDER.get();
    }

    public static void clear() {
        ENTITY_ID_HOLDER.remove();
        DETAILS_HOLDER.remove();
    }
}
