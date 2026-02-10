package com.teamup.server.common.audit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 审计日志Mapper
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
    
    /**
     * 删除指定时间之前的文件操作日志
     */
    @Delete("DELETE FROM audit_logs WHERE action IN ('DELETE_FILE', 'DELETE_FOLDER') AND created_at < #{threshold}")
    int deleteOldFileLogs(@Param("threshold") LocalDateTime threshold);
    
    /**
     * 删除指定时间之前的模板操作日志
     */
    @Delete("DELETE FROM audit_logs WHERE action IN ('CREATE_COMPETITION_TEMPLATE', 'UPDATE_COMPETITION_TEMPLATE', 'DELETE_COMPETITION_TEMPLATE') AND created_at < #{threshold}")
    int deleteOldTemplateLogs(@Param("threshold") LocalDateTime threshold);
}
