package com.teamup.server.modules.team.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.team.entity.Sprint;
import com.teamup.server.modules.team.entity.Task;
import com.teamup.server.modules.team.mapper.SprintMapper;
import com.teamup.server.modules.team.mapper.TaskMapper;
import com.teamup.server.modules.team.service.SprintService;
import com.teamup.server.modules.team.vo.SprintVO;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Sprint服务实现类
 */
@Service
@RequiredArgsConstructor
public class SprintServiceImpl implements SprintService {
    
    private final SprintMapper sprintMapper;
    private final TaskMapper taskMapper;
    private final UserMapper userMapper;
    
    @Override
    @Transactional
    public Sprint createSprint(Sprint sprint) {
        // 验证日期
        if (sprint.getEndDate().isBefore(sprint.getStartDate())) {
            throw new BusinessException("结束日期不能早于开始日期");
        }
        
        // 设置默认状态
        if (sprint.getStatus() == null) {
            sprint.setStatus("PLANNING");
        }
        
        sprintMapper.insert(sprint);
        return sprint;
    }
    
    @Override
    @Transactional
    public Sprint updateSprint(Sprint sprint) {
        Sprint existing = sprintMapper.selectById(sprint.getId());
        if (existing == null) {
            throw new BusinessException("Sprint不存在");
        }
        
        // 验证日期
        if (sprint.getEndDate() != null && sprint.getStartDate() != null) {
            if (sprint.getEndDate().isBefore(sprint.getStartDate())) {
                throw new BusinessException("结束日期不能早于开始日期");
            }
        }
        
        sprintMapper.updateById(sprint);
        return sprint;
    }
    
    @Override
    @Transactional
    public void deleteSprint(Long id) {
        Sprint sprint = sprintMapper.selectById(id);
        if (sprint == null) {
            throw new BusinessException("Sprint不存在");
        }
        
        // 检查是否有关联的任务
        LambdaQueryWrapper<Task> taskQuery = new LambdaQueryWrapper<>();
        taskQuery.eq(Task::getSprintId, id);
        Long taskCount = taskMapper.selectCount(taskQuery);
        
        if (taskCount > 0) {
            throw new BusinessException("该Sprint下还有任务，无法删除");
        }
        
        sprintMapper.deleteById(id);
    }
    
    @Override
    public List<SprintVO> getTeamSprints(Long teamId) {
        LambdaQueryWrapper<Sprint> query = new LambdaQueryWrapper<>();
        query.eq(Sprint::getTeamId, teamId)
             .orderByDesc(Sprint::getCreatedAt);
        
        List<Sprint> sprints = sprintMapper.selectList(query);
        
        return sprints.stream().map(sprint -> {
            SprintVO vo = new SprintVO();
            BeanUtils.copyProperties(sprint, vo);
            
            // 获取创建者信息
            User creator = userMapper.selectById(sprint.getCreatedBy());
            if (creator != null) {
                vo.setCreatorName(creator.getUsername());
            }
            
            // 统计任务信息
            LambdaQueryWrapper<Task> taskQuery = new LambdaQueryWrapper<>();
            taskQuery.eq(Task::getSprintId, sprint.getId());
            List<Task> tasks = taskMapper.selectList(taskQuery);
            
            vo.setTotalTasks(tasks.size());
            vo.setCompletedTasks((int) tasks.stream().filter(t -> "DONE".equals(t.getStatus())).count());
            vo.setInProgressTasks((int) tasks.stream().filter(t -> "DOING".equals(t.getStatus())).count());
            
            return vo;
        }).collect(Collectors.toList());
    }
    
    @Override
    public SprintVO getSprintDetail(Long id) {
        Sprint sprint = sprintMapper.selectById(id);
        if (sprint == null) {
            throw new BusinessException("Sprint不存在");
        }
        
        SprintVO vo = new SprintVO();
        BeanUtils.copyProperties(sprint, vo);
        
        // 获取创建者信息
        User creator = userMapper.selectById(sprint.getCreatedBy());
        if (creator != null) {
            vo.setCreatorName(creator.getUsername());
        }
        
        // 统计任务信息
        LambdaQueryWrapper<Task> taskQuery = new LambdaQueryWrapper<>();
        taskQuery.eq(Task::getSprintId, id);
        List<Task> tasks = taskMapper.selectList(taskQuery);
        
        vo.setTotalTasks(tasks.size());
        vo.setCompletedTasks((int) tasks.stream().filter(t -> "DONE".equals(t.getStatus())).count());
        vo.setInProgressTasks((int) tasks.stream().filter(t -> "DOING".equals(t.getStatus())).count());
        
        return vo;
    }
    
    @Override
    @Transactional
    public void startSprint(Long id) {
        Sprint sprint = sprintMapper.selectById(id);
        if (sprint == null) {
            throw new BusinessException("Sprint不存在");
        }
        
        if (!"PLANNING".equals(sprint.getStatus())) {
            throw new BusinessException("只有规划中的Sprint才能开始");
        }
        
        sprint.setStatus("IN_PROGRESS");
        sprintMapper.updateById(sprint);
    }
    
    @Override
    @Transactional
    public void completeSprint(Long id) {
        Sprint sprint = sprintMapper.selectById(id);
        if (sprint == null) {
            throw new BusinessException("Sprint不存在");
        }
        
        if (!"IN_PROGRESS".equals(sprint.getStatus())) {
            throw new BusinessException("只有进行中的Sprint才能完成");
        }
        
        sprint.setStatus("COMPLETED");
        sprintMapper.updateById(sprint);
    }
}
