package com.podcast.collab.security;

import com.podcast.collab.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeamSecurityChecker {

    private final SecurityUtil securityUtil;

    public Long getCurrentTeamId() {
        return securityUtil.getCurrentTeamId();
    }

    public User getCurrentUser() {
        return securityUtil.getCurrentUser();
    }

    public Long getCurrentUserId() {
        return securityUtil.getCurrentUserId();
    }

    public void checkTeamAccess(Long resourceTeamId) {
        Long currentTeamId = getCurrentTeamId();
        if (!currentTeamId.equals(resourceTeamId)) {
            throw new IllegalArgumentException("无权访问其他团队数据");
        }
    }

    public void checkTeamAccess() {
        getCurrentTeamId();
    }
}
