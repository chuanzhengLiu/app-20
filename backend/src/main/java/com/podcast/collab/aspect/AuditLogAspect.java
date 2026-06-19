package com.podcast.collab.aspect;

import com.podcast.collab.annotation.AuditLog;
import com.podcast.collab.security.SecurityUtil;
import com.podcast.collab.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {
    
    private final AuditService auditService;
    private final SecurityUtil securityUtil;
    
    @Around("@annotation(auditLogAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLogAnnotation) throws Throwable {
        AuditLogContext.clear();
        
        Long entityIdFromArgs = null;
        String entityIdParamName = auditLogAnnotation.entityIdParam();
        
        if (!entityIdParamName.isEmpty()) {
            entityIdFromArgs = resolveEntityIdFromArgs(joinPoint, entityIdParamName);
        }
        
        Object result = joinPoint.proceed();
        
        try {
            Long teamId = securityUtil.getCurrentTeamId();
            Long userId = securityUtil.getCurrentUserId();
            var details = AuditLogContext.getDetails();
            
            Long resolvedEntityId = entityIdFromArgs;
            if (resolvedEntityId == null && result != null) {
                resolvedEntityId = extractEntityIdFromResult(result);
            }
            
            auditService.logAction(teamId, userId, auditLogAnnotation.action(),
                    auditLogAnnotation.entityType(), resolvedEntityId, details);
        } catch (Exception e) {
            log.warn("审计日志记录失败: {}", e.getMessage());
        } finally {
            AuditLogContext.clear();
        }
        
        return result;
    }
    
    private Long resolveEntityIdFromArgs(ProceedingJoinPoint joinPoint, String entityIdParamName) {
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        
        Long byName = tryExtractByName(signature, args, entityIdParamName);
        if (byName != null) {
            return byName;
        }
        
        log.debug("AuditLog: 按参数名 '{}' 未找到，fallback到第一个Long类型参数", entityIdParamName);
        return findFirstLongArg(args);
    }
    
    private Long tryExtractByName(MethodSignature signature, Object[] args, String paramName) {
        try {
            String[] parameterNames = signature.getParameterNames();
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    if (paramName.equals(parameterNames[i]) && args[i] != null) {
                        return toLong(args[i]);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("AuditLog: 按名称提取参数失败: {}", e.getMessage());
        }
        return null;
    }
    
    private Long findFirstLongArg(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }
        for (Object arg : args) {
            Long converted = toLong(arg);
            if (converted != null) {
                return converted;
            }
        }
        return null;
    }
    
    private Long toLong(Object arg) {
        if (arg == null) {
            return null;
        }
        if (arg instanceof Long) {
            return (Long) arg;
        }
        if (arg instanceof Number) {
            return ((Number) arg).longValue();
        }
        try {
            return Long.valueOf(arg.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private Long extractEntityIdFromResult(Object result) {
        try {
            Method getId = result.getClass().getMethod("getId");
            Object id = getId.invoke(result);
            if (id instanceof Long) {
                return (Long) id;
            }
            if (id instanceof Number) {
                return ((Number) id).longValue();
            }
        } catch (Exception e) {
            log.debug("AuditLog: 从返回值提取id失败: {}", e.getMessage());
        }
        return null;
    }
}
