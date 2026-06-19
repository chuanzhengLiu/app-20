package com.podcast.collab.audit;

import java.util.HashMap;
import java.util.Map;

public final class AuditContext {
    
    private static final ThreadLocal<Map<String, Object>> DETAILS_HOLDER = new ThreadLocal<>();
    
    private AuditContext() {
    }
    
    public static void put(String key, Object value) {
        Map<String, Object> details = DETAILS_HOLDER.get();
        if (details == null) {
            details = new HashMap<>();
            DETAILS_HOLDER.set(details);
        }
        details.put(key, value);
    }
    
    public static void putAll(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        Map<String, Object> details = DETAILS_HOLDER.get();
        if (details == null) {
            details = new HashMap<>();
            DETAILS_HOLDER.set(details);
        }
        details.putAll(values);
    }
    
    public static Map<String, Object> get() {
        return DETAILS_HOLDER.get();
    }
    
    public static void clear() {
        DETAILS_HOLDER.remove();
    }
}
