package com.podcast.collab.service;

import com.podcast.collab.audit.AuditContext;
import com.podcast.collab.dto.DistributionDTO;
import com.podcast.collab.dto.request.CreateDistributionPlatformRequest;
import com.podcast.collab.dto.request.CreateDistributionRecordRequest;
import com.podcast.collab.dto.request.UpdateDistributionPlatformRequest;
import com.podcast.collab.dto.request.UpdateDistributionStatusRequest;
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
    
    public List<DistributionDTO> getPlatforms() {
        Long teamId = securityUtil.getCurrentTeamId();
        List<DistributionPlatform> platforms = platformRepository.findByTeamId(teamId);
        return platforms.stream()
                .map(DistributionDTO::fromPlatform)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public DistributionDTO createPlatform(CreateDistributionPlatformRequest request) {
        Long teamId = securityUtil.getCurrentTeamId();
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("团队不存在"));
        
        DistributionPlatform platform = DistributionPlatform.builder()
                .team(team)
                .name(request.getName())
                .type(request.getType())
                .config(request.getConfig() != null ? request.getConfig() : Map.of())
                .build();
        
        platform = platformRepository.save(platform);
        return DistributionDTO.fromPlatform(platform);
    }
    
    @Transactional
    public DistributionDTO updatePlatform(Long id, UpdateDistributionPlatformRequest request) {
        Long teamId = securityUtil.getCurrentTeamId();
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
    public void deletePlatform(Long id) {
        Long teamId = securityUtil.getCurrentTeamId();
        DistributionPlatform platform = platformRepository.findByIdAndTeamId(id, teamId)
                .orElseThrow(() -> new IllegalArgumentException("平台不存在"));
        platformRepository.delete(platform);
    }
    
    public List<DistributionDTO> getDistributionRecords(Long episodeId, Long platformId, DistributionRecord.Status status) {
        Long teamId = securityUtil.getCurrentTeamId();
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
    
    public DistributionDTO getDistributionRecord(Long id) {
        Long teamId = securityUtil.getCurrentTeamId();
        DistributionRecord record = recordRepository.findByIdAndTeamId(id, teamId)
                .orElseThrow(() -> new IllegalArgumentException("分发记录不存在"));
        return DistributionDTO.fromRecord(record);
    }
    
    @Transactional
    public DistributionDTO createDistributionRecord(CreateDistributionRecordRequest request) {
        Long teamId = securityUtil.getCurrentTeamId();
        
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
        return DistributionDTO.fromRecord(record);
    }
    
    @Transactional
    public DistributionDTO updateDistributionStatus(Long id, UpdateDistributionStatusRequest request) {
        Long teamId = securityUtil.getCurrentTeamId();
        DistributionRecord record = recordRepository.findByIdAndTeamId(id, teamId)
                .orElseThrow(() -> new IllegalArgumentException("分发记录不存在"));
        
        AuditContext.put("oldStatus", record.getStatus().name());
        
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
        return DistributionDTO.fromRecord(record);
    }
    
    @Transactional
    public void deleteDistributionRecord(Long id) {
        Long teamId = securityUtil.getCurrentTeamId();
        DistributionRecord record = recordRepository.findByIdAndTeamId(id, teamId)
                .orElseThrow(() -> new IllegalArgumentException("分发记录不存在"));
        recordRepository.delete(record);
    }
    
    public String getRssFeedUrl(Long teamId) {
        return "http://localhost:8080/rss/team/" + teamId + "/feed.xml";
    }
}
