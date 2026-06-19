package com.podcast.collab.aspect;

import com.podcast.collab.annotation.AuditLog;
import com.podcast.collab.audit.AuditContext;
import com.podcast.collab.dto.ApiResponse;
import com.podcast.collab.security.SecurityUtil;
import com.podcast.collab.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {
    
    private final AuditService auditService;
    private final SecurityUtil securityUtil;
    
    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        Long oldEntityId = extractEntityIdFromParams(joinPoint, auditLog);
        
        Map<String, Object> requestDetails = extractDetailFields(joinPoint, auditLog);
        
        Object result = joinPoint.proceed();
        
        try {
            Long teamId = securityUtil.getCurrentTeamId();
            Long userId = securityUtil.getCurrentUserId();
            
            Long entityId = oldEntityId;
            if (entityId == null && auditLog.extractEntityIdFromResult()) {
                entityId = extractEntityIdFromResult(result);
            }
            
            Map<String, Object> details = new HashMap<>();
            
            if (requestDetails != null) {
                details.putAll(requestDetails);
            }
            
            Map<String, Object> contextDetails = AuditContext.get();
            if (contextDetails != null) {
                details.putAll(contextDetails);
            }
            
            if (auditLog.logDetails()) {
                Map<String, Object> resultDetails = extractResultDetails(result);
                if (resultDetails != null) {
                    details.putAll(resultDetails);
                }
            }
            
            auditService.logAction(teamId, userId, auditLog.action(), 
                    auditLog.entityType(), entityId, details.isEmpty() ? null : details);
        } catch (Exception e) {
            log.warn("记录审计日志失败: {}", e.getMessage());
        } finally {
            AuditContext.clear();
        }
        
        return result;
    }
    
    private Long extractEntityIdFromParams(ProceedingJoinPoint joinPoint, AuditLog auditLog) {
        if (auditLog.entityIdParam().isEmpty()) {
            return null;
        }
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        
        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(auditLog.entityIdParam())) {
                if (args[i] instanceof Long) {
                    return (Long) args[i];
                }
                if (args[i] instanceof Number) {
                    return ((Number) args[i]).longValue();
                }
                break;
            }
        }
        
        return null;
    }
    
    private Map<String, Object> extractDetailFields(ProceedingJoinPoint joinPoint, AuditLog auditLog) {
        if (auditLog.detailFields().length == 0) {
            return null;
        }
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        
        Object requestBody = null;
        for (int i = 0; i < paramNames.length; i++) {
            if (args[i] != null && isRequestDto(args[i])) {
                requestBody = args[i];
                break;
            }
        }
        
        if (requestBody == null) {
            return null;
        }
        
        Map<String, Object> details = new HashMap<>();
        Class<?> clazz = requestBody.getClass();
        
        for (String fieldMapping : auditLog.detailFields()) {
            String fieldName;
            String keyName;
            
            if (fieldMapping.contains(":")) {
                String[] parts = fieldMapping.split(":", 2);
                fieldName = parts[0];
                keyName = parts[1];
            } else {
                fieldName = fieldMapping;
                keyName = fieldMapping;
            }
            
            try {
                String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                Method getter = clazz.getMethod(getterName);
                Object value = getter.invoke(requestBody);
                if (value != null) {
                    if (value instanceof Enum<?>) {
                        details.put(keyName, ((Enum<?>) value).name());
                    } else {
                        details.put(keyName, value);
                    }
                }
            } catch (Exception e) {
                log.debug("无法提取审计字段 {}: {}", fieldName, e.getMessage());
            }
        }
        
        return details;
    }
    
    private boolean isRequestDto(Object obj) {
        String packageName = obj.getClass().getPackageName();
        return packageName.contains("dto.request");
    }
    
    private Long extractEntityIdFromResult(Object result) {
        if (result instanceof ResponseEntity) {
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
            Object body = responseEntity.getBody();
            if (body instanceof ApiResponse) {
                ApiResponse<?> apiResponse = (ApiResponse<?>) body;
                Object data = apiResponse.getData();
                if (data != null) {
                    try {
                        Method getIdMethod = data.getClass().getMethod("getId");
                        Object idValue = getIdMethod.invoke(data);
                        if (idValue instanceof Long) {
                            return (Long) idValue;
                        }
                        if (idValue instanceof Number) {
                            return ((Number) idValue).longValue();
                        }
                    } catch (Exception e) {
                        log.debug("无法从返回结果提取实体ID: {}", e.getMessage());
                    }
                }
            }
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractResultDetails(Object result) {
        if (result instanceof ResponseEntity) {
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
            Object body = responseEntity.getBody();
            if (body instanceof ApiResponse) {
                ApiResponse<?> apiResponse = (ApiResponse<?>) body;
                Map<String, Object> details = new HashMap<>();
                details.put("success", apiResponse.isSuccess());
                if (apiResponse.getMessage() != null) {
                    details.put("message", apiResponse.getMessage());
                }
                return details;
            }
        }
        return null;
    }
}
