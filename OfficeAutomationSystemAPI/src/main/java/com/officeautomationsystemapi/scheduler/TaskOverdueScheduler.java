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
import java.util.List;

// 每 30 分钟把过了计划结束时间没结束的任务标成逾期，并通知负责人和创建人
@Component
public class TaskOverdueScheduler {

    private static final Logger log = LoggerFactory.getLogger(TaskOverdueScheduler.class);

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
            n.setJump_url("/home/task?type=assigned");
            n.setIs_read(0);
            n.setCreated_at(LocalDateTime.now());
            notificationRepository.insert(n);
        } catch (Exception ignored) {
            // 通知失败不影响标记
        }
    }

    @Scheduled(cron = "0 0/30 * * * ?")
    public void markOverdueTasks() {
        QueryWrapper<Task> qw = new QueryWrapper<>();
        qw.eq("is_overdue", 0)
                .in("status", 1, 2, 3)
                .isNotNull("plan_end_time")
                .lt("plan_end_time", LocalDateTime.now());
        List<Task> overdue = taskRepository.selectList(qw);
        int marked = 0;
        for (Task t : overdue) {
            t.setIs_overdue(1);
            taskRepository.updateById(t);
            String msg = "任务已逾期，请尽快处理：" + t.getTitle();
            if (t.getAssignee_id() != null) {
                notifyTask(t.getAssignee_id(), msg);
            }
            if (t.getCreator_id() != null && !t.getCreator_id().equals(t.getAssignee_id())) {
                notifyTask(t.getCreator_id(), msg);
            }
            marked++;
        }
        if (marked > 0) {
            log.info("[任务逾期标记] 本次自动标记 {} 条逾期任务并已发送通知", marked);
        }
    }
}
