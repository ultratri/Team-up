package com.teamup.server.modules.user.controller;

import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.service.UserService;
import com.teamup.server.modules.user.mapper.UserRoleMapper;
import com.teamup.server.modules.user.vo.UserVO;
import com.teamup.server.common.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRoleMapper userRoleMapper;

    /**
     * 获取用户信息
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        return Result.success(user);
    }

    /**
     * 根据学号获取用户
     */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("isAuthenticated()")
    public Result<User> getUserByStudentId(@PathVariable String studentId) {
        User user = userService.getUserByStudentId(studentId);
        return Result.success(user);
    }

    /**
     * 搜索用户（按用户名、学号、邮箱）
     */
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public Result<java.util.List<User>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "10") int limit
    ) {
        java.util.List<User> users = userService.searchUsers(keyword, limit);
        return Result.success(users);
    }

    /**
     * 获取用户列表（分页，管理员）
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserVO>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sortBy
    ) {
        try {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> pageParam = 
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> result = 
                userService.getUserList(pageParam, keyword, role, status, sortBy);
            
            // 转换为 UserVO 并填充角色信息
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserVO> voPage = 
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
            voPage.setTotal(result.getTotal());
            voPage.setCurrent(result.getCurrent());
            voPage.setSize(result.getSize());
            
            java.util.List<UserVO> voList = new ArrayList<>();
            for (User user : result.getRecords()) {
                UserVO vo = new UserVO();
                BeanUtils.copyProperties(user, vo);
                // 获取用户角色
                try {
                    java.util.List<String> roles = userRoleMapper.getUserRoles(user.getId());
                    vo.setRoles(roles != null ? roles : new ArrayList<>());
                } catch (Exception e) {
                    // 如果获取角色失败，设置为空列表
                    vo.setRoles(new ArrayList<>());
                }
                voList.add(vo);
            }
            voPage.setRecords(voList);
            
            return Result.success(voPage);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "获取用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 创建用户（管理员）
     */
    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public Result<User> createUser(@RequestBody com.teamup.server.modules.user.dto.RegisterRequest request) {
        User user = userService.createUserByAdmin(request);
        return Result.success(user);
    }

    /**
     * 更新用户（管理员）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public Result<User> updateUser(
            @PathVariable Long id,
            @RequestBody com.teamup.server.modules.user.dto.UserUpdateRequest request
    ) {
        User user = userService.updateUserByAdmin(id, request);
        return Result.success(user);
    }

    /**
     * 删除用户（管理员）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUserByAdmin(id);
        return Result.success(null);
    }
}

