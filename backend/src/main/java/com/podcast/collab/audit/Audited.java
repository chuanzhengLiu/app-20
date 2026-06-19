package com.podcast.collab.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志声明式注解。被标注的 Service 方法在成功返回后由 AuditAspect 自动写审计日志，
 * 业务代码不再需要显式调用 AuditService。
 *
 * SpEL 上下文中可用的根对象：
 *   - 入参：按形参名引用，如 #id、#request、#teamId
 *   - 返回值：#result（仅在 entityId / details 解析阶段可用，方法异常时不解析）
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audited {

    /**
     * 操作动作名，例如 CREATE_TASK、UPDATE_TASK_STATUS。
     */
    String action();

    /**
     * 实体类型，例如 TASK、DISTRIBUTION_PLATFORM。
     */
    String entityType();

    /**
     * 实体 ID 的 SpEL 表达式，默认从返回值中取 id 字段。
     * 写法示例：
     *   - "#id"                 取入参 id
     *   - "#result.id"          取返回 DTO 的 id（默认）
     */
    String entityId() default "#result.id";

    /**
     * 审计详情 Map 的 SpEL 表达式，留空表示不记录详情。
     * 写法示例："T(java.util.Map).of('oldStatus', #oldStatus.name(), 'newStatus', #request.status.name())"
     */
    String details() default "";
}
