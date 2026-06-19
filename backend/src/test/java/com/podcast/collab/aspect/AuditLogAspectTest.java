package com.podcast.collab.aspect;

import com.podcast.collab.annotation.AuditLog;
import com.podcast.collab.dto.DistributionDTO;
import com.podcast.collab.dto.TaskDTO;
import com.podcast.collab.security.SecurityUtil;
import com.podcast.collab.service.AuditService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogAspectTest {
    
    @Mock
    private AuditService auditService;
    
    @Mock
    private SecurityUtil securityUtil;
    
    @Mock
    private ProceedingJoinPoint joinPoint;
    
    @Mock
    private MethodSignature methodSignature;
    
    private AuditLogAspect aspect;
    
    @BeforeEach
    void setUp() {
        aspect = new AuditLogAspect(auditService, securityUtil);
    }
    
    static class TestService {
        @AuditLog(action = "CREATE_PLATFORM", entityType = "DISTRIBUTION_PLATFORM")
        public DistributionDTO createPlatform(Long teamId, Object req) { return null; }
        
        @AuditLog(action = "UPDATE_PLATFORM", entityType = "DISTRIBUTION_PLATFORM", entityIdParam = "id")
        public DistributionDTO updatePlatform(Long id, Long teamId, Object req) { return null; }
        
        @AuditLog(action = "CREATE_DISTRIBUTION_RECORD", entityType = "DISTRIBUTION_RECORD")
        public DistributionDTO createDistributionRecord(Long teamId, Object req) { return null; }
        
        @AuditLog(action = "CREATE_TASK", entityType = "TASK")
        public TaskDTO createTask(Object req) { return null; }
        
        @AuditLog(action = "DELETE_TASK", entityType = "TASK", entityIdParam = "id")
        public void deleteTask(Long id) {}
    }
    
    private AuditLog getAuditLogAnnotation(String methodName, Class<?>... paramTypes) throws Exception {
        Method method = TestService.class.getMethod(methodName, paramTypes);
        return method.getAnnotation(AuditLog.class);
    }
    
    @Test
    @DisplayName("createPlatform: entityIdParam为空，必须从返回值DTO取id，不能误取teamId=100")
    void createPlatform_shouldExtractIdFromResult_notFromTeamIdArg() throws Throwable {
        Long teamIdFromSecurity = 100L;
        Long teamIdArg = 100L;
        Long newPlatformId = 999L;
        
        DistributionDTO returnedDto = DistributionDTO.builder().id(newPlatformId).build();
        
        AuditLog annotation = getAuditLogAnnotation("createPlatform", Long.class, Object.class);
        
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"teamId", "request"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{teamIdArg, new Object()});
        when(joinPoint.proceed()).thenReturn(returnedDto);
        when(securityUtil.getCurrentTeamId()).thenReturn(teamIdFromSecurity);
        when(securityUtil.getCurrentUserId()).thenReturn(1L);
        
        aspect.around(joinPoint, annotation);
        
        ArgumentCaptor<Long> entityIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(auditService).logAction(eq(teamIdFromSecurity), eq(1L),
                eq("CREATE_PLATFORM"), eq("DISTRIBUTION_PLATFORM"),
                entityIdCaptor.capture(), any());
        
        assertEquals(newPlatformId, entityIdCaptor.getValue(),
                "createPlatform的entityId必须是新生成的平台id(999)，不能错误地取teamId(100)");
    }
    
    @Test
    @DisplayName("createDistributionRecord: entityIdParam为空，必须从返回值取id，不能误取teamId")
    void createDistributionRecord_shouldExtractIdFromResult() throws Throwable {
        Long teamIdArg = 200L;
        Long newRecordId = 777L;
        
        DistributionDTO returnedDto = DistributionDTO.builder().id(newRecordId).build();
        
        AuditLog annotation = getAuditLogAnnotation("createDistributionRecord", Long.class, Object.class);
        
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"teamId", "request"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{teamIdArg, new Object()});
        when(joinPoint.proceed()).thenReturn(returnedDto);
        when(securityUtil.getCurrentTeamId()).thenReturn(teamIdArg);
        when(securityUtil.getCurrentUserId()).thenReturn(1L);
        
        aspect.around(joinPoint, annotation);
        
        ArgumentCaptor<Long> entityIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(auditService).logAction(eq(teamIdArg), eq(1L),
                eq("CREATE_DISTRIBUTION_RECORD"), eq("DISTRIBUTION_RECORD"),
                entityIdCaptor.capture(), any());
        
        assertEquals(newRecordId, entityIdCaptor.getValue(),
                "createDistributionRecord的entityId必须是新记录id(777)，不能是teamId(200)");
    }
    
    @Test
    @DisplayName("updatePlatform: entityIdParam='id'，应按名取路径参数id=555，忽略后面的teamId=100")
    void updatePlatform_shouldExtractIdFromNamedParam() throws Throwable {
        Long pathId = 555L;
        Long teamIdArg = 100L;
        
        DistributionDTO returnedDto = DistributionDTO.builder().id(pathId).build();
        
        AuditLog annotation = getAuditLogAnnotation("updatePlatform", Long.class, Long.class, Object.class);
        
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"id", "teamId", "request"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{pathId, teamIdArg, new Object()});
        when(joinPoint.proceed()).thenReturn(returnedDto);
        when(securityUtil.getCurrentTeamId()).thenReturn(teamIdArg);
        when(securityUtil.getCurrentUserId()).thenReturn(1L);
        
        aspect.around(joinPoint, annotation);
        
        ArgumentCaptor<Long> entityIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(auditService).logAction(eq(teamIdArg), eq(1L),
                eq("UPDATE_PLATFORM"), eq("DISTRIBUTION_PLATFORM"),
                entityIdCaptor.capture(), any());
        
        assertEquals(pathId, entityIdCaptor.getValue(),
                "updatePlatform的entityId必须是路径参数id=555");
    }
    
    @Test
    @DisplayName("deleteTask: entityIdParam='id'，方法void无返回值，应从参数取id")
    void deleteTask_shouldExtractIdFromArg_whenVoidReturn() throws Throwable {
        Long targetId = 333L;
        
        AuditLog annotation = getAuditLogAnnotation("deleteTask", Long.class);
        
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{targetId});
        when(joinPoint.proceed()).thenReturn(null);
        when(securityUtil.getCurrentTeamId()).thenReturn(100L);
        when(securityUtil.getCurrentUserId()).thenReturn(1L);
        
        aspect.around(joinPoint, annotation);
        
        ArgumentCaptor<Long> entityIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(auditService).logAction(eq(100L), eq(1L),
                eq("DELETE_TASK"), eq("TASK"),
                entityIdCaptor.capture(), any());
        
        assertEquals(targetId, entityIdCaptor.getValue(),
                "deleteTask返回void，entityId必须从参数id=333取");
    }
    
    @Test
    @DisplayName("createTask: entityIdParam为空，参数只有request DTO没有Long参数，必须从返回值取新id")
    void createTask_noLongArgInParams_shouldExtractFromResult() throws Throwable {
        Long newTaskId = 42L;
        TaskDTO returnedDto = TaskDTO.builder().id(newTaskId).build();
        
        AuditLog annotation = getAuditLogAnnotation("createTask", Object.class);
        
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"request"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{new Object()});
        when(joinPoint.proceed()).thenReturn(returnedDto);
        when(securityUtil.getCurrentTeamId()).thenReturn(100L);
        when(securityUtil.getCurrentUserId()).thenReturn(1L);
        
        aspect.around(joinPoint, annotation);
        
        ArgumentCaptor<Long> entityIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(auditService).logAction(eq(100L), eq(1L),
                eq("CREATE_TASK"), eq("TASK"),
                entityIdCaptor.capture(), any());
        
        assertEquals(newTaskId, entityIdCaptor.getValue(),
                "createTask参数里没有Long，entityId必须从返回值DTO.id=42取");
    }
}
