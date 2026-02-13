-- 清理导师列表的测试数据
-- 只保留真实的导师数据

-- 删除不存在用户的导师绩效记录
DELETE FROM mentor_performance 
WHERE mentor_id NOT IN (SELECT id FROM users);

-- 删除没有真实mentor_relationships记录但有假绩效数据的记录
-- 保留有真实学员关系的导师数据
DELETE FROM mentor_performance 
WHERE mentor_id IN (
    SELECT mp.mentor_id 
    FROM (SELECT mentor_id FROM mentor_performance) mp
    LEFT JOIN mentor_relationships mr ON mp.mentor_id = mr.mentor_id
    WHERE mp.mentor_id NOT IN (1, 2) -- 保留系统管理员和李明华
    GROUP BY mp.mentor_id
    HAVING COUNT(mr.id) = 0 AND MAX(mp.total_mentees) > 0
);

-- 为新导师（dao2）创建正确的初始绩效记录
INSERT INTO mentor_performance (mentor_id, total_mentees, active_mentees, successful_mentees, average_mentee_score, total_reward_points, rating)
SELECT 6, 0, 0, 0, 0, 0, 4.0
WHERE NOT EXISTS (SELECT 1 FROM mentor_performance WHERE mentor_id = 6);
