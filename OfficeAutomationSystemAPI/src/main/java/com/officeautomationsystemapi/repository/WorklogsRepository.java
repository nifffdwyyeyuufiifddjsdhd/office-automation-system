package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.officeautomationsystemapi.entity.Worklogs;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface WorklogsRepository extends BaseMapper<Worklogs> {

    // 统计某用户某月（yyyy-MM）的日志条数
    @Select("SELECT COUNT(*) FROM worklogs WHERE user_id=#{userId} AND deleted_at IS NULL " +
            "AND DATE_FORMAT(log_date,'%Y-%m')=#{month}")
    Long countByUserMonth(@Param("userId") Long userId, @Param("month") String month);

    // 统计某用户有日志的天数（某月）
    @Select("SELECT COUNT(DISTINCT log_date) FROM worklogs WHERE user_id=#{userId} AND deleted_at IS NULL " +
            "AND DATE_FORMAT(log_date,'%Y-%m')=#{month}")
    Long countDaysByUserMonth(@Param("userId") Long userId, @Param("month") String month);

    // 回收站分页，只查 deleted_at 非空的；userIds 非空时按人过滤
    @Select({"<script>",
            "SELECT * FROM worklogs WHERE deleted_at IS NOT NULL",
            "AND (title LIKE CONCAT('%',#{keyword},'%') OR #{keyword} IS NULL OR #{keyword}='')",
            "<if test='userIds != null'>",
            "AND user_id IN",
            "<foreach collection='userIds' item='uid' open='(' separator=',' close=')'>#{uid}</foreach>",
            "</if>",
            "ORDER BY deleted_at DESC",
            "</script>"})
    IPage<Worklogs> selectRecyclePage(IPage<Worklogs> page, @Param("keyword") String keyword,
                                      @Param("userIds") List<Long> userIds);

    // 某用户本月被查看数合计 / 评论数合计
    @Select("SELECT COALESCE(SUM(view_count),0) FROM worklogs WHERE user_id=#{userId} AND deleted_at IS NULL")
    Long sumViewCount(@Param("userId") Long userId);

    @Select("SELECT COALESCE(SUM(comment_count),0) FROM worklogs WHERE user_id=#{userId} AND deleted_at IS NULL")
    Long sumCommentCount(@Param("userId") Long userId);

    // 按 user_id 聚合日志数（用于部门排名，先查成员再统计）
    @Select({"<script>",
            "SELECT user_id, COUNT(*) AS cnt FROM worklogs",
            "WHERE deleted_at IS NULL AND user_id IN",
            "<foreach collection='userIds' item='uid' open='(' separator=',' close=')'>#{uid}</foreach>",
            "GROUP BY user_id ORDER BY cnt DESC",
            "</script>"})
    List<java.util.Map<String, Object>> countByUserIds(@Param("userIds") List<Long> userIds);

    // 回收站恢复：清除软删除标记
    @Update("UPDATE worklogs SET deleted_at = NULL, updated_at = NOW() WHERE id = #{id}")
    int restoreById(@Param("id") Long id);
}
