package com.officeautomationsystemapi.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.officeautomationsystemapi.entity.NotificationRecords;
import com.officeautomationsystemapi.entity.Task;
import com.officeautomationsystemapi.repository.NotificationRecordsRepository;
import com.officeautomationsystemapi.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 每天 02:30 把完成或撤销、拒绝超过 30 天的任务归档
@Component
public class TaskArchiveScheduler {

    private static final Logger log = LoggerFactory.getLogger(TaskArchiveScheduler.class);
    private static final int ARCHIVE_DAYS = 30;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private NotificationRecordsRepository notificationRepository;

    private void notifyTask(Long userId, String content) {
        if (userId == null) {
            return;
        }
        try {
            NotificationRecords n = new NotificationRecords();
            n.setUser_id(userId);
            n.setTitle("任务通知");
            n.setContent(content);
            n.setType(3);
            n.setBiz_type("task");
            n.setJump_url("/home/task?type=all");
            n.setIs_read(0);
            n.setCreated_at(LocalDateTime.now());
            notificationRepository.insert(n);
        } catch (Exception ignored) {
            // 通知失败不影响归档
        }
    }

    @Scheduled(cron = "0 30 2 * * ?")
    public void autoArchive() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(ARCHIVE_DAYS);
        List<Task> targets = new ArrayList<>();

        // 已完成超 30 天，没有完成时间就看计划时间
        QueryWrapper<Task> completed = new QueryWrapper<>();
        completed.eq("archive_status", 0).eq("status", 4)
                .and(w -> w.lt("actual_end_time", threshold)
                        .or(o -> o.isNull("actual_end_time").lt("plan_end_time", threshold)));
        targets.addAll(taskRepository.selectList(completed));

        // 撤销或拒绝超 30 天，按最后更新时间算
        QueryWrapper<Task> ended = new QueryWrapper<>();
        ended.eq("archive_status", 0).in("status", 5, 6).lt("updated_at", threshold);
        targets.addAll(taskRepository.selectList(ended));

        int archived = 0;
        for (Task t : targets) {
            t.setArchive_status(1);
            taskRepository.updateById(t);
            if (t.getCreator_id() != null) {
                notifyTask(t.getCreator_id(), "任务已自动归档：" + t.getTitle());
            }
            archived++;
        }
        if (archived > 0) {
            log.info("[任务自动归档] 本次自动归档 {} 条任务", archived);
        }
    }
}
