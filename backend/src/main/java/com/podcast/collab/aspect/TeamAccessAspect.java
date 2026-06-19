package com.podcast.collab.aspect;

import com.podcast.collab.annotation.TeamAccess;
import com.podcast.collab.dto.ApiResponse;
import com.podcast.collab.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TeamAccessAspect {

    private final SecurityUtil securityUtil;

    @Pointcut("@annotation(teamAccess)")
    public void teamAccessPointcut(TeamAccess teamAccess) {
    }

    @Around("teamAccessPointcut(teamAccess)")
    public Object around(ProceedingJoinPoint joinPoint, TeamAccess teamAccess) throws Throwable {
        Long currentTeamId;
        try {
            currentTeamId = securityUtil.getCurrentTeamId();
        } catch (Exception e) {
            throw e;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] paramValues = joinPoint.getArgs();

        String teamIdParamName = teamAccess.teamIdParam();
        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(teamIdParamName)) {
                Object paramValue = paramValues[i];
                if (paramValue instanceof Long resourceTeamId) {
                    if (!currentTeamId.equals(resourceTeamId)) {
                        String message = "无权访问其他团队数据";
                        if (joinPoint.getSignature().getName().contains("create") ||
                            joinPoint.getSignature().getName().contains("update") ||
                            joinPoint.getSignature().getName().contains("delete")) {
                            message = "无权操作其他团队数据";
                        }
                        return ResponseEntity.badRequest().body(ApiResponse.error(message));
                    }
                }
                break;
            }
        }

        return joinPoint.proceed();
    }
}
