package com.teamup.server.modules.user.service;

import com.teamup.server.modules.user.dto.LoginRequest;
import com.teamup.server.modules.user.dto.RegisterRequest;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.vo.LoginResponse;

/**
 * 用户服务接口
 */
public interface UserService {
    /**
     * 用户注册
     * @return 登录响应（用于 need-confirm 后完成自动登录）
     */
    LoginResponse register(RegisterRequest request);

    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);

    /**
     * 根据ID获取用户
     */
    User getUserById(Long id);

    /**
     * 根据学号获取用户
     */
    User getUserByStudentId(String studentId);

    /**
     * 搜索用户（按用户名、学号、邮箱）
     */
    java.util.List<User> searchUsers(String keyword, int limit);

    /**
     * 获取用户列表（分页，管理员）
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> getUserList(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> page,
            String keyword,
            String role,
            String status,
            String sortBy
    );

    /**
     * 管理员创建用户
     */
    User createUserByAdmin(RegisterRequest request);

    /**
     * 管理员更新用户
     */
    User updateUserByAdmin(Long id, com.teamup.server.modules.user.dto.UserUpdateRequest request);

    /**
     * 管理员删除用户
     */
    void deleteUserByAdmin(Long id);

    /**
     * 获取用户角色列表
     */
    java.util.List<String> getUserRoles(Long userId);
}

