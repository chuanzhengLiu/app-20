package com.podcast.collab.controller;

import com.podcast.collab.annotation.AuditLog;
import com.podcast.collab.annotation.TeamScope;
import com.podcast.collab.dto.ApiResponse;
import com.podcast.collab.dto.DistributionDTO;
import com.podcast.collab.dto.request.CreateDistributionPlatformRequest;
import com.podcast.collab.dto.request.CreateDistributionRecordRequest;
import com.podcast.collab.dto.request.UpdateDistributionPlatformRequest;
import com.podcast.collab.dto.request.UpdateDistributionStatusRequest;
import com.podcast.collab.entity.DistributionRecord;
import com.podcast.collab.service.DistributionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/distribution")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DistributionController {
    
    private final DistributionService distributionService;
    
    @GetMapping("/platforms")
    @TeamScope
    public ResponseEntity<ApiResponse<List<DistributionDTO>>> getPlatforms(
            @RequestParam Long teamId) {
        
        List<DistributionDTO> dtos = distributionService.getPlatforms();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
    
    @PostMapping("/platforms")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCER', 'OPERATOR')")
    @TeamScope(message = "无权操作其他团队数据")
    @AuditLog(action = "CREATE_DISTRIBUTION_PLATFORM", entityType = "DISTRIBUTION_PLATFORM", extractEntityIdFromResult = true,
            detailFields = {"name", "type"})
    public ResponseEntity<ApiResponse<DistributionDTO>> createPlatform(
            @RequestParam Long teamId,
            @Valid @RequestBody CreateDistributionPlatformRequest request) {
        
        DistributionDTO dto = distributionService.createPlatform(request);
        return ResponseEntity.ok(ApiResponse.success(dto, "平台创建成功"));
    }
    
    @PutMapping("/platforms/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCER', 'OPERATOR')")
    @TeamScope(message = "无权操作其他团队数据")
    @AuditLog(action = "UPDATE_DISTRIBUTION_PLATFORM", entityType = "DISTRIBUTION_PLATFORM", entityIdParam = "id")
    public ResponseEntity<ApiResponse<DistributionDTO>> updatePlatform(
            @PathVariable Long id,
            @RequestParam Long teamId,
            @Valid @RequestBody UpdateDistributionPlatformRequest request) {
        
        DistributionDTO dto = distributionService.updatePlatform(id, request);
        return ResponseEntity.ok(ApiResponse.success(dto, "平台更新成功"));
    }
    
    @DeleteMapping("/platforms/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCER')")
    @TeamScope(message = "无权操作其他团队数据")
    @AuditLog(action = "DELETE_DISTRIBUTION_PLATFORM", entityType = "DISTRIBUTION_PLATFORM", entityIdParam = "id")
    public ResponseEntity<ApiResponse<Void>> deletePlatform(
            @PathVariable Long id,
            @RequestParam Long teamId) {
        
        distributionService.deletePlatform(id);
        return ResponseEntity.ok(ApiResponse.success(null, "平台删除成功"));
    }
    
    @GetMapping("/records")
    @TeamScope
    public ResponseEntity<ApiResponse<List<DistributionDTO>>> getDistributionRecords(
            @RequestParam Long teamId,
            @RequestParam(required = false) Long episodeId,
            @RequestParam(required = false) Long platformId,
            @RequestParam(required = false) DistributionRecord.Status status) {
        
        List<DistributionDTO> dtos = distributionService.getDistributionRecords(episodeId, platformId, status);
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
    
    @GetMapping("/records/{id}")
    @TeamScope
    public ResponseEntity<ApiResponse<DistributionDTO>> getDistributionRecord(
            @PathVariable Long id,
            @RequestParam Long teamId) {
        
        DistributionDTO dto = distributionService.getDistributionRecord(id);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }
    
    @PostMapping("/records")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCER', 'OPERATOR')")
    @TeamScope(message = "无权操作其他团队数据")
    @AuditLog(action = "CREATE_DISTRIBUTION_RECORD", entityType = "DISTRIBUTION_RECORD", extractEntityIdFromResult = true,
            detailFields = {"episodeId", "platformId"})
    public ResponseEntity<ApiResponse<DistributionDTO>> createDistributionRecord(
            @RequestParam Long teamId,
            @Valid @RequestBody CreateDistributionRecordRequest request) {
        
        DistributionDTO dto = distributionService.createDistributionRecord(request);
        return ResponseEntity.ok(ApiResponse.success(dto, "分发任务创建成功"));
    }
    
    @PatchMapping("/records/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCER', 'OPERATOR')")
    @TeamScope(message = "无权操作其他团队数据")
    @AuditLog(action = "UPDATE_DISTRIBUTION_STATUS", entityType = "DISTRIBUTION_RECORD", entityIdParam = "id",
            detailFields = {"status:newStatus", "publishUrl", "errorMessage"})
    public ResponseEntity<ApiResponse<DistributionDTO>> updateDistributionStatus(
            @PathVariable Long id,
            @RequestParam Long teamId,
            @Valid @RequestBody UpdateDistributionStatusRequest request) {
        
        DistributionDTO dto = distributionService.updateDistributionStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(dto, "状态更新成功"));
    }
    
    @DeleteMapping("/records/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCER')")
    @TeamScope(message = "无权操作其他团队数据")
    @AuditLog(action = "DELETE_DISTRIBUTION_RECORD", entityType = "DISTRIBUTION_RECORD", entityIdParam = "id")
    public ResponseEntity<ApiResponse<Void>> deleteDistributionRecord(
            @PathVariable Long id,
            @RequestParam Long teamId) {
        
        distributionService.deleteDistributionRecord(id);
        return ResponseEntity.ok(ApiResponse.success(null, "分发记录删除成功"));
    }
    
    @GetMapping("/rss/{teamId}")
    public ResponseEntity<ApiResponse<String>> getRssFeed(@PathVariable Long teamId) {
        String rssUrl = distributionService.getRssFeedUrl(teamId);
        return ResponseEntity.ok(ApiResponse.success(rssUrl, "RSS订阅地址获取成功"));
    }
}
