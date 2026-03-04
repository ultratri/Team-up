package com.teamup.server.modules.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.common.api.Result;
import com.teamup.server.common.utils.SecurityUtils;
import com.teamup.server.modules.newbie.entity.SkillCertification;
import com.teamup.server.modules.newbie.mapper.SkillCertificationMapper;
import com.teamup.server.modules.newbie.vo.SkillCertificationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 技能认证控制器
 */
@RestController
@RequestMapping("/skill-certifications")
@RequiredArgsConstructor
@Slf4j
public class SkillCertificationController {
    
    private final SkillCertificationMapper skillCertificationMapper;
    
    /**
     * 申请技能认证
     */
    @PostMapping
    public Result<Void> applyCertification(@RequestBody Map<String, Object> request) {
        Long userId = SecurityUtils.getUserId();
        
        String skillName = (String) request.get("skillName");
        String proficiencyLevel = (String) request.get("proficiencyLevel");
        String certificationType = (String) request.get("certificationType");
        String proofUrl = (String) request.get("proofUrl");
        String proofDescription = (String) request.get("proofDescription");
        
        SkillCertification cert = new SkillCertification();
        cert.setUserId(userId);
        cert.setSkillName(skillName);
        cert.setProficiencyLevel(proficiencyLevel);
        cert.setCertificationType(certificationType);
        cert.setProofUrl(proofUrl);
        cert.setProofDescription(proofDescription);
        cert.setStatus("PENDING");
        cert.setCreatedAt(LocalDateTime.now());
        cert.setUpdatedAt(LocalDateTime.now());
        
        skillCertificationMapper.insert(cert);
        
        log.info("用户申请技能认证: userId={}, skillName={}, type={}", userId, skillName, certificationType);
        
        return Result.success(null, "认证申请已提交,请等待审核");
    }
    
    /**
     * 获取我的认证申请列表
     */
    @GetMapping("/my")
    public Result<List<SkillCertificationVO>> getMyCertifications() {
        try {
            Long userId = SecurityUtils.getUserId();
            log.info("获取用户 {} 的技能认证列表", userId);
            
            List<SkillCertification> certifications = skillCertificationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SkillCertification>()
                    .eq(SkillCertification::getUserId, userId)
                    .orderByDesc(SkillCertification::getCreatedAt)
            );
            
            List<SkillCertificationVO> voList = certifications.stream().map(cert -> {
                SkillCertificationVO vo = new SkillCertificationVO();
                BeanUtils.copyProperties(cert, vo);
                return vo;
            }).collect(Collectors.toList());
            
            log.info("成功获取 {} 条认证记录", voList.size());
            return Result.success(voList);
        } catch (Exception e) {
            log.error("获取技能认证列表失败", e);
            return Result.error(500, "获取认证列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 取消认证申请(仅限PENDING状态)
     */
    @DeleteMapping("/{id}")
    public Result<Void> cancelCertification(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        
        SkillCertification cert = skillCertificationMapper.selectById(id);
        if (cert == null) {
            return Result.error(404, "认证申请不存在");
        }
        
        if (!cert.getUserId().equals(userId)) {
            return Result.error(403, "无权操作此认证申请");
        }
        
        if (!"PENDING".equals(cert.getStatus())) {
            return Result.error(400, "只能取消待审核的申请");
        }
        
        skillCertificationMapper.deleteById(id);
        
        log.info("用户取消技能认证申请: userId={}, certId={}", userId, id);
        
        return Result.success(null, "已取消认证申请");
    }
}
