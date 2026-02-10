package com.teamup.server.modules.user.config;

import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.entity.UserRole;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.mapper.UserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 系统初始化：创建默认管理员账号
 * 如果默认管理员账号不存在，会自动创建
 */
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    // 默认管理员账号信息
    private static final String DEFAULT_ADMIN_STUDENT_ID = "admin";
    private static final String DEFAULT_ADMIN_USERNAME = "系统管理员";
    private static final String DEFAULT_ADMIN_EMAIL = "admin@teamup.edu.cn";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123456"; // 默认密码，首次登录后应修改

    @Override
    public void run(String... args) {
        try {
            // 检查默认管理员是否存在
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getStudentId, DEFAULT_ADMIN_STUDENT_ID);
            User admin = userMapper.selectOne(wrapper);

            if (admin == null) {
                // 创建默认管理员账号
                admin = new User();
                admin.setStudentId(DEFAULT_ADMIN_STUDENT_ID);
                admin.setUsername(DEFAULT_ADMIN_USERNAME);
                admin.setPassword(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
                admin.setEmail(DEFAULT_ADMIN_EMAIL);
                admin.setStatus("ACTIVE");
                admin.setCreatedAt(LocalDateTime.now());
                admin.setUpdatedAt(LocalDateTime.now());
                userMapper.insert(admin);

                // 添加平台管理员角色（每个用户只有一个角色）
                UserRole role = new UserRole();
                role.setUserId(admin.getId());
                role.setRoleName("PLATFORM_ADMIN");
                role.setGrantedAt(LocalDateTime.now());
                userRoleMapper.insert(role);

                System.out.println("========================================");
                System.out.println("✅ 默认管理员账号已创建");
                System.out.println("学号: " + DEFAULT_ADMIN_STUDENT_ID);
                System.out.println("密码: " + DEFAULT_ADMIN_PASSWORD);
                System.out.println("用户ID: " + admin.getId());
                System.out.println("⚠️  请首次登录后立即修改密码！");
                System.out.println("========================================");
            } else {
                System.out.println("========================================");
                System.out.println("ℹ️  默认管理员账号已存在");
                System.out.println("学号: " + DEFAULT_ADMIN_STUDENT_ID);
                System.out.println("用户ID: " + admin.getId());
                System.out.println("========================================");
                
                // 检查是否已有管理员角色
                LambdaQueryWrapper<UserRole> roleWrapper = new LambdaQueryWrapper<>();
                roleWrapper.eq(UserRole::getUserId, admin.getId())
                          .eq(UserRole::getRoleName, "PLATFORM_ADMIN");
                if (userRoleMapper.selectCount(roleWrapper) == 0) {
                    // 如果没有管理员角色，添加
                    UserRole role = new UserRole();
                    role.setUserId(admin.getId());
                    role.setRoleName("PLATFORM_ADMIN");
                    role.setGrantedAt(LocalDateTime.now());
                    userRoleMapper.insert(role);
                    System.out.println("✅ 已为默认管理员账号添加管理员角色");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ 初始化默认管理员账号失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
