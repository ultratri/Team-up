package com.teamup.server.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.utils.SecurityUtils;
import com.teamup.server.modules.project.mapper.ProjectMapper;
import com.teamup.server.modules.tag.mapper.UserTagMapper;
import com.teamup.server.modules.tag.vo.UserSkillVO;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.entity.UserAvailability;
import com.teamup.server.modules.user.entity.UserCredit;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.mapper.UserAvailabilityMapper;
import com.teamup.server.modules.user.mapper.UserCreditMapper;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.mapper.UserProfileMapper;
import com.teamup.server.modules.user.mapper.UserRoleMapper;
import com.teamup.server.modules.user.service.TalentService;
import com.teamup.server.modules.user.vo.TalentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 人才墙服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TalentServiceImpl implements TalentService {
    
    private final UserMapper userMapper;
    private final UserProfileMapper profileMapper;
    private final UserAvailabilityMapper availabilityMapper;
    private final UserCreditMapper creditMapper;
    private final UserTagMapper tagMapper;
    private final UserRoleMapper roleMapper;
    private final ProjectMapper projectMapper;
    
    /**
     * 获取人才列表
     * 
     * 查询逻辑（优化版）：
     * 1. 使用优化的 JOIN 查询一次性获取符合条件的用户ID（已排序和分页）
     * 2. 根据用户ID批量加载详细信息
     * 3. 应用内存筛选（院系、关键词）
     * 4. 返回结果
     * 
     * 性能优化点：
     * - 使用 JOIN 减少数据库往返
     * - 在数据库层完成排序和基本筛选
     * - 使用缓存减少重复查询
     * - 只加载必要的数据
     */
    @Override
    public Page<TalentVO> getTalentList(Integer page, Integer size, 
                                        String department, String keyword, String intention) {
        
        Long currentUserId = SecurityUtils.getUserId();
        
        // 1. 确定可见范围条件
        List<String> visibilityConditions = getVisibilityConditions(currentUserId);
        
        // 2. 使用优化的查询获取用户ID列表（已在数据库层排序和筛选）
        Page<Long> userIdPage = new Page<>(page, size);
        try {
            userIdPage = availabilityMapper.selectTalentUserIds(
                userIdPage, 
                visibilityConditions, 
                intention
            );
        } catch (Exception e) {
            log.error("查询人才用户ID失败", e);
            // 降级到原有查询方式
            return getTalentListFallback(page, size, department, keyword, intention);
        }
        
        // 3. 批量加载用户详细信息并转换为VO
        List<TalentVO> talents = userIdPage.getRecords().stream()
            .map(userId -> {
                UserAvailability availability = availabilityMapper.selectByUserId(userId);
                return availability != null ? convertToTalentVO(availability, currentUserId) : null;
            })
            .filter(vo -> vo != null)
            .filter(vo -> matchesFilters(vo, department, keyword, null))  // 应用内存筛选
            .collect(Collectors.toList());
        
        // 4. 构建分页结果
        Page<TalentVO> result = new Page<>(page, size);
        result.setRecords(talents);
        result.setTotal(userIdPage.getTotal());
        result.setPages(userIdPage.getPages());
        
        return result;
    }
    
    /**
     * 获取可见范围条件
     */
    private List<String> getVisibilityConditions(Long currentUserId) {
        List<String> conditions = new java.util.ArrayList<>();
        conditions.add("PUBLIC");
        
        // 检查是否是导师
        List<String> roles = getUserRolesWithCache(currentUserId);
        if (roles != null && roles.contains("MENTOR")) {
            conditions.add("MENTOR");
        }
        
        // 检查是否有项目
        if (hasProjectsWithCache(currentUserId)) {
            conditions.add("PROJECT_CREATOR");
        }
        
        return conditions;
    }
    
    /**
     * 降级查询方法（当优化查询失败时使用）
     */
    private Page<TalentVO> getTalentListFallback(Integer page, Integer size, 
                                                 String department, String keyword, String intention) {
        log.warn("使用降级查询方法");
        
        // 构建查询条件
        LambdaQueryWrapper<UserAvailability> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAvailability::getIsAvailable, true);
        
        // 应用可见范围过滤
        Long currentUserId = SecurityUtils.getUserId();
        applyVisibilityFilter(wrapper, currentUserId);
        
        // 应用组队意向筛选
        if (StringUtils.hasText(intention)) {
            wrapper.like(UserAvailability::getIntention, intention);
        }
        
        // 查询所有符合条件的记录
        List<UserAvailability> allAvailabilities = availabilityMapper.selectList(wrapper);
        
        // 转换为VO并应用筛选和排序
        List<TalentVO> allTalents = allAvailabilities.stream()
            .map(this::convertToTalentVO)
            .filter(vo -> vo != null)
            .filter(this::meetsQualificationCriteria)
            .filter(vo -> matchesFilters(vo, department, keyword, null))
            .sorted(this::compareTalents)
            .collect(Collectors.toList());
        
        // 手动分页
        int total = allTalents.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        
        List<TalentVO> pagedTalents = fromIndex < total ? 
            allTalents.subList(fromIndex, toIndex) : 
            Collections.emptyList();
        
        // 构建分页结果
        Page<TalentVO> result = new Page<>(page, size);
        result.setRecords(pagedTalents);
        result.setTotal(total);
        result.setPages((total + size - 1) / size);
        
        return result;
    }
    
    /**
     * 应用可见范围过滤
     */
    private void applyVisibilityFilter(
            LambdaQueryWrapper<UserAvailability> wrapper, 
            Long currentUserId) {
        
        // 获取当前用户角色（使用缓存）
        List<String> roles = getUserRolesWithCache(currentUserId);
        boolean isMentor = roles != null && roles.contains("MENTOR");
        
        // 查询当前用户是否有项目（使用缓存）
        boolean hasProjects = hasProjectsWithCache(currentUserId);
        
        // 构建可见范围条件
        wrapper.and(w -> {
            w.eq(UserAvailability::getVisibility, "PUBLIC");
            if (isMentor) {
                w.or().eq(UserAvailability::getVisibility, "MENTOR");
            }
            if (hasProjects) {
                w.or().eq(UserAvailability::getVisibility, "PROJECT_CREATOR");
            }
        });
    }
    
    /**
     * 获取用户角色（带缓存）
     */
    @Cacheable(value = "userRoles", key = "#userId", unless = "#result == null")
    private List<String> getUserRolesWithCache(Long userId) {
        try {
            return roleMapper.getUserRoles(userId);
        } catch (Exception e) {
            log.warn("获取用户角色失败，userId: {}, error: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * 检查用户是否有项目（带缓存）
     */
    @Cacheable(value = "userProjectCount", key = "#userId")
    private boolean hasProjectsWithCache(Long userId) {
        try {
            LambdaQueryWrapper<com.teamup.server.modules.project.entity.Project> projectWrapper = 
                new LambdaQueryWrapper<>();
            projectWrapper.eq(com.teamup.server.modules.project.entity.Project::getCreatorId, userId);
            long projectCount = projectMapper.selectCount(projectWrapper);
            return projectCount > 0;
        } catch (Exception e) {
            log.warn("检查用户项目失败，userId: {}, error: {}", userId, e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查当前用户是否可以查看指定人才的联系方式
     */
    private boolean canViewContactInfo(String visibility, Long currentUserId) {
        // PUBLIC - 所有人可见
        if ("PUBLIC".equals(visibility)) {
            return true;
        }
        
        // MENTOR - 只有导师可见
        if ("MENTOR".equals(visibility)) {
            List<String> roles = getUserRolesWithCache(currentUserId);
            return roles != null && roles.contains("MENTOR");
        }
        
        // PROJECT_CREATOR - 只有项目创建者可见
        if ("PROJECT_CREATOR".equals(visibility)) {
            return hasProjectsWithCache(currentUserId);
        }
        
        return false;
    }
    
    /**
     * 验证用户是否满足上墙资格
     */
    private boolean meetsQualificationCriteria(TalentVO talent) {
        // 1. 基本信息完整
        if (!StringUtils.hasText(talent.getRealName()) ||
            !StringUtils.hasText(talent.getDepartment()) ||
            !StringUtils.hasText(talent.getMajor())) {
            return false;
        }
        
        // 2. 至少有1个技能标签
        if (talent.getSkills() == null || talent.getSkills().isEmpty()) {
            return false;
        }
        
        // 3. 账号状态为ACTIVE
        if (!"ACTIVE".equals(talent.getStatus())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 筛选逻辑
     */
    private boolean matchesFilters(TalentVO talent, String department, 
                                   String keyword, String intention) {
        // 院系筛选
        if (StringUtils.hasText(department)) {
            if (!department.equals(talent.getDepartment())) {
                return false;
            }
        }
        
        // 关键词搜索（姓名、技能、简介）
        if (StringUtils.hasText(keyword)) {
            String lowerKeyword = keyword.toLowerCase();
            boolean matchesName = talent.getRealName() != null && 
                                 talent.getRealName().toLowerCase().contains(lowerKeyword);
            boolean matchesBio = talent.getBio() != null && 
                                talent.getBio().toLowerCase().contains(lowerKeyword);
            boolean matchesSkills = talent.getSkills() != null && 
                                   talent.getSkills().stream()
                                       .anyMatch(skill -> skill.toLowerCase().contains(lowerKeyword));
            
            if (!matchesName && !matchesBio && !matchesSkills) {
                return false;
            }
        }
        
        // 组队意向筛选
        if (StringUtils.hasText(intention)) {
            if (talent.getIntentions() == null || 
                !talent.getIntentions().contains(intention)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 人才排序比较器
     */
    private int compareTalents(TalentVO a, TalentVO b) {
        // 1. 信誉分降序
        int creditCompare = Integer.compare(
            b.getCreditScore() != null ? b.getCreditScore() : 0,
            a.getCreditScore() != null ? a.getCreditScore() : 0
        );
        if (creditCompare != 0) return creditCompare;
        
        // 2. 最后登录时间降序
        if (a.getLastLoginAt() != null && b.getLastLoginAt() != null) {
            int loginCompare = b.getLastLoginAt().compareTo(a.getLastLoginAt());
            if (loginCompare != 0) return loginCompare;
        }
        
        // 3. 技能数量降序
        int aSkillCount = a.getSkills() != null ? a.getSkills().size() : 0;
        int bSkillCount = b.getSkills() != null ? b.getSkills().size() : 0;
        return Integer.compare(bSkillCount, aSkillCount);
    }
    
    /**
     * 转换为TalentVO
     */
    /**
         * 将UserAvailability转换为TalentVO
         */
        private TalentVO convertToTalentVO(UserAvailability availability) {
            return convertToTalentVO(availability, SecurityUtils.getUserId());
        }

        /**
         * 将UserAvailability转换为TalentVO（带权限控制）
         */
        private TalentVO convertToTalentVO(UserAvailability availability, Long currentUserId) {
            try {
                Long userId = availability.getUserId();

                // 查询用户基本信息
                User user = userMapper.selectById(userId);
                if (user == null) {
                    return null;
                }

                // 查询用户档案
                LambdaQueryWrapper<UserProfile> profileWrapper = new LambdaQueryWrapper<>();
                profileWrapper.eq(UserProfile::getUserId, userId);
                UserProfile profile = profileMapper.selectOne(profileWrapper);

                // 查询用户信誉
                LambdaQueryWrapper<UserCredit> creditWrapper = new LambdaQueryWrapper<>();
                creditWrapper.eq(UserCredit::getUserId, userId);
                UserCredit credit = creditMapper.selectOne(creditWrapper);

                // 查询用户技能标签
                List<UserSkillVO> skillVOs = tagMapper.selectUserSkills(userId);
                List<String> skills = skillVOs != null ? 
                    skillVOs.stream()
                        .map(UserSkillVO::getTagName)
                        .collect(Collectors.toList()) : 
                    Collections.emptyList();

                // 解析组队意向
                List<String> intentions = Collections.emptyList();
                if (StringUtils.hasText(availability.getIntention())) {
                    intentions = Arrays.asList(availability.getIntention().split(","));
                }

                // 检查是否可以查看联系方式
                boolean canViewContact = canViewContactInfo(availability.getVisibility(), currentUserId);

                // 构建TalentVO
                return TalentVO.builder()
                    .id(userId)
                    .username(user.getUsername())
                    .realName(profile != null ? profile.getRealName() : null)
                    .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                    .department(profile != null ? profile.getDepartment() : null)
                    .major(profile != null ? profile.getMajor() : null)
                    .bio(profile != null ? profile.getBio() : null)
                    .projectExperience(profile != null ? profile.getProjectExperience() : null)
                    .creditScore(credit != null ? credit.getTotalCredit() : 0)
                    .skills(skills)
                    .intentions(intentions)
                    .weeklyHours(availability.getWeeklyHours())
                    .notes(availability.getNotes())
                    .lastLoginAt(user.getLastLoginAt())
                    .status(user.getStatus())
                    .visibility(availability.getVisibility())
                    .availableFrom(availability.getAvailableFrom() != null ? 
                                  availability.getAvailableFrom().toString() : null)
                    .availableUntil(availability.getAvailableUntil() != null ? 
                                   availability.getAvailableUntil().toString() : null)
                    // 根据权限决定是否返回联系方式
                    .email(canViewContact ? user.getEmail() : null)
                    .phone(canViewContact ? user.getPhone() : null)
                    .wechat(canViewContact && profile != null ? profile.getWechat() : null)
                    .qq(canViewContact && profile != null ? profile.getQq() : null)
                    .build();
            } catch (Exception e) {
                // 记录错误但不中断处理
                return null;
            }
        }

}
