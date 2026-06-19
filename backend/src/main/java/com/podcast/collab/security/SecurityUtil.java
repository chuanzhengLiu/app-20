package com.podcast.collab.security;

import com.podcast.collab.entity.User;
import com.podcast.collab.exception.CrossTeamAccessException;
import com.podcast.collab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtil {
    
    private final UserRepository userRepository;
    
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("用户未登录");
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
            return user.getId();
        }
        
        throw new IllegalArgumentException("无法获取当前用户信息");
    }
    
    public User getCurrentUser() {
        Long userId = getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }
    
    public Long getCurrentTeamId() {
        User user = getCurrentUser();
        if (user.getTeam() == null) {
            throw new IllegalArgumentException("用户不属于任何团队");
        }
        return user.getTeam().getId();
    }
    
    /**
     * 读接口的团队权限入口：返回当前用户的 teamId；
     * 若传入了 requestedTeamId 且与当前用户的团队不一致，抛 CrossTeamAccessException，
     * 由 GlobalExceptionHandler 统一转换为 HTTP 400 + ApiResponse.error("无权访问其他团队数据")，
     * 响应体不携带 code 字段，与改造前完全一致。
     */
    public Long requireCurrentTeamIdForRead(Long requestedTeamId) {
        Long currentTeamId = getCurrentTeamId();
        if (requestedTeamId != null && !currentTeamId.equals(requestedTeamId)) {
            throw new CrossTeamAccessException("无权访问其他团队数据");
        }
        return currentTeamId;
    }

    /**
     * 写接口的团队权限入口：行为同读入口，但跨团队场景的错误文案为「无权操作其他团队数据」。
     */
    public Long requireCurrentTeamIdForWrite(Long requestedTeamId) {
        Long currentTeamId = getCurrentTeamId();
        if (requestedTeamId != null && !currentTeamId.equals(requestedTeamId)) {
            throw new CrossTeamAccessException("无权操作其他团队数据");
        }
        return currentTeamId;
    }
    
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
    
    public boolean isAdmin() {
        return hasRole("ADMIN") || hasRole("PRODUCER");
    }
}
