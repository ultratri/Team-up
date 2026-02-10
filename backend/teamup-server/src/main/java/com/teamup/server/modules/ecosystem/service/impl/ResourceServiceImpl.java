package com.teamup.server.modules.ecosystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.ecosystem.dto.ResourceCreateDTO;
import com.teamup.server.modules.ecosystem.entity.Like;
import com.teamup.server.modules.ecosystem.entity.Resource;
import com.teamup.server.modules.ecosystem.entity.ResourceTag;
import com.teamup.server.modules.ecosystem.mapper.LikeMapper;
import com.teamup.server.modules.ecosystem.mapper.ResourceMapper;
import com.teamup.server.modules.ecosystem.mapper.ResourceTagMapper;
import com.teamup.server.modules.ecosystem.service.ResourceService;
import com.teamup.server.modules.ecosystem.vo.ResourceVO;
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
 * 资源服务实现类
 */
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {
    
    private final ResourceMapper resourceMapper;
    private final ResourceTagMapper resourceTagMapper;
    private final LikeMapper likeMapper;
    private final UserService userService;
    private final ProfileService profileService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createResource(ResourceCreateDTO dto, Long userId) {
        // 创建资源
        Resource resource = new Resource();
        resource.setTitle(dto.getTitle());
        resource.setDescription(dto.getDescription());
        resource.setType(dto.getType());
        resource.setCover(dto.getCover());
        resource.setContent(dto.getContent());
        resource.setAuthorId(userId);
        resource.setProjectId(dto.getProjectId());
        resource.setViews(0);
        resource.setLikes(0);
        
        resourceMapper.insert(resource);
        
        // 保存标签
        if (dto.getTags() != null && !dto.getTags().isEmpty()) {
            for (String tagName : dto.getTags()) {
                ResourceTag tag = new ResourceTag();
                tag.setResourceId(resource.getId());
                tag.setTagName(tagName);
                resourceTagMapper.insert(tag);
            }
        }
        
        return resource.getId();
    }
    
    @Override
    public Page<ResourceVO> getResourceList(Integer page, Integer size, String type, String sortBy, Long userId) {
        Page<Resource> resourcePage = new Page<>(page, size);
        
        LambdaQueryWrapper<Resource> wrapper = new LambdaQueryWrapper<>();
        
        // 类型筛选
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Resource::getType, type);
        }
        
        // 排序
        if ("views".equals(sortBy)) {
            wrapper.orderByDesc(Resource::getViews);
        } else if ("likes".equals(sortBy)) {
            wrapper.orderByDesc(Resource::getLikes);
        } else {
            wrapper.orderByDesc(Resource::getCreatedAt);
        }
        
        resourceMapper.selectPage(resourcePage, wrapper);
        
        // 转换为VO
        Page<ResourceVO> voPage = new Page<>(page, size, resourcePage.getTotal());
        List<ResourceVO> voList = resourcePage.getRecords().stream()
                .map(resource -> convertToVO(resource, userId))
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return voPage;
    }
    
    @Override
    public ResourceVO getResourceDetail(Long id, Long userId) {
        Resource resource = resourceMapper.selectById(id);
        if (resource == null) {
            throw new BusinessException("资源不存在");
        }
        
        // 增加浏览量
        resourceMapper.incrementViews(id);
        
        return convertToVO(resource, userId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeResource(Long id, Long userId) {
        Resource resource = resourceMapper.selectById(id);
        if (resource == null) {
            throw new BusinessException("资源不存在");
        }
        
        // 检查是否已点赞
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getUserId, userId)
                .eq(Like::getTargetType, "RESOURCE")
                .eq(Like::getTargetId, id);
        
        if (likeMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("已经点赞过了");
        }
        
        // 创建点赞记录
        Like like = new Like();
        like.setUserId(userId);
        like.setTargetType("RESOURCE");
        like.setTargetId(id);
        likeMapper.insert(like);
        
        // 增加点赞数
        resourceMapper.incrementLikes(id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeResource(Long id, Long userId) {
        // 删除点赞记录
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getUserId, userId)
                .eq(Like::getTargetType, "RESOURCE")
                .eq(Like::getTargetId, id);
        
        likeMapper.delete(wrapper);
        
        // 减少点赞数
        resourceMapper.decrementLikes(id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteResource(Long id, Long userId) {
        Resource resource = resourceMapper.selectById(id);
        if (resource == null) {
            throw new BusinessException("资源不存在");
        }
        
        if (!resource.getAuthorId().equals(userId)) {
            throw new BusinessException("只能删除自己的资源");
        }
        
        resourceMapper.deleteById(id);
    }
    
    /**
     * 转换为VO
     */
    private ResourceVO convertToVO(Resource resource, Long userId) {
        ResourceVO vo = new ResourceVO();
        vo.setId(resource.getId());
        vo.setTitle(resource.getTitle());
        vo.setDescription(resource.getDescription());
        vo.setType(resource.getType());
        vo.setCover(resource.getCover());
        vo.setContent(resource.getContent());
        vo.setViews(resource.getViews());
        vo.setLikes(resource.getLikes());
        vo.setCreatedAt(resource.getCreatedAt());
        
        // 作者信息
        User author = userService.getUserById(resource.getAuthorId());
        if (author != null) {
            UserProfile profile = profileService.getProfileByUserId(author.getId());
            ResourceVO.AuthorInfo authorInfo = new ResourceVO.AuthorInfo();
            authorInfo.setId(author.getId());
            authorInfo.setRealName(profile != null ? profile.getRealName() : author.getUsername());
            authorInfo.setAvatar(profile != null ? profile.getAvatarUrl() : null);
            vo.setAuthor(authorInfo);
        }
        
        // 标签列表
        LambdaQueryWrapper<ResourceTag> tagWrapper = new LambdaQueryWrapper<>();
        tagWrapper.eq(ResourceTag::getResourceId, resource.getId());
        List<String> tags = resourceTagMapper.selectList(tagWrapper).stream()
                .map(ResourceTag::getTagName)
                .collect(Collectors.toList());
        vo.setTags(tags);
        
        // 是否已点赞
        if (userId != null) {
            LambdaQueryWrapper<Like> likeWrapper = new LambdaQueryWrapper<>();
            likeWrapper.eq(Like::getUserId, userId)
                    .eq(Like::getTargetType, "RESOURCE")
                    .eq(Like::getTargetId, resource.getId());
            vo.setLiked(likeMapper.selectCount(likeWrapper) > 0);
        } else {
            vo.setLiked(false);
        }
        
        return vo;
    }
}
