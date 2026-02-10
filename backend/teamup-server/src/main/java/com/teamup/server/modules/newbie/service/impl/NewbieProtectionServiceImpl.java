package com.teamup.server.modules.newbie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.user.security.UserContext;
import com.teamup.server.modules.newbie.entity.NewbieConfig;
import com.teamup.server.modules.newbie.entity.NewbieTask;
import com.teamup.server.modules.newbie.entity.SkillCertification;
import com.teamup.server.modules.newbie.mapper.NewbieConfigMapper;
import com.teamup.server.modules.newbie.mapper.NewbieTaskMapper;
import com.teamup.server.modules.newbie.mapper.SkillCertificationMapper;
import com.teamup.server.modules.newbie.service.NewbieProtectionService;
import com.teamup.server.modules.newbie.vo.SkillCertificationVO;
import com.teamup.server.modules.notification.service.NotificationService;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 新手保护服务实现
 */
@Service
@RequiredArgsConstructor
public class NewbieProtectionServiceImpl implements NewbieProtectionService {
    
    private final NewbieConfigMapper newbieConfigMapper;
    private final NewbieTaskMapper newbieTaskMapper;
    private final SkillCertificationMapper skillCertificationMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    
    @Override
    public NewbieConfig getConfig() {
        // 获取第一条配置记录（系统只有一条配置）
        List<NewbieConfig> configs = newbieConfigMapper.selectList(null);
        if (configs.isEmpty()) {
            throw new BusinessException("新手保护配置不存在");
        }
        return configs.get(0);
    }
    
    @Override
    @Transactional
    public void updateConfig(NewbieConfig config) {
        Long currentUserId = UserContext.getCurrentUserId();
        config.setUpdatedBy(currentUserId);
        config.setUpdatedAt(LocalDateTime.now());
        
        // 更新配置
        newbieConfigMapper.updateById(config);
    }
    
    @Override
    public List<NewbieTask> getTaskList() {
        // 管理员需要看到所有任务（包括未激活的）
        LambdaQueryWrapper<NewbieTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(NewbieTask::getDisplayOrder);
        return newbieTaskMapper.selectList(wrapper);
    }
    
    @Override
    @Transactional
    public void updateTask(NewbieTask task) {
        task.setUpdatedAt(LocalDateTime.now());
        newbieTaskMapper.updateById(task);
    }
    
    @Override
    public Page<SkillCertificationVO> getPendingCertifications(int page, int size) {
        // 查询待审核的技能认证
        LambdaQueryWrapper<SkillCertification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillCertification::getStatus, "PENDING")
               .orderByAsc(SkillCertification::getCreatedAt);
        
        Page<SkillCertification> certPage = skillCertificationMapper.selectPage(
            new Page<>(page, size), wrapper
        );
        
        // 转换为VO
        Page<SkillCertificationVO> voPage = new Page<>(page, size);
        voPage.setTotal(certPage.getTotal());
        
        List<SkillCertificationVO> voList = certPage.getRecords().stream().map(cert -> {
            SkillCertificationVO vo = new SkillCertificationVO();
            BeanUtils.copyProperties(cert, vo);
            
            // 获取用户信息
            User user = userMapper.selectById(cert.getUserId());
            if (user != null) {
                vo.setUserName(user.getUsername());
                // 这里可以添加头像URL，如果有的话
            }
            
            return vo;
        }).collect(Collectors.toList());
        
        voPage.setRecords(voList);
        return voPage;
    }
    
    @Override
    @Transactional
    public void approveCertification(Long certificationId) {
        SkillCertification cert = skillCertificationMapper.selectById(certificationId);
        if (cert == null) {
            throw new BusinessException("技能认证不存在");
        }
        
        if (!"PENDING".equals(cert.getStatus())) {
            throw new BusinessException("该技能认证已被审核");
        }
        
        // 更新认证状态
        cert.setStatus("APPROVED");
        cert.setCertificationType("OFFICIAL");  // 通过审核后升级为官方认证
        cert.setVerifiedBy(UserContext.getCurrentUserId());
        cert.setVerifiedAt(LocalDateTime.now());
        
        skillCertificationMapper.updateById(cert);

        // 发送通知给用户
        notificationService.createNotification(
            cert.getUserId(),
            "SYSTEM",
            "技能认证已通过",
            "恭喜！您的技能「" + cert.getSkillName() + "」已通过官方认证。",
            null,
            null
        );
    }

    @Override
    @Transactional
    public void rejectCertification(Long certificationId, String reason) {
        SkillCertification cert = skillCertificationMapper.selectById(certificationId);
        if (cert == null) {
            throw new BusinessException("技能认证不存在");
        }
        
        if (!"PENDING".equals(cert.getStatus())) {
            throw new BusinessException("该技能认证已被审核");
        }
        
        // 更新认证状态
        cert.setStatus("REJECTED");
        cert.setVerifiedBy(UserContext.getCurrentUserId());
        cert.setVerifiedAt(LocalDateTime.now());
        cert.setRejectReason(reason);
        
        skillCertificationMapper.updateById(cert);

        // 发送通知给用户
        notificationService.createNotification(
            cert.getUserId(),
            "SYSTEM",
            "技能认证未通过",
            "很抱歉，您的技能「" + cert.getSkillName() + "」认证未通过。原因：" + reason,
            null,
            null
        );
    }
}
