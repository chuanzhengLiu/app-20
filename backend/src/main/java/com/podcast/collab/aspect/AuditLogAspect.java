package com.podcast.collab.aspect;

import com.podcast.collab.annotation.AuditLog;
import com.podcast.collab.security.SecurityUtil;
import com.podcast.collab.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditService auditService;
    private final SecurityUtil securityUtil;

    @Pointcut("@annotation(auditLog)")
    public void auditLogPointcut(AuditLog auditLog) {
    }

    @Around("auditLogPointcut(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        AuditLogContext.clear();
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable t) {
            AuditLogContext.clear();
            throw t;
        }

        try {
            Long teamId = securityUtil.getCurrentTeamId();
            Long userId = securityUtil.getCurrentUserId();

            Long entityId = AuditLogContext.getEntityId();
            Map<String, Object> details = AuditLogContext.getDetails();

            if (entityId == null && result != null) {
                if (result instanceof com.podcast.collab.dto.TaskDTO dto) {
                    entityId = dto.getId();
                } else if (result instanceof com.podcast.collab.dto.DistributionDTO dto) {
                    entityId = dto.getId();
                }
            }

            auditService.logAction(teamId, userId, auditLog.action(), auditLog.entityType(), entityId, details);
        } catch (Exception e) {
            log.warn("记录审计日志失败: {}", e.getMessage());
        } finally {
            AuditLogContext.clear();
        }

        return result;
    }
}
