package com.podcast.collab.service;

import com.podcast.collab.annotation.AuditLog;
import com.podcast.collab.aspect.AuditLogContext;
import com.podcast.collab.dto.DistributionDTO;
import com.podcast.collab.dto.request.CreateDistributionRecordRequest;
import com.podcast.collab.dto.request.CreatePlatformRequest;
import com.podcast.collab.dto.request.UpdateDistributionStatusRequest;
import com.podcast.collab.dto.request.UpdatePlatformRequest;
import com.podcast.collab.entity.DistributionPlatform;
import com.podcast.collab.entity.DistributionRecord;
import com.podcast.collab.entity.Episode;
import com.podcast.collab.entity.Team;
import com.podcast.collab.repository.DistributionPlatformRepository;
import com.podcast.collab.repository.DistributionRecordRepository;
import com.podcast.collab.repository.EpisodeRepository;
import com.podcast.collab.repository.TeamRepository;
import com.podcast.collab.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DistributionService {
    
    private final DistributionPlatformRepository platformRepository;
    private final DistributionRecordRepository recordRepository;
    private final EpisodeRepository episodeRepository;
    private final TeamRepository teamRepository;
    private final SecurityUtil securityUtil;
    
    private void validateTeamAccessForRead(Long teamId) {
        Long currentTeamId = securityUtil.getCurrentTeamId();
        if (!currentTeamId.equals(teamId)) {
            throw new IllegalArgumentException("无权访问其他团队数据");
        }
    }
    
    private void validateTeamAccessForWrite(Long teamId) {
        Long currentTeamId = securityUtil.getCurrentTeamId();
        if (!currentTeamId.equals(teamId)) {
            throw new IllegalArgumentException("无权操作其他团队数据");
        }
    }
    
    @Transactional(readOnly = true)
    public List<DistributionDTO> getPlatforms(Long teamId) {
        validateTeamAccessForRead(teamId);
        List<DistributionPlatform> platforms = platformRepository.findByTeamId(teamId);
        return platforms.stream()
                .map(DistributionDTO::fromPlatform)
                .collect(Collectors.toList());
    }
    
    @Transactional
    @AuditLog(action = "CREATE_DISTRIBUTION_PLATFORM", entityType = "DISTRIBUTION_PLATFORM")
    public DistributionDTO createPlatform(Long teamId, CreatePlatformRequest request) {
        validateTeamAccessForWrite(teamId);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("团队不存在"));
        
        DistributionPlatform platform = DistributionPlatform.builder()
                .team(team)
                .name(request.getName())
                .type(request.getType())
                .config(request.getConfig() != null ? request.getConfig() : Map.of())
                .build();
        
        platform = platformRepository.save(platform);
        
        AuditLogContext.addDetail("name", platform.getName());
        AuditLogContext.addDetail("type", platform.getType().name());
        
        return DistributionDTO.fromPlatform(platform);
    }
    
    @Transactional
    @AuditLog(action = "UPDATE_DISTRIBUTION_PLATFORM", entityType = "DISTRIBUTION_PLATFORM", entityIdParam = "id")
    public DistributionDTO updatePlatform(Long id, Long teamId, UpdatePlatformRequest request) {
        validateTeamAccessForWrite(teamId);
        DistributionPlatform platform = platformRepository.findByIdAndTeamId(id, teamId)
                .orElseThrow(() -> new IllegalArgumentException("平台不存在"));
        
        if (request.getName() != null) {
            platform.setName(request.getName());
        }
        if (request.getConfig() != null) {
            platform.setConfig(request.getConfig());
        }
        
        platform = platformRepository.save(platform);
        return DistributionDTO.fromPlatform(platform);
    }
    
    @Transactional
    @AuditLog(action = "DELETE_DISTRIBUTION_PLATFORM", entityType = "DISTRIBUTION_PLATFORM", entityIdParam = "id")
    public void deletePlatform(Long id, Long teamId) {
        validateTeamAccessForWrite(teamId);
        DistributionPlatform platform = platformRepository.findByIdAndTeamId(id, teamId)
                .orElseThrow(() -> new IllegalArgumentException("平台不存在"));
        
        platformRepository.delete(platform);
    }
    
    @Transactional(readOnly = true)
    public List<DistributionDTO> getDistributionRecords(Long teamId, Long episodeId, Long platformId, DistributionRecord.Status status) {
        validateTeamAccessForRead(teamId);
        List<DistributionRecord> records;
        
        if (episodeId != null) {
            records = recordRepository.findByEpisodeIdAndTeamId(episodeId, teamId);
        } else if (platformId != null) {
            records = recordRepository.findByPlatformIdAndTeamId(platformId, teamId);
        } else if (status != null) {
            records = recordRepository.findByStatusAndTeamId(status, teamId);
        } else {
            records = recordRepository.findByTeamId(teamId);
        }
        
        return records.stream()
                .map(DistributionDTO::fromRecord)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public DistributionDTO getDistributionRecord(Long id, Long teamId) {
        validateTeamAccessForRead(teamId);
        DistributionRecord record = recordRepository.findByIdAndTeamId(id, teamId)
                .orElseThrow(() -> new IllegalArgumentException("分发记录不存在"));
        return DistributionDTO.fromRecord(record);
    }
    
    @Transactional
    @AuditLog(action = "CREATE_DISTRIBUTION_RECORD", entityType = "DISTRIBUTION_RECORD")
    public DistributionDTO createDistributionRecord(Long teamId, CreateDistributionRecordRequest request) {
        validateTeamAccessForWrite(teamId);
        
        Episode episode = episodeRepository.findByIdAndTeamId(request.getEpisodeId(), teamId)
                .orElseThrow(() -> new IllegalArgumentException("节目不存在"));
        
        DistributionPlatform platform = platformRepository.findByIdAndTeamId(request.getPlatformId(), teamId)
                .orElseThrow(() -> new IllegalArgumentException("平台不存在"));
        
        DistributionRecord record = DistributionRecord.builder()
                .episode(episode)
                .platform(platform)
                .status(DistributionRecord.Status.PENDING)
                .metadata(request.getMetadata())
                .build();
        
        record = recordRepository.save(record);
        
        AuditLogContext.addDetail("episodeId", request.getEpisodeId());
        AuditLogContext.addDetail("platformId", request.getPlatformId());
        
        return DistributionDTO.fromRecord(record);
    }
    
    @Transactional
    @AuditLog(action = "UPDATE_DISTRIBUTION_STATUS", entityType = "DISTRIBUTION_RECORD", entityIdParam = "id")
    public DistributionDTO updateDistributionStatus(Long id, Long teamId, UpdateDistributionStatusRequest request) {
        validateTeamAccessForWrite(teamId);
        DistributionRecord record = recordRepository.findByIdAndTeamId(id, teamId)
                .orElseThrow(() -> new IllegalArgumentException("分发记录不存在"));
        
        DistributionRecord.Status oldStatus = record.getStatus();
        record.setStatus(request.getStatus());
        
        if (request.getPublishUrl() != null) {
            record.setPublishUrl(request.getPublishUrl());
        }
        if (request.getErrorMessage() != null) {
            record.setErrorMessage(request.getErrorMessage());
        }
        if (request.getStatus() == DistributionRecord.Status.PUBLISHED && record.getPublishedAt() == null) {
            record.setPublishedAt(LocalDateTime.now());
        }
        
        record = recordRepository.save(record);
        
        AuditLogContext.addDetail("oldStatus", oldStatus.name());
        AuditLogContext.addDetail("newStatus", request.getStatus().name());
        
        return DistributionDTO.fromRecord(record);
    }
    
    @Transactional
    @AuditLog(action = "DELETE_DISTRIBUTION_RECORD", entityType = "DISTRIBUTION_RECORD", entityIdParam = "id")
    public void deleteDistributionRecord(Long id, Long teamId) {
        validateTeamAccessForWrite(teamId);
        DistributionRecord record = recordRepository.findByIdAndTeamId(id, teamId)
                .orElseThrow(() -> new IllegalArgumentException("分发记录不存在"));
        
        recordRepository.delete(record);
    }
    
    public String getRssFeedUrl(Long teamId) {
        return "http://localhost:8080/rss/team/" + teamId + "/feed.xml";
    }
}
