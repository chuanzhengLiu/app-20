package com.podcast.collab.audit;

import java.util.HashMap;
import java.util.Map;

/**
 * 审计上下文，配合 @Audited 与 AuditAspect 使用。
 *
 * 适用场景：当审计 details 需要记录方法执行过程中产生的中间值（例如更新前的旧状态），
 * 这些值无法直接通过返回值或入参的 SpEL 取到时，业务方法可在执行体内调用
 * AuditContext.put("oldStatus", oldStatus.name()) 暂存，切面在方法成功返回后会
 * 自动合并到审计日志的 details 字段中。
 *
 * 切面会在每次调用退出时清理 ThreadLocal，不会泄漏到下一次请求。
 */
public final class AuditContext {

    private static final ThreadLocal<Map<String, Object>> HOLDER = new ThreadLocal<>();

    private AuditContext() {
    }

    public static void put(String key, Object value) {
        Map<String, Object> map = HOLDER.get();
        if (map == null) {
            map = new HashMap<>();
            HOLDER.set(map);
        }
        map.put(key, value);
    }

    static Map<String, Object> drain() {
        Map<String, Object> map = HOLDER.get();
        HOLDER.remove();
        return map;
    }

    static void clear() {
        HOLDER.remove();
    }
}
