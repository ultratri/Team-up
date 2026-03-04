package com.teamup.server.modules.mentor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.common.utils.SecurityUtils;
import com.teamup.server.modules.mentor.dto.MentorReviewDTO;
import com.teamup.server.modules.mentor.entity.MentorReview;
import com.teamup.server.modules.mentor.mapper.MentorReviewMapper;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.entity.TeamMember;
import com.teamup.server.modules.team.mapper.TeamMapper;
import com.teamup.server.modules.team.mapper.TeamMemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 学员评价导师服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MentorReviewService {
    
    private final MentorReviewMapper reviewMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final TeamMapper teamMapper;
    private final MentorRatingService ratingService;
    
    /**
     * 提交评价
     * 
     * @param dto 评价DTO
     * @return 评价ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long submitReview(MentorReviewDTO dto) {
        Long studentId = SecurityUtils.getUserId();
        
        // 1. 验证学员是否在该团队中
        LambdaQueryWrapper<TeamMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(TeamMember::getTeamId, dto.getTeamId())
                    .eq(TeamMember::getUserId, studentId);
        
        TeamMember member = teamMemberMapper.selectOne(memberWrapper);
        if (member == null) {
            throw new RuntimeException("您不是该团队成员，无法评价");
        }
        
        // 2. 验证导师是否是该团队的导师（通过teams表的mentor_id字段）
        Team team = teamMapper.selectById(dto.getTeamId());
        if (team == null) {
            throw new RuntimeException("团队不存在");
        }
        
        if (!dto.getMentorId().equals(team.getMentorId())) {
            throw new RuntimeException("该用户不是团队导师，无法评价");
        }
        
        // 3. 检查是否已经评价过
        LambdaQueryWrapper<MentorReview> reviewWrapper = new LambdaQueryWrapper<>();
        reviewWrapper.eq(MentorReview::getStudentId, studentId)
                    .eq(MentorReview::getMentorId, dto.getMentorId())
                    .eq(MentorReview::getTeamId, dto.getTeamId())
                    .eq(MentorReview::getStatus, "ACTIVE");
        
        MentorReview existingReview = reviewMapper.selectOne(reviewWrapper);
        if (existingReview != null) {
            throw new RuntimeException("您已经评价过该导师，无法重复评价");
        }
        
        // 4. 计算综合评分
        BigDecimal overallRating = calculateOverallRating(
            dto.getProfessionalAbility(),
            dto.getGuidanceAttitude(),
            dto.getResponseSpeed(),
            dto.getHelpfulness()
        );
        
        // 5. 创建评价记录
        MentorReview review = new MentorReview();
        review.setMentorId(dto.getMentorId());
        review.setStudentId(studentId);
        review.setTeamId(dto.getTeamId());
        review.setProfessionalAbility(dto.getProfessionalAbility());
        review.setGuidanceAttitude(dto.getGuidanceAttitude());
        review.setResponseSpeed(dto.getResponseSpeed());
        review.setHelpfulness(dto.getHelpfulness());
        review.setOverallRating(overallRating);
        review.setComment(dto.getComment());
        review.setStatus("ACTIVE");
        
        reviewMapper.insert(review);
        
        log.info("学员 {} 评价导师 {} 成功，评分: {}", studentId, dto.getMentorId(), overallRating);
        
        // 6. 触发导师评分更新
        try {
            ratingService.updateMentorRating(dto.getMentorId());
            log.info("导师 {} 的评分已更新", dto.getMentorId());
        } catch (Exception e) {
            log.error("更新导师评分失败", e);
            // 不影响评价提交
        }
        
        return review.getId();
    }
    
    /**
     * 计算综合评分（四个维度的平均值）
     */
    private BigDecimal calculateOverallRating(
            Integer professionalAbility,
            Integer guidanceAttitude,
            Integer responseSpeed,
            Integer helpfulness) {
        
        double avg = (professionalAbility + guidanceAttitude + responseSpeed + helpfulness) / 4.0;
        return BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 获取导师的所有评价
     * 
     * @param mentorId 导师ID
     * @return 评价列表
     */
    public List<MentorReview> getMentorReviews(Long mentorId) {
        LambdaQueryWrapper<MentorReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MentorReview::getMentorId, mentorId)
               .eq(MentorReview::getStatus, "ACTIVE")
               .orderByDesc(MentorReview::getCreatedAt);
        
        return reviewMapper.selectList(wrapper);
    }
    
    /**
     * 获取导师的平均评分
     * 
     * @param mentorId 导师ID
     * @return 平均评分
     */
    public BigDecimal getMentorAverageRating(Long mentorId) {
        BigDecimal avgRating = reviewMapper.getAverageRating(mentorId);
        return avgRating != null ? avgRating : BigDecimal.ZERO;
    }
    
    /**
     * 获取导师的评价数量
     * 
     * @param mentorId 导师ID
     * @return 评价数量
     */
    public Integer getMentorReviewCount(Long mentorId) {
        Integer count = reviewMapper.getReviewCount(mentorId);
        return count != null ? count : 0;
    }
}
