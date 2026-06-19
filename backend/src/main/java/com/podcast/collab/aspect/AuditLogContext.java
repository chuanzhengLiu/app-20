package com.podcast.collab.aspect;

import java.util.HashMap;
import java.util.Map;

public class AuditLogContext {
    
    private static final ThreadLocal<Map<String, Object>> DETAILS_HOLDER = new ThreadLocal<>();
    
    public static void setDetails(Map<String, Object> details) {
        DETAILS_HOLDER.set(details);
    }
    
    public static void addDetail(String key, Object value) {
        Map<String, Object> details = DETAILS_HOLDER.get();
        if (details == null) {
            details = new HashMap<>();
            DETAILS_HOLDER.set(details);
        }
        details.put(key, value);
    }
    
    public static Map<String, Object> getDetails() {
        return DETAILS_HOLDER.get();
    }
    
    public static void clear() {
        DETAILS_HOLDER.remove();
    }
}
