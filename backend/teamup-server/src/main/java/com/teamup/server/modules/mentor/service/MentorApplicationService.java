package com.teamup.server.modules.mentor.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.mentor.dto.MentorApplicationRequest;
import com.teamup.server.modules.mentor.dto.ReviewApplicationRequest;
import com.teamup.server.modules.mentor.entity.MentorApplication;

/**
 * 导师申请服务接口
 */
public interface MentorApplicationService {
    
    /**
     * 提交导师申请
     * 
     * @param userId 用户ID
     * @param request 申请信息
     * @return 申请记录
     */
    MentorApplication submitApplication(Long userId, MentorApplicationRequest request);
    
    /**
     * 审核导师申请
     * 
     * @param applicationId 申请ID
     * @param reviewerId 审核人ID
     * @param request 审核信息
     */
    void reviewApplication(Long applicationId, Long reviewerId, ReviewApplicationRequest request);
    
    /**
     * 获取申请列表（管理员）
     * 
     * @param page 页码
     * @param size 每页大小
     * @param status 状态筛选
     * @return 申请列表
     */
    Page<MentorApplication> getApplicationList(Integer page, Integer size, String status);
    
    /**
     * 获取用户的申请记录
     * 
     * @param userId 用户ID
     * @return 申请记录
     */
    MentorApplication getUserApplication(Long userId);
    
    /**
     * 检查用户是否可以申请导师
     * 
     * @param userId 用户ID
     * @return 是否可以申请
     */
    boolean canApply(Long userId);
}
