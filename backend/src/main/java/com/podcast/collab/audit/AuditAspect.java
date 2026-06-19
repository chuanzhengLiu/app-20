package com.podcast.collab.audit;

import com.podcast.collab.security.SecurityUtil;
import com.podcast.collab.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 审计日志切面。统一拦截被 @Audited 注解的方法：
 *   - 方法成功返回后写一条审计日志；
 *   - entityId / details 通过 SpEL 表达式从入参与返回值动态求值；
 *   - 方法执行过程中由业务代码 AuditContext.put(...) 暂存的中间值会自动合并到 details；
 *   - 方法抛异常时不写日志，且确保清理 ThreadLocal 上下文。
 *
 * 这样任务/分发等模块的 Service 方法体内不再嵌入 auditService.logAction 调用。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final SecurityUtil securityUtil;
    private final AuditService auditService;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        Object result;
        try {
            result = pjp.proceed();
        } catch (Throwable ex) {
            AuditContext.clear();
            throw ex;
        }

        try {
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            Method method = signature.getMethod();
            Object[] args = pjp.getArgs();

            EvaluationContext context = buildContext(method, args, result);

            Long entityId = evaluateLong(audited.entityId(), context);

            Map<String, Object> details = new HashMap<>();
            String detailsExpr = audited.details();
            if (detailsExpr != null && !detailsExpr.isBlank()) {
                Object val = parser.parseExpression(detailsExpr).getValue(context);
                if (val instanceof Map<?, ?> m) {
                    m.forEach((k, v) -> details.put(String.valueOf(k), v));
                }
            }
            Map<String, Object> threadDetails = AuditContext.drain();
            if (threadDetails != null && !threadDetails.isEmpty()) {
                details.putAll(threadDetails);
            }

            Long teamId = securityUtil.getCurrentTeamId();
            Long userId = securityUtil.getCurrentUserId();

            auditService.logAction(teamId, userId, audited.action(), audited.entityType(),
                    entityId, details.isEmpty() ? null : details);
        } catch (Exception e) {
            log.warn("写入审计日志失败: action={}, entityType={}, error={}",
                    audited.action(), audited.entityType(), e.getMessage());
        } finally {
            AuditContext.clear();
        }

        return result;
    }

    private EvaluationContext buildContext(Method method, Object[] args, Object result) {
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        String[] names = parameterNameDiscoverer.getParameterNames(method);
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                ctx.setVariable(names[i], args[i]);
            }
        }
        ctx.setVariable("args", args);
        ctx.setVariable("result", result);
        return ctx;
    }

    private Long evaluateLong(String expression, EvaluationContext context) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        try {
            Expression exp = parser.parseExpression(expression);
            Object value = exp.getValue(context);
            if (value == null) {
                return null;
            }
            if (value instanceof Long longVal) {
                return longVal;
            }
            if (value instanceof Number num) {
                return num.longValue();
            }
            return Long.valueOf(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
