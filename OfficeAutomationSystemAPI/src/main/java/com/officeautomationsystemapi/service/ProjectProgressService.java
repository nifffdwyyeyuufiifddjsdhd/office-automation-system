package com.officeautomationsystemapi.service;

import com.officeautomationsystemapi.entity.Projects;
import com.officeautomationsystemapi.repository.ProjectsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

// 项目进度：未结束任务的平均进度，全做完算 100
// 任务侧改动和项目详情都调这里，避免两边进度对不上
@Service
public class ProjectProgressService {

    private static final String CLOSED_STATUS = "4,5,6";
    private static final int STATUS_DONE = 3;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ProjectsRepository projectRepository;

    // 只算不写，给列表展示用
    public int compute(Long projectId) {
        if (projectId == null) return 0;
        Map<String, Object> row = jdbc.queryForMap(
            "SELECT COUNT(*) AS total, COALESCE(SUM(progress),0) AS sum FROM tasks"
                + " WHERE project_id = ? AND deleted_at IS NULL AND archive_status = 0"
                + " AND status NOT IN (" + CLOSED_STATUS + ")", projectId);
        int total = ((Number) row.get("total")).intValue();
        if (total == 0) {
            Map<String, Object> doneRow = jdbc.queryForMap(
                "SELECT COUNT(*) AS c FROM tasks WHERE project_id = ? AND deleted_at IS NULL AND status = 4",
                projectId);
            return ((Number) doneRow.get("c")).intValue() > 0 ? 100 : 0;
        }
        int sum = ((Number) row.get("sum")).intValue();
        return Math.min(100, Math.round(sum * 1.0f / total));
    }

    // 重算并落库，返回最新进度；已结项的项目保持原值不改
    public int refresh(Long projectId) {
        if (projectId == null || projectId <= 0) return 0;
        Projects p = projectRepository.selectById(projectId);
        if (p == null) return 0;
        if (p.getStatus() != null && p.getStatus() == STATUS_DONE) {
            return p.getProgress() == null ? 100 : p.getProgress();
        }
        int progress = compute(projectId);
        if (!Objects.equals(p.getProgress(), progress)) {
            p.setProgress(progress);
            projectRepository.updateById(p);
        }
        return progress;
    }
}
