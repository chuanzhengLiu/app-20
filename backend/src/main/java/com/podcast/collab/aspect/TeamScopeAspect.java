package com.podcast.collab.aspect;

import com.podcast.collab.annotation.TeamScope;
import com.podcast.collab.exception.TeamAccessDeniedException;
import com.podcast.collab.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TeamScopeAspect {
    
    private final SecurityUtil securityUtil;
    
    @Around("@annotation(teamScope)")
    public Object around(ProceedingJoinPoint joinPoint, TeamScope teamScope) throws Throwable {
        Long currentTeamId = securityUtil.getCurrentTeamId();
        
        if (teamScope.validateTeamIdParam()) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            
            String teamIdParamName = teamScope.teamIdParam();
            for (int i = 0; i < paramNames.length; i++) {
                if (paramNames[i].equals(teamIdParamName)) {
                    Object teamIdArg = args[i];
                    if (teamIdArg instanceof Long paramTeamId) {
                        if (!currentTeamId.equals(paramTeamId)) {
                            log.warn("团队权限校验失败: 当前用户团队ID={}, 请求团队ID={}", currentTeamId, paramTeamId);
                            throw new TeamAccessDeniedException(teamScope.message());
                        }
                    }
                    break;
                }
            }
        }
        
        return joinPoint.proceed();
    }
}
