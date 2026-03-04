package com.teamup.server.modules.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.report.dto.HandleReportDTO;
import com.teamup.server.modules.report.dto.SubmitReportDTO;
import com.teamup.server.modules.report.entity.Report;
import com.teamup.server.modules.report.mapper.ReportMapper;
import com.teamup.server.modules.report.service.ReportService;
import com.teamup.server.modules.report.vo.ReportDetailVO;
import com.teamup.server.modules.report.vo.ReportStatisticsVO;
import com.teamup.server.modules.user.service.CreditService;
import com.teamup.server.modules.user.service.UserService;
import com.teamup.server.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 举报服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report> implements ReportService {
    
    private final ReportMapper reportMapper;
    private final ObjectMapper objectMapper;
    private final CreditService creditService;
    private final UserService userService;
    private final NotificationService notificationService;
    // 注入各模块服务用于删除内容
    // 注意: 这些服务需要在使用时检查是否为null,因为可能存在循环依赖
    // 如果遇到循环依赖,可以使用@Lazy注解或通过ApplicationContext获取
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReport(Long reporterId, SubmitReportDTO dto) {
        log.info("用户 {} 提交举报: targetType={}, targetId={}", reporterId, dto.getTargetType(), dto.getTargetId());
        
        // 检查是否重复举报
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getReporterId, reporterId)
               .eq(Report::getTargetType, dto.getTargetType())
               .eq(Report::getTargetId, dto.getTargetId())
               .eq(Report::getStatus, Report.ReportStatus.PENDING);
        
        if (reportMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("您已经举报过该内容，请勿重复举报");
        }
        
        // 创建举报记录
        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(dto.getTargetType());
        report.setTargetId(dto.getTargetId());
        report.setReason(dto.getReason());
        report.setDescription(dto.getDescription());
        
        // 转换证据链接为JSON
        if (dto.getEvidenceUrls() != null && !dto.getEvidenceUrls().isEmpty()) {
            try {
                report.setEvidenceUrls(objectMapper.writeValueAsString(dto.getEvidenceUrls()));
            } catch (JsonProcessingException e) {
                log.error("转换证据链接失败", e);
            }
        }
        
        report.setStatus(Report.ReportStatus.PENDING);
        
        reportMapper.insert(report);
        log.info("举报提交成功: reportId={}", report.getId());
    }
    
    @Override
    public Page<ReportDetailVO> listReports(int page, int size, Report.ReportStatus status, Report.TargetType targetType) {
        // TODO: 实现分页查询，需要关联查询举报人、目标信息
        // 这里简化实现，实际应该使用自定义SQL
        Page<Report> reportPage = new Page<>(page, size);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        
        if (status != null) {
            wrapper.eq(Report::getStatus, status);
        }
        if (targetType != null) {
            wrapper.eq(Report::getTargetType, targetType);
        }
        
        wrapper.orderByDesc(Report::getCreatedAt);
        
        Page<Report> result = reportMapper.selectPage(reportPage, wrapper);
        
        // 转换为VO（简化版，实际应该查询关联信息）
        Page<ReportDetailVO> voPage = new Page<>(page, size, result.getTotal());
        // TODO: 转换逻辑
        
        return voPage;
    }
    
    @Override
    public ReportDetailVO getReportDetail(Long reportId) {
        ReportDetailVO detail = reportMapper.selectReportDetail(reportId);
        if (detail == null) {
            throw new BusinessException("举报记录不存在");
        }
        
        // 如果需要解析证据链接，从数据库重新查询Report实体
        if (detail.getEvidenceUrls() == null || detail.getEvidenceUrls().isEmpty()) {
            Report report = reportMapper.selectById(reportId);
            if (report != null && report.getEvidenceUrls() != null) {
                try {
                    List<String> urls = objectMapper.readValue(
                        report.getEvidenceUrls(), 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                    );
                    detail.setEvidenceUrls(urls);
                } catch (JsonProcessingException e) {
                    log.error("解析证据链接失败", e);
                }
            }
        }
        
        return detail;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleReport(Long handlerId, HandleReportDTO dto) {
        log.info("管理员 {} 处理举报: reportId={}, status={}", handlerId, dto.getReportId(), dto.getStatus());
        
        Report report = reportMapper.selectById(dto.getReportId());
        if (report == null) {
            throw new BusinessException("举报记录不存在");
        }
        
        if (report.getStatus() != Report.ReportStatus.PENDING && 
            report.getStatus() != Report.ReportStatus.REVIEWING) {
            throw new BusinessException("该举报已处理，无法重复处理");
        }
        
        // 更新举报状态
        report.setStatus(dto.getStatus());
        report.setHandlerId(handlerId);
        report.setHandleResult(dto.getHandleResult());
        report.setHandledAt(LocalDateTime.now());
        
        reportMapper.updateById(report);
        
        // 如果需要惩罚目标
        if (Boolean.TRUE.equals(dto.getPunishTarget())) {
            executePunishment(report, dto);
        }
        
        // 发送通知给举报人
        try {
            String title = "举报处理结果通知";
            String content = String.format("您提交的举报（ID: %d）已处理完成。\n\n处理结果：%s\n\n感谢您对平台建设的支持！", 
                                          report.getId(), dto.getHandleResult());
            
            notificationService.createNotification(
                report.getReporterId(),
                "REPORT",
                title,
                content,
                "REPORT",
                report.getId()
            );
        } catch (Exception e) {
            log.error("发送举报结果通知失败", e);
        }
        
        // 如果举报属实，通知被举报人
        if (dto.getStatus() == Report.ReportStatus.RESOLVED) {
            Long targetUserId = getTargetUserId(report);
            if (targetUserId != null) {
                try {
                    String title = "违规处理通知";
                    String content = String.format("您的%s因违规被举报，已被处理。原因：%s\n处理结果：%s", 
                                         getTargetTypeName(report.getTargetType()),
                                         getReasonName(report.getReason()),
                                         dto.getHandleResult());
                    
                    notificationService.createNotification(
                        targetUserId,
                        "SYSTEM",
                        title,
                        content,
                        report.getTargetType().name(),
                        report.getTargetId()
                    );
                } catch (Exception e) {
                    log.error("发送违规通知失败", e);
                }
            }
        }
        
        log.info("举报处理完成: reportId={}", dto.getReportId());
    }
    
    /**
     * 执行惩罚
     */
    private void executePunishment(Report report, HandleReportDTO dto) {
        log.info("执行惩罚: targetType={}, targetId={}, punishmentType={}", 
                 report.getTargetType(), report.getTargetId(), dto.getPunishmentType());
        
        try {
            switch (dto.getPunishmentType()) {
                case "BAN_USER":
                    banUser(report, dto);
                    break;
                case "DELETE_CONTENT":
                    deleteContent(report);
                    break;
                case "DEDUCT_CREDIT":
                    deductCredit(report);
                    break;
                default:
                    log.warn("未知的惩罚类型: {}", dto.getPunishmentType());
            }
        } catch (Exception e) {
            log.error("执行惩罚失败", e);
            throw new BusinessException("执行惩罚失败: " + e.getMessage());
        }
    }
    
    /**
     * 封禁用户
     */
    private void banUser(Report report, HandleReportDTO dto) {
        Long userId = getTargetUserId(report);
        if (userId == null) {
            log.error("无法获取目标用户ID");
            throw new BusinessException("无法获取目标用户ID");
        }
        
        // 调用UserService封禁用户
        userService.banUser(userId, dto.getPunishmentDays(), "举报违规：" + getReasonName(report.getReason()));
        
        LocalDateTime banUntil = LocalDateTime.now().plusDays(dto.getPunishmentDays());
        
        log.info("用户封禁成功: userId={}, days={}, until={}", userId, dto.getPunishmentDays(), banUntil);
        
        // 发送封禁通知
        try {
            String title = "账号封禁通知";
            String content = String.format("您的账号因违规被封禁%d天。\n违规原因：%s\n解封时间：%s\n\n如有异议，请联系管理员。", 
                             dto.getPunishmentDays(),
                             getReasonName(report.getReason()),
                             banUntil.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            
            notificationService.createNotification(
                userId,
                "SYSTEM",
                title,
                content,
                "USER",
                userId
            );
        } catch (Exception e) {
            log.error("发送封禁通知失败", e);
        }
    }
    
    /**
     * 删除内容
     * 注意: 此方法会永久删除内容,请谨慎使用
     */
    private void deleteContent(Report report) {
        log.info("开始删除违规内容: targetType={}, targetId={}", report.getTargetType(), report.getTargetId());
        
        try {
            switch (report.getTargetType()) {
                case PROJECT:
                    deleteProject(report.getTargetId());
                    break;
                case TEAM:
                    deleteTeam(report.getTargetId());
                    break;
                case COMMENT:
                    deleteComment(report.getTargetId());
                    break;
                case USER:
                    log.warn("不支持删除用户，请使用封禁功能");
                    throw new BusinessException("不支持删除用户，请使用封禁功能");
            }
            
            log.info("违规内容删除成功: targetType={}, targetId={}", report.getTargetType(), report.getTargetId());
        } catch (Exception e) {
            log.error("删除违规内容失败: targetType={}, targetId={}", report.getTargetType(), report.getTargetId(), e);
            throw new BusinessException("删除内容失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除项目
     */
    private void deleteProject(Long projectId) {
        try {
            // 使用MyBatis直接删除,避免循环依赖
            // 注意: 这里简化实现,实际应该调用ProjectService的删除方法以确保级联删除
            int deleted = reportMapper.deleteProject(projectId);
            if (deleted > 0) {
                log.info("项目删除成功: projectId={}", projectId);
            } else {
                log.warn("项目不存在或已被删除: projectId={}", projectId);
            }
        } catch (Exception e) {
            log.error("删除项目失败: projectId={}", projectId, e);
            throw new BusinessException("删除项目失败");
        }
    }
    
    /**
     * 删除团队
     */
    private void deleteTeam(Long teamId) {
        try {
            // 使用MyBatis直接删除,避免循环依赖
            int deleted = reportMapper.deleteTeam(teamId);
            if (deleted > 0) {
                log.info("团队删除成功: teamId={}", teamId);
            } else {
                log.warn("团队不存在或已被删除: teamId={}", teamId);
            }
        } catch (Exception e) {
            log.error("删除团队失败: teamId={}", teamId, e);
            throw new BusinessException("删除团队失败");
        }
    }
    
    /**
     * 删除评论
     */
    private void deleteComment(Long commentId) {
        try {
            // 使用MyBatis直接删除,避免循环依赖
            int deleted = reportMapper.deleteComment(commentId);
            if (deleted > 0) {
                log.info("评论删除成功: commentId={}", commentId);
            } else {
                log.warn("评论不存在或已被删除: commentId={}", commentId);
            }
        } catch (Exception e) {
            log.error("删除评论失败: commentId={}", commentId, e);
            throw new BusinessException("删除评论失败");
        }
    }
    
    /**
     * 扣除信誉分
     */
    private void deductCredit(Report report) {
        Long userId = getTargetUserId(report);
        if (userId == null) {
            log.error("无法获取目标用户ID");
            throw new BusinessException("无法获取目标用户ID");
        }
        
        // 根据违规类型扣除不同分数
        int deductAmount = getDeductAmount(report.getReason());
        
        // 调用信誉分服务扣除分数
        creditService.addCreditRecord(
            userId,
            -deductAmount,
            "REPORT_PENALTY",
            null,
            String.format("举报违规扣分：%s", getReasonName(report.getReason()))
        );
        
        log.info("信誉分扣除成功: userId={}, amount={}", userId, deductAmount);
        
        // 发送通知
        try {
            String title = "信誉分扣除通知";
            String content = String.format("您因违规行为被扣除%d信誉分。\n违规原因：%s\n\n请遵守平台规则，维护良好的社区环境。", 
                             deductAmount, 
                             getReasonName(report.getReason()));
            
            notificationService.createNotification(
                userId,
                "SYSTEM",
                title,
                content,
                "USER",
                userId
            );
        } catch (Exception e) {
            log.error("发送信誉分扣除通知失败", e);
        }
    }
    
    /**
     * 获取目标用户ID
     * 根据不同的目标类型,查询对应的创建者/作者ID
     */
    private Long getTargetUserId(Report report) {
        switch (report.getTargetType()) {
            case USER:
                // 直接返回用户ID
                return report.getTargetId();
            case PROJECT:
                // 查询项目创建者ID
                return reportMapper.getProjectCreatorId(report.getTargetId());
            case TEAM:
                // 查询团队创建者ID
                return reportMapper.getTeamCreatorId(report.getTargetId());
            case COMMENT:
                // 查询评论作者ID
                return reportMapper.getCommentAuthorId(report.getTargetId());
            default:
                log.warn("未知的目标类型: {}", report.getTargetType());
                return null;
        }
    }
    
    /**
     * 根据违规类型获取扣除分数
     */
    private int getDeductAmount(Report.ReportReason reason) {
        switch (reason) {
            case FRAUD:
                return 20; // 诈骗行为扣20分
            case HARASSMENT:
                return 15; // 骚扰行为扣15分
            case INAPPROPRIATE:
                return 10; // 不当内容扣10分
            case SPAM:
                return 5;  // 垃圾信息扣5分
            case OTHER:
                return 5;  // 其他扣5分
            default:
                return 5;
        }
    }
    
    /**
     * 获取目标类型名称
     */
    private String getTargetTypeName(Report.TargetType targetType) {
        switch (targetType) {
            case PROJECT: return "项目";
            case TEAM: return "团队";
            case USER: return "账号";
            case COMMENT: return "评论";
            default: return "内容";
        }
    }
    
    /**
     * 获取举报原因名称
     */
    private String getReasonName(Report.ReportReason reason) {
        switch (reason) {
            case SPAM: return "垃圾信息";
            case FRAUD: return "诈骗行为";
            case INAPPROPRIATE: return "不当内容";
            case HARASSMENT: return "骚扰行为";
            case OTHER: return "其他";
            default: return "未知";
        }
    }
    
    @Override
    public List<ReportStatisticsVO> getReportStatistics() {
        return reportMapper.selectReportStatistics();
    }
}
