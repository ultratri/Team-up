package com.teamup.server.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.user.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户档案Mapper
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {

    @Select({
            "<script>",
            "SELECT user_id FROM user_profiles",
            "WHERE 1=1",
            "<if test='departments != null and departments.size() &gt; 0'>",
            " AND department IN",
            " <foreach collection='departments' item='d' open='(' separator=',' close=')'>",
            "   #{d}",
            " </foreach>",
            "</if>",
            "<if test='majors != null and majors.size() &gt; 0'>",
            " AND major IN",
            " <foreach collection='majors' item='m' open='(' separator=',' close=')'>",
            "   #{m}",
            " </foreach>",
            "</if>",
            "<if test='grades != null and grades.size() &gt; 0'>",
            " AND grade IN",
            " <foreach collection='grades' item='g' open='(' separator=',' close=')'>",
            "   #{g}",
            " </foreach>",
            "</if>",
            "LIMIT #{limit}",
            "</script>"
    })
    List<Long> selectUserIdsByAudience(
            @Param("departments") List<String> departments,
            @Param("majors") List<String> majors,
            @Param("grades") List<Integer> grades,
            @Param("limit") int limit
    );
}

