package com.teamup.server.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.modules.user.dto.LoginRequest;
import com.teamup.server.modules.user.dto.RegisterRequest;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.entity.UserCredit;
import com.teamup.server.modules.user.entity.UserRole;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.mapper.UserProfileMapper;
import com.teamup.server.modules.user.mapper.UserCreditMapper;
import com.teamup.server.modules.user.mapper.UserRoleMapper;
import com.teamup.server.modules.user.security.JwtUtil;
import com.teamup.server.modules.user.service.UserService;
import com.teamup.server.modules.user.vo.LoginResponse;
import com.teamup.server.modules.user.vo.UserVO;
import com.teamup.server.common.exception.BusinessException;
import org.springframework.beans.BeanUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserCreditMapper userCreditMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request) {
        // 检查学号/工号是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserCode, request.getUserCode());
        if (userMapper.selectCount(wrapper) > 0) {
            String roleHint = "MENTOR".equals(request.getRole()) ? "工号" : "学号";
            throw new BusinessException(roleHint + "已存在");
        }

        // 检查用户名是否已存在
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 检查邮箱是否已存在
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, request.getEmail());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("邮箱已被使用");
        }

        // 创建用户
        User user = new User();
        user.setUserCode(request.getUserCode());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        
        // 导师注册需要审核，状态设为 INACTIVE；学生直接激活
        String requestedRole = request.getRole();
        if (requestedRole == null || requestedRole.isEmpty()) {
            requestedRole = "STUDENT";
        }
        user.setStatus("MENTOR".equals(requestedRole) ? "INACTIVE" : "ACTIVE");
        
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        userMapper.insert(user);

        // 创建用户档案
        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        userProfileMapper.insert(profile);

        // 创建用户信誉记录
        UserCredit credit = new UserCredit();
        credit.setUserId(user.getId());
        credit.setTotalCredit(0);
        credit.setCreditLevel("NEWBIE");
        credit.setUpdatedAt(LocalDateTime.now());
        userCreditMapper.insert(credit);

        // 分配角色
        if (requestedRole == null || requestedRole.isEmpty()) {
            requestedRole = "STUDENT";
        }
        
        // 验证角色合法性：只允许 STUDENT 和 MENTOR
        if (!"STUDENT".equals(requestedRole) && !"MENTOR".equals(requestedRole)) {
            throw new BusinessException("无效的注册角色，只能注册为学生或导师");
        }
        
        // 删除该用户的所有现有角色（确保只有一个角色）
        LambdaQueryWrapper<UserRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(UserRole::getUserId, user.getId());
        userRoleMapper.delete(roleWrapper);
        
        // 插入新角色
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleName(requestedRole);
        userRole.setGrantedAt(LocalDateTime.now());
        userRoleMapper.insert(userRole);

        // 生成临时登录凭证（need-confirm 页面完成确认后前端再真正落地 token 到 store）
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        List<String> roles = new ArrayList<>();
        roles.add(requestedRole);

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        userVO.setRoles(roles);

        return new LoginResponse(token, userVO);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // 查找用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserCode, request.getUserCode());
        User user = userMapper.selectOne(wrapper);
        
        if (user == null) {
            throw new BusinessException("学号/工号不存在，请检查后重试");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误，请重新输入");
        }

        // 检查账号状态
        if (!"ACTIVE".equals(user.getStatus())) {
            if ("INACTIVE".equals(user.getStatus())) {
                throw new BusinessException("账号待审核，请等待管理员审核通过");
            } else if ("BANNED".equals(user.getStatus())) {
                throw new BusinessException("账号已被禁用，请联系管理员");
            } else {
                throw new BusinessException("账号状态异常，请联系管理员");
            }
        }

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        // 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        // 获取用户角色
        List<String> roles = userRoleMapper.getUserRoles(user.getId());
        if (roles == null) {
            roles = new java.util.ArrayList<>(); // 如果角色为空，初始化为空列表
        }
        
        // 构建 UserVO
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        userVO.setRoles(roles);
        
        // 加载用户个人资料
        try {
            LambdaQueryWrapper<UserProfile> profileWrapper = new LambdaQueryWrapper<>();
            profileWrapper.eq(UserProfile::getUserId, user.getId());
            UserProfile profile = userProfileMapper.selectOne(profileWrapper);
            
            if (profile != null) {
                com.teamup.server.modules.user.vo.UserProfileVO profileVO = 
                    new com.teamup.server.modules.user.vo.UserProfileVO();
                BeanUtils.copyProperties(profile, profileVO);
                userVO.setProfile(profileVO);
                log.info("✅ 用户 {} 的profile已加载", user.getUserCode());
            } else {
                log.warn("⚠️ 用户 {} 没有profile信息", user.getUserCode());
            }
        } catch (Exception e) {
            log.error("❌ 加载用户profile失败: {}", e.getMessage());
            // 不影响登录，继续
        }

        return new LoginResponse(token, userVO);
    }

    @Override
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setPassword(null);  // 不返回密码
            // 填充角色信息
            try {
                List<String> roles = userRoleMapper.getUserRoles(id);
                user.setRoles(roles != null ? roles : new ArrayList<>());
            } catch (Exception e) {
                log.error("Failed to fetch roles for user {}: {}", id, e.getMessage());
                user.setRoles(new ArrayList<>());
            }
        }
        return user;
    }

    @Override
    public User getUserByUserCode(String userCode) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserCode, userCode);
        User user = userMapper.selectOne(wrapper);
        if (user != null) {
            user.setPassword(null);  // 不返回密码
        }
        return user;
    }

    @Override
    public java.util.List<User> searchUsers(String keyword, int limit) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w
                .like(User::getUsername, kw)
                .or()
                .like(User::getUserCode, kw)
                .or()
                .like(User::getEmail, kw)
            );
        }
        wrapper.eq(User::getStatus, "ACTIVE");  // 只返回活跃用户
        wrapper.last("LIMIT " + Math.min(limit, 50));  // 最多返回50条
        java.util.List<User> users = userMapper.selectList(wrapper);
        // 清除所有用户的密码
        users.forEach(user -> user.setPassword(null));
        return users;
    }

    @Override
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> getUserList(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> page,
            String keyword,
            String role,
            String status,
            String sortBy
    ) {
        log.info("getUserList called with: keyword={}, role={}, status={}, sortBy={}", keyword, role, status, sortBy);
        
        // 特殊处理角色排序
        if (sortBy != null && sortBy.startsWith("role_")) {
            log.info("Using role-based sorting");
            return getUserListWithRoleSort(page, keyword, role, status, sortBy);
        }
        
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        // 关键词搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w
                .like(User::getUsername, kw)
                .or()
                .like(User::getUserCode, kw)
                .or()
                .like(User::getEmail, kw)
            );
        }
        
        // 状态筛选
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(User::getStatus, status);
        }
        
        // 角色筛选（通过 user_roles 表关联）
        if (role != null && !role.trim().isEmpty()) {
            wrapper.inSql(User::getId, 
                "SELECT user_id FROM user_roles WHERE role_name = '" + role + "'");
        }
        
        // 排序处理
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            log.info("Applying sort: {}", sortBy);
            String[] parts = sortBy.split("_");
            if (parts.length == 2) {
                String field = parts[0];
                String order = parts[1];
                boolean isAsc = "asc".equalsIgnoreCase(order);
                
                log.info("Sort field: {}, order: {}, isAsc: {}", field, order, isAsc);
                
                switch (field) {
                    case "id":
                        wrapper.orderBy(true, isAsc, User::getId);
                        log.info("Sorting by ID");
                        break;
                    case "userCode":
                        wrapper.orderBy(true, isAsc, User::getUserCode);
                        // 二级排序：用户编号相同时按创建时间
                        wrapper.orderByDesc(User::getCreatedAt);
                        log.info("Sorting by userCode with secondary sort by createdAt");
                        break;
                    case "username":
                        wrapper.orderBy(true, isAsc, User::getUsername);
                        // 二级排序：用户名相同时按创建时间
                        wrapper.orderByDesc(User::getCreatedAt);
                        log.info("Sorting by username with secondary sort by createdAt");
                        break;
                    case "email":
                        wrapper.orderBy(true, isAsc, User::getEmail);
                        // 二级排序：邮箱相同时按创建时间
                        wrapper.orderByDesc(User::getCreatedAt);
                        log.info("Sorting by email with secondary sort by createdAt");
                        break;
                    case "phone":
                        wrapper.orderBy(true, isAsc, User::getPhone);
                        // 二级排序：手机号相同时按创建时间
                        wrapper.orderByDesc(User::getCreatedAt);
                        log.info("Sorting by phone with secondary sort by createdAt");
                        break;
                    case "status":
                        wrapper.orderBy(true, isAsc, User::getStatus);
                        // 二级排序：状态相同时按创建时间
                        wrapper.orderByDesc(User::getCreatedAt);
                        log.info("Sorting by status with secondary sort by createdAt");
                        break;
                    case "createdAt":
                        wrapper.orderBy(true, isAsc, User::getCreatedAt);
                        log.info("Sorting by createdAt");
                        break;
                    case "lastLogin":
                    case "lastLoginAt":
                        wrapper.orderBy(true, isAsc, User::getLastLoginAt);
                        // 二级排序：最后登录时间相同时按创建时间
                        wrapper.orderByDesc(User::getCreatedAt);
                        log.info("Sorting by lastLoginAt with secondary sort by createdAt");
                        break;
                    default:
                        // 默认按创建时间降序
                        wrapper.orderByDesc(User::getCreatedAt);
                        log.warn("Unknown sort field: {}, using default sort", field);
                        break;
                }
            } else {
                // 默认按创建时间降序
                wrapper.orderByDesc(User::getCreatedAt);
                log.warn("Invalid sortBy format: {}, using default sort", sortBy);
            }
        } else {
            // 默认按创建时间降序
            wrapper.orderByDesc(User::getCreatedAt);
            log.info("No sort specified, using default sort (createdAt desc)");
        }
        
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> result = 
            userMapper.selectPage(page, wrapper);
        
        // 清除密码
        result.getRecords().forEach(user -> user.setPassword(null));
        
        return result;
    }
    
    /**
     * 按角色排序的用户列表查询
     * 由于角色存储在关联表中，需要特殊处理
     */
    private com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> getUserListWithRoleSort(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> page,
            String keyword,
            String role,
            String status,
            String sortBy
    ) {
        // 先查询所有符合条件的用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        // 关键词搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w
                .like(User::getUsername, kw)
                .or()
                .like(User::getUserCode, kw)
                .or()
                .like(User::getEmail, kw)
            );
        }
        
        // 状态筛选
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(User::getStatus, status);
        }
        
        // 角色筛选
        if (role != null && !role.trim().isEmpty()) {
            wrapper.inSql(User::getId, 
                "SELECT user_id FROM user_roles WHERE role_name = '" + role + "'");
        }
        
        // 先获取所有数据（不分页）
        List<User> allUsers = userMapper.selectList(wrapper);
        
        // 为每个用户获取角色并计算优先级
        boolean isAsc = sortBy.endsWith("_asc");
        allUsers.forEach(user -> {
            List<String> roles = userRoleMapper.getUserRoles(user.getId());
            user.setRoles(roles);
        });
        
        // 按角色优先级排序
        allUsers.sort((u1, u2) -> {
            int priority1 = getRolePriority(u1.getRoles());
            int priority2 = getRolePriority(u2.getRoles());
            
            if (isAsc) {
                // 学生优先：STUDENT(1) < MENTOR(2) < PLATFORM_ADMIN(4)
                return Integer.compare(priority1, priority2);
            } else {
                // 管理员优先：PLATFORM_ADMIN(4) > MENTOR(2) > STUDENT(1)
                return Integer.compare(priority2, priority1);
            }
        });
        
        // 手动分页
        int start = (int) ((page.getCurrent() - 1) * page.getSize());
        int end = (int) Math.min(start + page.getSize(), allUsers.size());
        List<User> pageUsers = allUsers.subList(start, end);
        
        // 清除密码
        pageUsers.forEach(user -> user.setPassword(null));
        
        // 构建分页结果
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> result = 
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                page.getCurrent(), page.getSize(), allUsers.size());
        result.setRecords(pageUsers);
        
        return result;
    }
    
    /**
     * 获取角色优先级
     * PLATFORM_ADMIN=4, MENTOR=2, STUDENT=1
     */
    private int getRolePriority(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return 1; // 默认为学生
        }
        
        // 返回最高优先级的角色
        if (roles.contains("PLATFORM_ADMIN")) return 4;
        
        if (roles.contains("MENTOR")) return 2;
        return 1; // STUDENT 或其他
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User createUserByAdmin(RegisterRequest request) {
        // 检查学号/工号是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserCode, request.getUserCode());
        if (userMapper.selectCount(wrapper) > 0) {
            String roleHint = "MENTOR".equals(request.getRole()) ? "工号" : "学号";
            throw new RuntimeException(roleHint + "已存在");
        }

        // 创建用户
        User user = new User();
        user.setUserCode(request.getUserCode());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        // 创建用户档案
        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        userProfileMapper.insert(profile);

        // 创建用户信用
        UserCredit credit = new UserCredit();
        credit.setUserId(user.getId());
        credit.setTotalCredit(100);
        credit.setCreditLevel("NEWBIE");
        userCreditMapper.insert(credit);

        // 分配角色（每个用户只有一个角色）
        String requestedRole = request.getRole();
        if (requestedRole == null || requestedRole.isEmpty()) {
            requestedRole = "STUDENT";
        }
        
        if (!"STUDENT".equals(requestedRole) && !"MENTOR".equals(requestedRole)) {
            throw new RuntimeException("只能创建学生或导师账号");
        }
        
        // 删除该用户的所有现有角色（确保只有一个角色）
        LambdaQueryWrapper<UserRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(UserRole::getUserId, user.getId());
        userRoleMapper.delete(roleWrapper);
        
        // 插入新角色
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleName(requestedRole);
        userRole.setGrantedAt(LocalDateTime.now());
        userRoleMapper.insert(userRole);

        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User updateUserByAdmin(Long id, com.teamup.server.modules.user.dto.UserUpdateRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查是否是平台管理员
        List<String> currentRoles = userRoleMapper.getUserRoles(id);
        boolean isPlatformAdmin = currentRoles != null && currentRoles.contains("PLATFORM_ADMIN");
        
        // 不允许编辑平台管理员
        if (isPlatformAdmin) {
            throw new RuntimeException("不能编辑平台管理员账号");
        }

        // 更新基本信息
        if (request.getUserCode() != null && !request.getUserCode().equals(user.getUserCode())) {
            // 检查新学号是否已存在
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUserCode, request.getUserCode());
            wrapper.ne(User::getId, id);
            if (userMapper.selectCount(wrapper) > 0) {
                throw new RuntimeException("学号/工号已存在");
            }
            user.setUserCode(request.getUserCode());
        }
        
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        // 更新角色（每个用户只有一个角色）
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            // 删除所有现有角色
            LambdaQueryWrapper<UserRole> roleWrapper = new LambdaQueryWrapper<>();
            roleWrapper.eq(UserRole::getUserId, id);
            userRoleMapper.delete(roleWrapper);
            
            // 添加新角色（只添加一个角色）
            if ("STUDENT".equals(request.getRole()) || "MENTOR".equals(request.getRole())) {
                UserRole userRole = new UserRole();
                userRole.setUserId(id);
                userRole.setRoleName(request.getRole());
                userRole.setGrantedAt(LocalDateTime.now());
                userRoleMapper.insert(userRole);
            } else {
                throw new RuntimeException("只能设置为学生或导师角色");
            }
        }

        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserByAdmin(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 检查是否是平台管理员
        List<String> roles = userRoleMapper.getUserRoles(id);
        if (roles != null && roles.contains("PLATFORM_ADMIN")) {
            throw new RuntimeException("不能删除平台管理员账号");
        }
        
        // 特别保护：不能删除用户名为"系统管理员"的账号
        if ("系统管理员".equals(user.getUsername())) {
            throw new RuntimeException("不能删除系统管理员账号");
        }
        
        // 删除用户相关数据
        LambdaQueryWrapper<UserRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(UserRole::getUserId, id);
        userRoleMapper.delete(roleWrapper);
        
        // 注意：实际项目中可能需要软删除或保留数据
        // 这里使用硬删除，生产环境建议改为软删除
        userMapper.deleteById(id);
    }

    @Override
    public java.util.List<String> getUserRoles(Long userId) {
        return userRoleMapper.getUserRoles(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void banUser(Long userId, Integer days, String reason) {
        log.info("封禁用户: userId={}, days={}, reason={}", userId, days, reason);
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 设置封禁状态
        user.setStatus("BANNED");
        user.setBanUntil(LocalDateTime.now().plusDays(days));
        user.setBanReason(reason);
        
        userMapper.updateById(user);
        log.info("用户封禁成功: userId={}, banUntil={}", userId, user.getBanUntil());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbanUser(Long userId) {
        log.info("解封用户: userId={}", userId);
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 清除封禁状态
        user.setStatus("ACTIVE");
        user.setBanUntil(null);
        user.setBanReason(null);
        
        userMapper.updateById(user);
        log.info("用户解封成功: userId={}", userId);
    }
    
    @Override
    public boolean isUserBanned(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        
        // 检查是否被封禁
        if (!"BANNED".equals(user.getStatus())) {
            return false;
        }
        
        // 检查封禁是否已过期
        if (user.getBanUntil() != null && user.getBanUntil().isBefore(LocalDateTime.now())) {
            // 封禁已过期，自动解封
            unbanUser(userId);
            return false;
        }
        
        return true;
    }
}
