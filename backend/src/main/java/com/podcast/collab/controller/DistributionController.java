package com.podcast.collab.controller;

import com.podcast.collab.dto.ApiResponse;
import com.podcast.collab.dto.DistributionDTO;
import com.podcast.collab.dto.request.CreateDistributionRecordRequest;
import com.podcast.collab.dto.request.CreatePlatformRequest;
import com.podcast.collab.dto.request.UpdateDistributionStatusRequest;
import com.podcast.collab.dto.request.UpdatePlatformRequest;
import com.podcast.collab.entity.DistributionRecord;
import com.podcast.collab.service.DistributionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分发运营接口层。
 * Controller 不再直接操作 Repository，仅负责接收请求与返回响应；
 * 跨团队访问校验由 DistributionService 在入口调用 SecurityUtil 完成；
 * 参数校验由 DTO + @Valid 自动完成；
 * 审计日志由 @Audited 注解 + AuditAspect 自动写入。
 */
@RestController
@RequestMapping("/api/distribution")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DistributionController {

    private final DistributionService distributionService;

    @GetMapping("/platforms")
    public ResponseEntity<ApiResponse<List<DistributionDTO>>> getPlatforms(
            @RequestParam Long teamId) {
        return ResponseEntity.ok(ApiResponse.success(distributionService.listPlatforms(teamId)));
    }

    @PostMapping("/platforms")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<DistributionDTO>> createPlatform(
            @RequestParam Long teamId,
            @Valid @RequestBody CreatePlatformRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                distributionService.createPlatform(teamId, request), "平台创建成功"));
    }

    @PutMapping("/platforms/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<DistributionDTO>> updatePlatform(
            @PathVariable Long id,
            @RequestParam Long teamId,
            @Valid @RequestBody UpdatePlatformRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                distributionService.updatePlatform(id, teamId, request), "平台更新成功"));
    }

    @DeleteMapping("/platforms/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCER')")
    public ResponseEntity<ApiResponse<Void>> deletePlatform(
            @PathVariable Long id,
            @RequestParam Long teamId) {
        distributionService.deletePlatform(id, teamId);
        return ResponseEntity.ok(ApiResponse.success(null, "平台删除成功"));
    }

    @GetMapping("/records")
    public ResponseEntity<ApiResponse<List<DistributionDTO>>> getDistributionRecords(
            @RequestParam Long teamId,
            @RequestParam(required = false) Long episodeId,
            @RequestParam(required = false) Long platformId,
            @RequestParam(required = false) DistributionRecord.Status status) {
        return ResponseEntity.ok(ApiResponse.success(
                distributionService.listRecords(teamId, episodeId, platformId, status)));
    }

    @GetMapping("/records/{id}")
    public ResponseEntity<ApiResponse<DistributionDTO>> getDistributionRecord(
            @PathVariable Long id,
            @RequestParam Long teamId) {
        return ResponseEntity.ok(ApiResponse.success(distributionService.getRecord(id, teamId)));
    }

    @PostMapping("/records")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<DistributionDTO>> createDistributionRecord(
            @RequestParam Long teamId,
            @Valid @RequestBody CreateDistributionRecordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                distributionService.createRecord(teamId, request), "分发任务创建成功"));
    }

    @PatchMapping("/records/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<DistributionDTO>> updateDistributionStatus(
            @PathVariable Long id,
            @RequestParam Long teamId,
            @Valid @RequestBody UpdateDistributionStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                distributionService.updateRecordStatus(id, teamId, request), "状态更新成功"));
    }

    @DeleteMapping("/records/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCER')")
    public ResponseEntity<ApiResponse<Void>> deleteDistributionRecord(
            @PathVariable Long id,
            @RequestParam Long teamId) {
        distributionService.deleteRecord(id, teamId);
        return ResponseEntity.ok(ApiResponse.success(null, "分发记录删除成功"));
    }

    @GetMapping("/rss/{teamId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<String>> getRssFeed(@PathVariable Long teamId) {
        String rssUrl = "http://localhost:8080/rss/team/" + teamId + "/feed.xml";
        return ResponseEntity.ok(ApiResponse.success(rssUrl, "RSS订阅地址获取成功"));
    }
}
