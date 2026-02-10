package com.teamup.server.modules.ecosystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.ecosystem.dto.MomentCreateDTO;
import com.teamup.server.modules.ecosystem.entity.Like;
import com.teamup.server.modules.ecosystem.entity.Moment;
import com.teamup.server.modules.ecosystem.mapper.LikeMapper;
import com.teamup.server.modules.ecosystem.mapper.MomentMapper;
import com.teamup.server.modules.ecosystem.service.MomentService;
import com.teamup.server.modules.ecosystem.vo.MomentVO;
import com.teamup.server.modules.project.entity.Project;
import com.teamup.server.modules.project.mapper.ProjectMapper;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.service.UserService;
import com.teamup.server.modules.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 动态服务实现类
 */
@Service
@RequiredArgsConstructor
public class MomentServiceImpl implements MomentService {
    
    private final MomentMapper momentMapper;
    private final LikeMapper likeMapper;
    private final UserService userService;
    private final ProfileService profileService;
    private final ProjectMapper projectMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMoment(MomentCreateDTO dto, Long userId) {
        Moment moment = new Moment();
        moment.setUserId(userId);
        moment.setType(dto.getType());
        moment.setContent(dto.getContent());
        moment.setRelatedProjectId(dto.getRelatedProjectId());
        moment.setLikes(0);
        moment.setComments(0);
        
        momentMapper.insert(moment);
        return moment.getId();
    }
    
    @Override
    public Page<MomentVO> getMomentList(Integer page, Integer size, String type, Long userId) {
        Page<Moment> momentPage = new Page<>(page, size);
        
        LambdaQueryWrapper<Moment> wrapper = new LambdaQueryWrapper<>();
        
        // 类型筛选
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Moment::getType, type);
        }
        
        // 按时间倒序
        wrapper.orderByDesc(Moment::getCreatedAt);
        
        momentMapper.selectPage(momentPage, wrapper);
        
        // 转换为VO
        Page<MomentVO> voPage = new Page<>(page, size, momentPage.getTotal());
        List<MomentVO> voList = momentPage.getRecords().stream()
                .map(moment -> convertToVO(moment, userId))
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return voPage;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeMoment(Long id, Long userId) {
        Moment moment = momentMapper.selectById(id);
        if (moment == null) {
            throw new BusinessException("动态不存在");
        }
        
        // 检查是否已点赞
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getUserId, userId)
                .eq(Like::getTargetType, "MOMENT")
                .eq(Like::getTargetId, id);
        
        if (likeMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("已经点赞过了");
        }
        
        // 创建点赞记录
        Like like = new Like();
        like.setUserId(userId);
        like.setTargetType("MOMENT");
        like.setTargetId(id);
        likeMapper.insert(like);
        
        // 增加点赞数
        momentMapper.incrementLikes(id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeMoment(Long id, Long userId) {
        // 删除点赞记录
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getUserId, userId)
                .eq(Like::getTargetType, "MOMENT")
                .eq(Like::getTargetId, id);
        
        likeMapper.delete(wrapper);
        
        // 减少点赞数
        momentMapper.decrementLikes(id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMoment(Long id, Long userId) {
        Moment moment = momentMapper.selectById(id);
        if (moment == null) {
            throw new BusinessException("动态不存在");
        }
        
        if (!moment.getUserId().equals(userId)) {
            throw new BusinessException("只能删除自己的动态");
        }
        
        momentMapper.deleteById(id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoCreateProjectMoment(String type, Long userId, Long projectId, String content) {
        Moment moment = new Moment();
        moment.setUserId(userId);
        moment.setType(type);
        moment.setContent(content);
        moment.setRelatedProjectId(projectId);
        moment.setLikes(0);
        moment.setComments(0);
        
        momentMapper.insert(moment);
    }
    
    /**
     * 转换为VO
     */
    private MomentVO convertToVO(Moment moment, Long userId) {
        MomentVO vo = new MomentVO();
        vo.setId(moment.getId());
        vo.setType(moment.getType());
        vo.setContent(moment.getContent());
        vo.setLikes(moment.getLikes());
        vo.setComments(moment.getComments());
        vo.setCreatedAt(moment.getCreatedAt());
        
        // 用户信息
        User user = userService.getUserById(moment.getUserId());
        if (user != null) {
            UserProfile profile = profileService.getProfileByUserId(user.getId());
            MomentVO.UserInfo userInfo = new MomentVO.UserInfo();
            userInfo.setId(user.getId());
            userInfo.setRealName(profile != null ? profile.getRealName() : user.getUsername());
            userInfo.setAvatar(profile != null ? profile.getAvatarUrl() : null);
            vo.setUser(userInfo);
        }
        
        // 相关项目信息
        if (moment.getRelatedProjectId() != null) {
            Project project = projectMapper.selectById(moment.getRelatedProjectId());
            if (project != null) {
                MomentVO.RelatedProject relatedProject = new MomentVO.RelatedProject();
                relatedProject.setId(project.getId());
                relatedProject.setTitle(project.getTitle());
                vo.setRelatedProject(relatedProject);
            }
        }
        
        // 是否已点赞
        if (userId != null) {
            LambdaQueryWrapper<Like> likeWrapper = new LambdaQueryWrapper<>();
            likeWrapper.eq(Like::getUserId, userId)
                    .eq(Like::getTargetType, "MOMENT")
                    .eq(Like::getTargetId, moment.getId());
            vo.setLiked(likeMapper.selectCount(likeWrapper) > 0);
        } else {
            vo.setLiked(false);
        }
        
        return vo;
    }
}
