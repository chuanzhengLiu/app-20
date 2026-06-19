package com.podcast.collab.service;

import com.podcast.collab.audit.AuditContext;
import com.podcast.collab.audit.Audited;
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

/**
 * 分发运营业务服务。
 * 团队权限通过 SecurityUtil.requireCurrentTeamIdForRead/ForWrite 在 Service 入口主动校验：
 *   - 读接口跨团队抛 400「无权访问其他团队数据」
 *   - 写接口跨团队抛 400「无权操作其他团队数据」
 * 审计日志通过 @Audited 注解 + AuditAspect 自动写入，方法体不再嵌入 auditService 调用。
 */
@Service
@RequiredArgsConstructor
public class DistributionService {

    private final DistributionPlatformRepository platformRepository;
    private final DistributionRecordRepository recordRepository;
    private final EpisodeRepository episodeRepository;
    private final TeamRepository teamRepository;
    private final SecurityUtil securityUtil;

    @Transactional(readOnly = true)
    public List<DistributionDTO> listPlatforms(Long teamId) {
        Long currentTeamId = securityUtil.requireCurrentTeamIdForRead(teamId);
        return platformRepository.findByTeamId(currentTeamId).stream()
                .map(DistributionDTO::fromPlatform)
                .collect(Collectors.toList());
    }

    @Transactional
    @Audited(action = "CREATE_DISTRIBUTION_PLATFORM", entityType = "DISTRIBUTION_PLATFORM",
            details = "T(java.util.Map).of('name', #request.name, 'type', #request.type.name())")
    public DistributionDTO createPlatform(Long teamId, CreatePlatformRequest request) {
        Long currentTeamId = securityUtil.requireCurrentTeamIdForWrite(teamId);
        Team team = teamRepository.findById(currentTeamId)
                .orElseThrow(() -> new IllegalArgumentException("团队不存在"));

        DistributionPlatform platform = DistributionPlatform.builder()
                .team(team)
                .name(request.getName())
                .type(request.getType())
                .config(request.getConfig() != null ? request.getConfig() : Map.of())
                .build();

        return DistributionDTO.fromPlatform(platformRepository.save(platform));
    }

    @Transactional
    @Audited(action = "UPDATE_DISTRIBUTION_PLATFORM", entityType = "DISTRIBUTION_PLATFORM",
            entityId = "#id")
    public DistributionDTO updatePlatform(Long id, Long teamId, UpdatePlatformRequest request) {
        Long currentTeamId = securityUtil.requireCurrentTeamIdForWrite(teamId);
        DistributionPlatform platform = platformRepository.findByIdAndTeamId(id, currentTeamId)
                .orElseThrow(() -> new IllegalArgumentException("平台不存在"));

        if (request.getName() != null) {
            platform.setName(request.getName());
        }
        if (request.getConfig() != null) {
            platform.setConfig(request.getConfig());
        }

        return DistributionDTO.fromPlatform(platformRepository.save(platform));
    }

    @Transactional
    @Audited(action = "DELETE_DISTRIBUTION_PLATFORM", entityType = "DISTRIBUTION_PLATFORM",
            entityId = "#id")
    public void deletePlatform(Long id, Long teamId) {
        Long currentTeamId = securityUtil.requireCurrentTeamIdForWrite(teamId);
        DistributionPlatform platform = platformRepository.findByIdAndTeamId(id, currentTeamId)
                .orElseThrow(() -> new IllegalArgumentException("平台不存在"));
        platformRepository.delete(platform);
    }

    @Transactional(readOnly = true)
    public List<DistributionDTO> listRecords(Long teamId, Long episodeId, Long platformId,
                                             DistributionRecord.Status status) {
        Long currentTeamId = securityUtil.requireCurrentTeamIdForRead(teamId);

        List<DistributionRecord> records;
        if (episodeId != null) {
            records = recordRepository.findByEpisodeIdAndTeamId(episodeId, currentTeamId);
        } else if (platformId != null) {
            records = recordRepository.findByPlatformIdAndTeamId(platformId, currentTeamId);
        } else if (status != null) {
            records = recordRepository.findByStatusAndTeamId(status, currentTeamId);
        } else {
            records = recordRepository.findByTeamId(currentTeamId);
        }

        return records.stream()
                .map(DistributionDTO::fromRecord)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DistributionDTO getRecord(Long id, Long teamId) {
        Long currentTeamId = securityUtil.requireCurrentTeamIdForRead(teamId);
        DistributionRecord record = recordRepository.findByIdAndTeamId(id, currentTeamId)
                .orElseThrow(() -> new IllegalArgumentException("分发记录不存在"));
        return DistributionDTO.fromRecord(record);
    }

    @Transactional
    @Audited(action = "CREATE_DISTRIBUTION_RECORD", entityType = "DISTRIBUTION_RECORD",
            details = "T(java.util.Map).of('episodeId', #request.episodeId, 'platformId', #request.platformId)")
    public DistributionDTO createRecord(Long teamId, CreateDistributionRecordRequest request) {
        Long currentTeamId = securityUtil.requireCurrentTeamIdForWrite(teamId);

        Episode episode = episodeRepository.findByIdAndTeamId(request.getEpisodeId(), currentTeamId)
                .orElseThrow(() -> new IllegalArgumentException("节目不存在"));
        DistributionPlatform platform = platformRepository.findByIdAndTeamId(request.getPlatformId(), currentTeamId)
                .orElseThrow(() -> new IllegalArgumentException("平台不存在"));

        DistributionRecord record = DistributionRecord.builder()
                .episode(episode)
                .platform(platform)
                .status(DistributionRecord.Status.PENDING)
                .metadata(request.getMetadata())
                .build();

        return DistributionDTO.fromRecord(recordRepository.save(record));
    }

    @Transactional
    @Audited(action = "UPDATE_DISTRIBUTION_STATUS", entityType = "DISTRIBUTION_RECORD",
            entityId = "#id")
    public DistributionDTO updateRecordStatus(Long id, Long teamId, UpdateDistributionStatusRequest request) {
        Long currentTeamId = securityUtil.requireCurrentTeamIdForWrite(teamId);
        DistributionRecord record = recordRepository.findByIdAndTeamId(id, currentTeamId)
                .orElseThrow(() -> new IllegalArgumentException("分发记录不存在"));

        DistributionRecord.Status oldStatus = record.getStatus();
        DistributionRecord.Status newStatus = request.getStatus();

        AuditContext.put("oldStatus", oldStatus.name());
        AuditContext.put("newStatus", newStatus.name());

        record.setStatus(newStatus);
        if (request.getPublishUrl() != null) {
            record.setPublishUrl(request.getPublishUrl());
        }
        if (request.getErrorMessage() != null) {
            record.setErrorMessage(request.getErrorMessage());
        }
        if (newStatus == DistributionRecord.Status.PUBLISHED && record.getPublishedAt() == null) {
            record.setPublishedAt(LocalDateTime.now());
        }

        return DistributionDTO.fromRecord(recordRepository.save(record));
    }

    @Transactional
    @Audited(action = "DELETE_DISTRIBUTION_RECORD", entityType = "DISTRIBUTION_RECORD",
            entityId = "#id")
    public void deleteRecord(Long id, Long teamId) {
        Long currentTeamId = securityUtil.requireCurrentTeamIdForWrite(teamId);
        DistributionRecord record = recordRepository.findByIdAndTeamId(id, currentTeamId)
                .orElseThrow(() -> new IllegalArgumentException("分发记录不存在"));
        recordRepository.delete(record);
    }
}
