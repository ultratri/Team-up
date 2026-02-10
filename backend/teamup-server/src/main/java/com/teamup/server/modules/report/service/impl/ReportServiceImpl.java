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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        
        log.info("举报处理完成: reportId={}", dto.getReportId());
    }
    
    /**
     * 执行惩罚
     */
    private void executePunishment(Report report, HandleReportDTO dto) {
        log.info("执行惩罚: targetType={}, targetId={}, punishmentType={}", 
                 report.getTargetType(), report.getTargetId(), dto.getPunishmentType());
        
        // TODO: 根据惩罚类型执行相应操作
        // BAN_USER: 封禁用户
        // DELETE_CONTENT: 删除内容（项目/评论等）
        // DEDUCT_CREDIT: 扣除信誉分
        
        switch (dto.getPunishmentType()) {
            case "BAN_USER":
                // 封禁用户逻辑
                log.info("封禁用户: userId={}, days={}", report.getTargetId(), dto.getPunishmentDays());
                break;
            case "DELETE_CONTENT":
                // 删除内容逻辑
                log.info("删除内容: targetType={}, targetId={}", report.getTargetType(), report.getTargetId());
                break;
            case "DEDUCT_CREDIT":
                // 扣除信誉分逻辑
                log.info("扣除信誉分: userId={}", report.getTargetId());
                break;
            default:
                log.warn("未知的惩罚类型: {}", dto.getPunishmentType());
        }
    }
    
    @Override
    public List<ReportStatisticsVO> getReportStatistics() {
        return reportMapper.selectReportStatistics();
    }
}
