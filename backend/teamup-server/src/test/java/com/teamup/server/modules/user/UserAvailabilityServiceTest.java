package com.teamup.server.modules.user;

import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.tag.entity.UserTag;
import com.teamup.server.modules.tag.mapper.UserTagMapper;
import com.teamup.server.modules.user.dto.UserAvailabilityRequest;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.entity.UserAvailability;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.mapper.UserAvailabilityMapper;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.mapper.UserProfileMapper;
import com.teamup.server.modules.user.service.impl.UserAvailabilityServiceImpl;
import com.teamup.server.modules.user.vo.UserAvailabilityVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserAvailabilityService单元测试
 */
@ExtendWith(MockitoExtension.class)
class UserAvailabilityServiceTest {
    
    @Mock
    private UserAvailabilityMapper availabilityMapper;
    
    @Mock
    private UserMapper userMapper;
    
    @Mock
    private UserProfileMapper profileMapper;
    
    @Mock
    private UserTagMapper tagMapper;
    
    @InjectMocks
    private UserAvailabilityServiceImpl service;
    
    private User testUser;
    private UserProfile testProfile;
    private UserAvailability testAvailability;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setStatus("ACTIVE");
        
        testProfile = new UserProfile();
        testProfile.setId(1L);
        testProfile.setUserId(1L);
        testProfile.setRealName("测试用户");
        testProfile.setDepartment("计算机学院");
        testProfile.setMajor("软件工程");
        
        testAvailability = new UserAvailability();
        testAvailability.setId(1L);
        testAvailability.setUserId(1L);
        testAvailability.setIsAvailable(true);
        testAvailability.setIntention("JOIN_PROJECT,FIND_TEAMMATES");
        testAvailability.setVisibility("PUBLIC");
        testAvailability.setWeeklyHours(20);
        testAvailability.setNotes("测试备注");
    }
    
    @Test
    void testGetUserAvailability_WhenExists() {
        // Given
        when(availabilityMapper.selectByUserId(1L)).thenReturn(testAvailability);
        
        // When
        UserAvailabilityVO result = service.getUserAvailability(1L);
        
        // Then
        assertNotNull(result);
        assertTrue(result.getIsAvailable());
        assertEquals(2, result.getIntentions().size());
        assertTrue(result.getIntentions().contains("JOIN_PROJECT"));
        assertTrue(result.getIntentions().contains("FIND_TEAMMATES"));
        assertEquals("PUBLIC", result.getVisibility());
        assertEquals(20, result.getWeeklyHours());
        assertEquals("测试备注", result.getNotes());
        
        verify(availabilityMapper).selectByUserId(1L);
    }
    
    @Test
    void testGetUserAvailability_WhenNotExists() {
        // Given
        when(availabilityMapper.selectByUserId(1L)).thenReturn(null);
        
        // When
        UserAvailabilityVO result = service.getUserAvailability(1L);
        
        // Then
        assertNotNull(result);
        assertFalse(result.getIsAvailable());
        assertTrue(result.getIntentions().isEmpty());
        assertEquals("PUBLIC", result.getVisibility());
        
        verify(availabilityMapper).selectByUserId(1L);
    }
    
    @Test
    void testUpdateAvailability_CreateNew_Success() {
        // Given
        UserAvailabilityRequest request = new UserAvailabilityRequest();
        request.setIsAvailable(true);
        request.setIntentions(Arrays.asList("JOIN_PROJECT", "FIND_TEAMMATES"));
        request.setVisibility("PUBLIC");
        request.setWeeklyHours(20);
        request.setNotes("测试备注");
        
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(profileMapper.selectOne(any())).thenReturn(testProfile);
        when(tagMapper.selectCount(any())).thenReturn(1L);
        when(availabilityMapper.selectByUserId(1L)).thenReturn(null);
        when(availabilityMapper.insert(any())).thenReturn(1);
        
        // When
        service.updateAvailability(1L, request);
        
        // Then
        verify(userMapper).selectById(1L);
        verify(profileMapper).selectOne(any());
        verify(tagMapper).selectCount(any());
        verify(availabilityMapper).selectByUserId(1L);
        verify(availabilityMapper).insert(any());
    }
    
    @Test
    void testUpdateAvailability_UpdateExisting_Success() {
        // Given
        UserAvailabilityRequest request = new UserAvailabilityRequest();
        request.setIsAvailable(true);
        request.setIntentions(Arrays.asList("FIND_MENTOR"));
        request.setVisibility("MENTOR");
        request.setWeeklyHours(10);
        
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(profileMapper.selectOne(any())).thenReturn(testProfile);
        when(tagMapper.selectCount(any())).thenReturn(1L);
        when(availabilityMapper.selectByUserId(1L)).thenReturn(testAvailability);
        when(availabilityMapper.updateById(any())).thenReturn(1);
        
        // When
        service.updateAvailability(1L, request);
        
        // Then
        verify(userMapper).selectById(1L);
        verify(profileMapper).selectOne(any());
        verify(tagMapper).selectCount(any());
        verify(availabilityMapper).selectByUserId(1L);
        verify(availabilityMapper).updateById(any());
    }
    
    @Test
    void testUpdateAvailability_UserNotExists_ThrowsException() {
        // Given
        UserAvailabilityRequest request = new UserAvailabilityRequest();
        request.setIsAvailable(true);
        
        when(userMapper.selectById(1L)).thenReturn(null);
        
        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> service.updateAvailability(1L, request));
        
        assertEquals("用户不存在", exception.getMessage());
        verify(userMapper).selectById(1L);
        verify(availabilityMapper, never()).insert(any());
        verify(availabilityMapper, never()).updateById(any());
    }
    
    @Test
    void testUpdateAvailability_MissingProfile_ThrowsException() {
        // Given
        UserAvailabilityRequest request = new UserAvailabilityRequest();
        request.setIsAvailable(true);
        
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(profileMapper.selectOne(any())).thenReturn(null);
        
        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> service.updateAvailability(1L, request));
        
        assertEquals("请先完善基本信息（真实姓名、院系、专业）", exception.getMessage());
        verify(userMapper).selectById(1L);
        verify(profileMapper).selectOne(any());
    }
    
    @Test
    void testUpdateAvailability_MissingRealName_ThrowsException() {
        // Given
        UserAvailabilityRequest request = new UserAvailabilityRequest();
        request.setIsAvailable(true);
        
        testProfile.setRealName(null);
        
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(profileMapper.selectOne(any())).thenReturn(testProfile);
        
        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> service.updateAvailability(1L, request));
        
        assertEquals("请先完善基本信息（真实姓名、院系、专业）", exception.getMessage());
    }
    
    @Test
    void testUpdateAvailability_NoSkillTags_ThrowsException() {
        // Given
        UserAvailabilityRequest request = new UserAvailabilityRequest();
        request.setIsAvailable(true);
        
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(profileMapper.selectOne(any())).thenReturn(testProfile);
        when(tagMapper.selectCount(any())).thenReturn(0L);
        
        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> service.updateAvailability(1L, request));
        
        assertEquals("请先添加至少1个技能标签", exception.getMessage());
        verify(tagMapper).selectCount(any());
    }
    
    @Test
    void testUpdateAvailability_InactiveUser_ThrowsException() {
        // Given
        UserAvailabilityRequest request = new UserAvailabilityRequest();
        request.setIsAvailable(true);
        
        testUser.setStatus("INACTIVE");
        
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(profileMapper.selectOne(any())).thenReturn(testProfile);
        when(tagMapper.selectCount(any())).thenReturn(1L);
        
        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> service.updateAvailability(1L, request));
        
        assertEquals("账号状态异常，无法上墙", exception.getMessage());
    }
    
    @Test
    void testUpdateAvailability_SetToUnavailable_NoValidation() {
        // Given
        UserAvailabilityRequest request = new UserAvailabilityRequest();
        request.setIsAvailable(false);
        request.setIntentions(Arrays.asList());
        request.setVisibility("PUBLIC");
        
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(availabilityMapper.selectByUserId(1L)).thenReturn(testAvailability);
        when(availabilityMapper.updateById(any())).thenReturn(1);
        
        // When
        service.updateAvailability(1L, request);
        
        // Then
        verify(userMapper).selectById(1L);
        verify(availabilityMapper).selectByUserId(1L);
        verify(availabilityMapper).updateById(any());
        // 验证没有调用资格验证相关的方法
        verify(profileMapper, never()).selectOne(any());
        verify(tagMapper, never()).selectCount(any());
    }
}
