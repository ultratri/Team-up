-- Ensure skill level consistency
-- Created: 2026-03-03
-- Description: Unify all skill levels to BEGINNER, INTERMEDIATE, ADVANCED, EXPERT

-- 1. Ensure project_skill_requirements.expected_level supports all 4 levels
ALTER TABLE project_skill_requirements 
MODIFY COLUMN expected_level ENUM('BEGINNER','INTERMEDIATE','ADVANCED','EXPERT') 
DEFAULT 'INTERMEDIATE'
COMMENT 'Proficiency level: BEGINNER, INTERMEDIATE, ADVANCED, EXPERT';

-- 2. Update table comment
ALTER TABLE project_skill_requirements 
COMMENT='Project skill requirements table';
