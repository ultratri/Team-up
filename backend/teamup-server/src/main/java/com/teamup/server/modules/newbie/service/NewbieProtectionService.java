package com.teamup.server.modules.newbie.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.newbie.entity.NewbieConfig;
import com.teamup.server.modules.newbie.entity.NewbieTask;
import com.teamup.server.modules.newbie.vo.SkillCertificationVO;

import java.util.List;

/**
 * 新手保护服务接口
 */
public interface NewbieProtectionService {
    
    /**
     * 获取新手保护配置
     */
    NewbieConfig getConfig();
    
    /**
     * 更新新手保护配置
     */
    void updateConfig(NewbieConfig config);
    
    /**
     * 获取新手任务列表
     */
    List<NewbieTask> getTaskList();
    
    /**
     * 更新新手任务
     */
    void updateTask(NewbieTask task);
    
    /**
     * 获取待审核的技能认证列表
     */
    Page<SkillCertificationVO> getPendingCertifications(int page, int size);
    
    /**
     * 审核技能认证 - 通过
     */
    void approveCertification(Long certificationId);
    
    /**
     * 审核技能认证 - 拒绝
     */
    void rejectCertification(Long certificationId, String reason);
}
