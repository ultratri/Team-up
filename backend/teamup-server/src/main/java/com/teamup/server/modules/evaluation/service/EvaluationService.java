package com.teamup.server.modules.evaluation.service;

import com.teamup.server.modules.evaluation.dto.EvaluationDTO;
import com.teamup.server.modules.evaluation.vo.EvaluationVO;
import java.util.List;

/**
 * 评价服务接口
 */
public interface EvaluationService {
    
    /**
     * 提交评价
     * @param teamId 团队ID
     * @param evaluatorId 评价者ID
     * @param dto 评价数据
     */
    void submitEvaluation(Long teamId, Long evaluatorId, EvaluationDTO dto);
    
    /**
     * 获取团队评价列表
     * @param teamId 团队ID
     * @return 评价列表
     */
    List<EvaluationVO> getEvaluations(Long teamId);
    
    /**
     * 检查是否可以评价
     * @param evaluatorId 评价者ID
     * @param evaluatedId 被评价者ID
     * @param projectId 项目ID
     * @return 是否可以评价
     */
    boolean canEvaluate(Long evaluatorId, Long evaluatedId, Long projectId);
}
