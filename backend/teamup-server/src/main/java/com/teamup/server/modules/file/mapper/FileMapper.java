package com.teamup.server.modules.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.file.entity.FileEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文件Mapper接口
 */
@Mapper
public interface FileMapper extends BaseMapper<FileEntity> {
    
    /**
     * 根据团队ID和父文件夹ID查询文件列表
     * 
     * @param teamId 团队ID
     * @param parentFolderId 父文件夹ID（null表示根目录）
     * @return 文件列表
     */
    @Select("<script>" +
            "SELECT * FROM files " +
            "WHERE team_id = #{teamId} " +
            "<if test='parentFolderId != null'>" +
            "AND parent_folder_id = #{parentFolderId} " +
            "</if>" +
            "<if test='parentFolderId == null'>" +
            "AND parent_folder_id IS NULL " +
            "</if>" +
            "ORDER BY uploaded_at DESC" +
            "</script>")
    List<FileEntity> selectByTeamAndFolder(@Param("teamId") Long teamId, 
                                           @Param("parentFolderId") Long parentFolderId);
    
    /**
     * 根据团队ID统计文件数量（不包括文件夹）
     * 
     * @param teamId 团队ID
     * @return 文件数量
     */
    @Select("SELECT COUNT(*) FROM files WHERE team_id = #{teamId} AND is_folder = FALSE")
    int countFilesByTeamId(@Param("teamId") Long teamId);
    
    /**
     * 递归查询文件夹下的所有文件和子文件夹
     * 
     * @param folderId 文件夹ID
     * @return 文件列表
     */
    @Select("WITH RECURSIVE folder_tree AS (" +
            "  SELECT * FROM files WHERE id = #{folderId} " +
            "  UNION ALL " +
            "  SELECT f.* FROM files f " +
            "  INNER JOIN folder_tree ft ON f.parent_folder_id = ft.id" +
            ") " +
            "SELECT * FROM folder_tree WHERE id != #{folderId}")
    List<FileEntity> selectAllFilesInFolder(@Param("folderId") Long folderId);

    /**
     * 统计某个文件夹下（当前层级）的文件总大小
     *
     * @param teamId 团队ID
     * @param folderId 文件夹ID
     * @return 文件大小总和（字节）
     */
    @Select("SELECT COALESCE(SUM(file_size), 0) FROM files WHERE team_id = #{teamId} AND parent_folder_id = #{folderId} AND is_folder = FALSE")
    Long sumFileSizeByFolder(@Param("teamId") Long teamId, @Param("folderId") Long folderId);
}
