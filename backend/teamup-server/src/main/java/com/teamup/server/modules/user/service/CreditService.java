package com.teamup.server.modules.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.modules.user.entity.UserCredit;
import com.teamup.server.modules.user.entity.CreditRecord;
import com.teamup.server.modules.user.mapper.UserCreditMapper;
import com.teamup.server.modules.user.mapper.CreditRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 信誉分服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreditService {
    
    private final UserCreditMapper userCreditMapper;
    private final CreditRecordMapper creditRecordMapper;
    
    /**
     * 添加信誉变更记录
     */
    @Transactional
    public void addCreditRecord(Long userId, Integer changeAmount, 
                                String changeType, 
                                Long projectId, String reason) {
        // 1. 创建信誉变更记录
        CreditRecord record = new CreditRecord();
        record.setUserId(userId);
        record.setChangeAmount(changeAmount);
        record.setChangeType(changeType);
        record.setProjectId(projectId);
        record.setReason(reason);
        record.setCreatedAt(LocalDateTime.now());
        creditRecordMapper.insert(record);
        
        // 2. 查询或创建用户信誉记录
        UserCredit credit = userCreditMapper.selectOne(
            new LambdaQueryWrapper<UserCredit>()
                .eq(UserCredit::getUserId, userId)
        );
        
        if (credit == null) {
            credit = new UserCredit();
            credit.setUserId(userId);
            credit.setTotalCredit(60); // 新用户默认60分
        }
        
        // 3. 更新总信誉分
        Integer newTotal = credit.getTotalCredit() + changeAmount;
        // 信誉分不能低于0
        if (newTotal < 0) {
            newTotal = 0;
        }
        credit.setTotalCredit(newTotal);
        credit.setUpdatedAt(LocalDateTime.now());
        
        // 4. 更新信誉等级
        credit.setCreditLevel(calculateCreditLevel(newTotal));
        
        // 5. 保存或更新
        if (credit.getId() == null) {
            userCreditMapper.insert(credit);
        } else {
            userCreditMapper.updateById(credit);
        }
        
        log.info("信誉分变更: userId={}, change={}, type={}, newTotal={}, level={}", 
            userId, changeAmount, changeType, newTotal, credit.getCreditLevel());
    }
    
    /**
     * 计算信誉等级
     */
    private String calculateCreditLevel(Integer totalCredit) {
        if (totalCredit >= 95) return "OUTSTANDING";
        if (totalCredit >= 80) return "EXCELLENT";
        if (totalCredit >= 60) return "RELIABLE";
        return "NEWBIE";
    }
    
    /**
     * 获取用户信誉记录
     */
    public UserCredit getUserCredit(Long userId) {
        UserCredit credit = userCreditMapper.selectOne(
            new LambdaQueryWrapper<UserCredit>()
                .eq(UserCredit::getUserId, userId)
        );
        
        // 如果不存在，返回默认值
        if (credit == null) {
            credit = new UserCredit();
            credit.setUserId(userId);
            credit.setTotalCredit(60);
            credit.setCreditLevel("NEWBIE");
        }
        
        return credit;
    }
}
