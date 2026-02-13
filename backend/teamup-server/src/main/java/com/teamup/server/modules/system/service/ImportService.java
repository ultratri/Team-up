package com.teamup.server.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.opencsv.CSVReader;
import com.teamup.server.modules.competition.entity.Competition;
import com.teamup.server.modules.competition.mapper.CompetitionMapper;
import com.teamup.server.modules.project.entity.Project;
import com.teamup.server.modules.project.mapper.ProjectMapper;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.mapper.TeamMapper;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;
    private final TeamMapper teamMapper;
    private final CompetitionMapper competitionMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 导入数据
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> importData(String type, MultipartFile file) {
        try {
            String filename = file.getOriginalFilename();
            if (filename == null) {
                throw new IllegalArgumentException("文件名为空");
            }

            if (filename.endsWith(".csv")) {
                return importFromCsv(type, file);
            } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                return importFromExcel(type, file);
            } else {
                throw new IllegalArgumentException("不支持的文件格式，仅支持 Excel 和 CSV");
            }
        } catch (Exception e) {
            log.error("导入数据失败: type={}", type, e);
            throw new RuntimeException("导入失败: " + e.getMessage());
        }
    }

    /**
     * 从 Excel 导入
     */
    private Map<String, Integer> importFromExcel(String type, MultipartFile file) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            int successCount = 0;
            int failCount = 0;

            // 跳过标题行，从第二行开始
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    switch (type) {
                        case "users":
                            importUserRow(row);
                            break;
                        case "projects":
                            importProjectRow(row);
                            break;
                        case "teams":
                            importTeamRow(row);
                            break;
                        case "competitions":
                            importCompetitionRow(row);
                            break;
                        default:
                            throw new IllegalArgumentException("不支持的导入类型: " + type);
                    }
                    successCount++;
                } catch (Exception e) {
                    log.warn("导入第 {} 行失败: {}", i + 1, e.getMessage());
                    failCount++;
                }
            }

            Map<String, Integer> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            return result;
        }
    }

    /**
     * 从 CSV 导入
     */
    private Map<String, Integer> importFromCsv(String type, MultipartFile file) throws Exception {
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), "UTF-8"))) {
            // 跳过标题行
            reader.readNext();

            int successCount = 0;
            int failCount = 0;
            String[] line;

            while ((line = reader.readNext()) != null) {
                try {
                    switch (type) {
                        case "users":
                            importUserFromCsv(line);
                            break;
                        case "projects":
                            importProjectFromCsv(line);
                            break;
                        case "teams":
                            importTeamFromCsv(line);
                            break;
                        case "competitions":
                            importCompetitionFromCsv(line);
                            break;
                        default:
                            throw new IllegalArgumentException("不支持的导入类型: " + type);
                    }
                    successCount++;
                } catch (Exception e) {
                    log.warn("导入行失败: {}", e.getMessage());
                    failCount++;
                }
            }

            Map<String, Integer> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            return result;
        }
    }

    // ==================== Excel 行导入方法 ====================

    private void importUserRow(Row row) {
        User user = new User();
        user.setUserCode(getCellValue(row.getCell(1)));
        user.setUsername(getCellValue(row.getCell(2)));
        user.setEmail(getCellValue(row.getCell(3)));
        user.setPhone(getCellValue(row.getCell(4)));
        user.setStatus(getCellValue(row.getCell(6)));
        user.setCreatedAt(parseDateTime(getCellValue(row.getCell(7))));
        user.setLastLoginAt(parseDateTime(getCellValue(row.getCell(8))));
        
        // 检查是否已存在（根据学号或邮箱）
        User existing = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUserCode, user.getUserCode())
        );
        
        if (existing != null) {
            // 更新现有用户（保留密码）
            user.setId(existing.getId());
            user.setPassword(existing.getPassword());
            userMapper.updateById(user);
        } else {
            // 新增用户时跳过（不导入新用户，因为涉及密码问题）
            log.warn("跳过新用户导入: {}", user.getUserCode());
        }
    }

    private void importProjectRow(Row row) {
        Project project = new Project();
        project.setTitle(getCellValue(row.getCell(1)));
        project.setProjectType(getCellValue(row.getCell(2)));
        project.setDescription(getCellValue(row.getCell(3)));
        project.setStatus(getCellValue(row.getCell(4)));
        
        String creatorIdStr = getCellValue(row.getCell(5));
        if (!creatorIdStr.isEmpty()) {
            project.setCreatorId(Long.parseLong(creatorIdStr));
        }
        
        project.setCreatedAt(parseDateTime(getCellValue(row.getCell(6))));
        
        // 检查是否已存在（根据标题和创建者）
        Project existing = projectMapper.selectOne(
            new LambdaQueryWrapper<Project>()
                .eq(Project::getTitle, project.getTitle())
                .eq(Project::getCreatorId, project.getCreatorId())
        );
        
        if (existing != null) {
            project.setId(existing.getId());
            projectMapper.updateById(project);
        } else {
            projectMapper.insert(project);
        }
    }

    private void importTeamRow(Row row) {
        Team team = new Team();
        team.setTeamName(getCellValue(row.getCell(1)));
        // 将type转换为team_nature：PROJECT->TEMPORARY, COMPETITION->LONG_TERM
        String type = getCellValue(row.getCell(2));
        if ("COMPETITION".equals(type)) {
            team.setTeamNature("LONG_TERM");
        } else {
            team.setTeamNature("TEMPORARY");
        }
        team.setDescription(getCellValue(row.getCell(3)));
        
        String leaderIdStr = getCellValue(row.getCell(4));
        if (!leaderIdStr.isEmpty()) {
            team.setLeaderId(Long.parseLong(leaderIdStr));
        }
        
        String projectIdStr = getCellValue(row.getCell(5));
        if (!projectIdStr.isEmpty() && !projectIdStr.equals("0")) {
            team.setProjectId(Long.parseLong(projectIdStr));
        }
        
        String competitionIdStr = getCellValue(row.getCell(6));
        if (!competitionIdStr.isEmpty() && !competitionIdStr.equals("0")) {
            team.setCompetitionId(Long.parseLong(competitionIdStr));
        }
        
        team.setCreatedAt(parseDateTime(getCellValue(row.getCell(7))));
        
        // 检查是否已存在（根据团队名称）
        Team existing = teamMapper.selectOne(
            new LambdaQueryWrapper<Team>()
                .eq(Team::getTeamName, team.getTeamName())
        );
        
        if (existing != null) {
            team.setId(existing.getId());
            teamMapper.updateById(team);
        } else {
            teamMapper.insert(team);
        }
    }

    private void importCompetitionRow(Row row) {
        Competition competition = new Competition();
        competition.setName(getCellValue(row.getCell(1)));
        competition.setOrganizer(getCellValue(row.getCell(2)));
        competition.setLevel(getCellValue(row.getCell(3)));
        competition.setStatus(getCellValue(row.getCell(4)));
        competition.setSignupStartAt(parseDateTime(getCellValue(row.getCell(5))));
        competition.setSignupEndAt(parseDateTime(getCellValue(row.getCell(6))));
        competition.setStartAt(parseDateTime(getCellValue(row.getCell(7))));
        competition.setEndAt(parseDateTime(getCellValue(row.getCell(8))));
        
        // 检查是否已存在（根据比赛名称和主办方）
        Competition existing = competitionMapper.selectOne(
            new LambdaQueryWrapper<Competition>()
                .eq(Competition::getName, competition.getName())
                .eq(Competition::getOrganizer, competition.getOrganizer())
        );
        
        if (existing != null) {
            competition.setId(existing.getId());
            competitionMapper.updateById(competition);
        } else {
            competitionMapper.insert(competition);
        }
    }

    // ==================== CSV 行导入方法 ====================

    private void importUserFromCsv(String[] line) {
        if (line.length < 9) return;
        
        User user = new User();
        user.setUserCode(line[1]);
        user.setUsername(line[2]);
        user.setEmail(line[3]);
        user.setPhone(line[4]);
        user.setStatus(line[6]);
        user.setCreatedAt(parseDateTime(line[7]));
        user.setLastLoginAt(parseDateTime(line[8]));
        
        User existing = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUserCode, user.getUserCode())
        );
        
        if (existing != null) {
            user.setId(existing.getId());
            user.setPassword(existing.getPassword());
            userMapper.updateById(user);
        } else {
            // 跳过新用户导入
            log.warn("跳过新用户导入: {}", user.getUserCode());
        }
    }

    private void importProjectFromCsv(String[] line) {
        if (line.length < 7) return;
        
        Project project = new Project();
        project.setTitle(line[1]);
        project.setProjectType(line[2]);
        project.setDescription(line[3]);
        project.setStatus(line[4]);
        
        if (!line[5].isEmpty()) {
            project.setCreatorId(Long.parseLong(line[5]));
        }
        
        project.setCreatedAt(parseDateTime(line[6]));
        
        Project existing = projectMapper.selectOne(
            new LambdaQueryWrapper<Project>()
                .eq(Project::getTitle, project.getTitle())
                .eq(Project::getCreatorId, project.getCreatorId())
        );
        
        if (existing != null) {
            project.setId(existing.getId());
            projectMapper.updateById(project);
        } else {
            projectMapper.insert(project);
        }
    }

    private void importTeamFromCsv(String[] line) {
        if (line.length < 8) return;
        
        Team team = new Team();
        team.setTeamName(line[1]);
        // 将type转换为team_nature：PROJECT->TEMPORARY, COMPETITION->LONG_TERM
        String type = line[2];
        if ("COMPETITION".equals(type)) {
            team.setTeamNature("LONG_TERM");
        } else {
            team.setTeamNature("TEMPORARY");
        }
        team.setDescription(line[3]);
        
        if (!line[4].isEmpty()) {
            team.setLeaderId(Long.parseLong(line[4]));
        }
        
        if (!line[5].isEmpty() && !line[5].equals("0")) {
            team.setProjectId(Long.parseLong(line[5]));
        }
        
        if (!line[6].isEmpty() && !line[6].equals("0")) {
            team.setCompetitionId(Long.parseLong(line[6]));
        }
        
        team.setCreatedAt(parseDateTime(line[7]));
        
        Team existing = teamMapper.selectOne(
            new LambdaQueryWrapper<Team>()
                .eq(Team::getTeamName, team.getTeamName())
        );
        
        if (existing != null) {
            team.setId(existing.getId());
            teamMapper.updateById(team);
        } else {
            teamMapper.insert(team);
        }
    }

    private void importCompetitionFromCsv(String[] line) {
        if (line.length < 9) return;
        
        Competition competition = new Competition();
        competition.setName(line[1]);
        competition.setOrganizer(line[2]);
        competition.setLevel(line[3]);
        competition.setStatus(line[4]);
        competition.setSignupStartAt(parseDateTime(line[5]));
        competition.setSignupEndAt(parseDateTime(line[6]));
        competition.setStartAt(parseDateTime(line[7]));
        competition.setEndAt(parseDateTime(line[8]));
        
        Competition existing = competitionMapper.selectOne(
            new LambdaQueryWrapper<Competition>()
                .eq(Competition::getName, competition.getName())
                .eq(Competition::getOrganizer, competition.getOrganizer())
        );
        
        if (existing != null) {
            competition.setId(existing.getId());
            competitionMapper.updateById(competition);
        } else {
            competitionMapper.insert(competition);
        }
    }

    // ==================== 工具方法 ====================

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().format(DATE_FORMATTER);
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty() || dateStr.equals("-")) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("解析日期失败: {}", dateStr);
            return null;
        }
    }
}
