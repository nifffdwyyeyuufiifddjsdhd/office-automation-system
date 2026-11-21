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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 每小时提醒 24 小时内到期的任务，每天 9 点发当日待办汇总
@Component
public class TaskRemindScheduler {

    private static final Logger log = LoggerFactory.getLogger(TaskRemindScheduler.class);

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private NotificationRecordsRepository notificationRepository;

    private void notifyTask(Long userId, String content, String link) {
        if (userId == null) {
            return;
        }
        try {
            NotificationRecords n = new NotificationRecords();
            n.setUser_id(userId);
            n.setTitle("任务提醒");
            n.setContent(content);
            n.setType(3);
            n.setBiz_type("task");
            n.setJump_url(link);
            n.setIs_read(0);
            n.setCreated_at(LocalDateTime.now());
            notificationRepository.insert(n);
        } catch (Exception ignored) {
            // 通知失败不影响提醒标记
        }
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void remindDueSoon() {
        LocalDateTime now = LocalDateTime.now();
        QueryWrapper<Task> qw = new QueryWrapper<>();
        qw.eq("due_reminded", 0)
                .in("status", 1, 2)
                .isNotNull("plan_end_time")
                .gt("plan_end_time", now)
                .le("plan_end_time", now.plusHours(24));
        List<Task> list = taskRepository.selectList(qw);
        int cnt = 0;
        for (Task t : list) {
            String msg = "任务即将到期（24小时内），请及时处理：" + t.getTitle();
            if (t.getAssignee_id() != null) {
                notifyTask(t.getAssignee_id(), msg, "/home/task?type=assigned");
            }
            if (t.getCreator_id() != null && !t.getCreator_id().equals(t.getAssignee_id())) {
                notifyTask(t.getCreator_id(), msg, "/home/task?type=created");
            }
            t.setDue_reminded(1);
            taskRepository.updateById(t);
            cnt++;
        }
        if (cnt > 0) {
            log.info("[任务到期提醒] 本次发送 {} 条到期前提醒", cnt);
        }
    }

    @Scheduled(cron = "0 0 9 * * ?")
    public void dailyTodoSummary() {
        List<Task> list = taskRepository.selectList(new QueryWrapper<Task>().in("status", 1, 2, 3));

        // 三个位置是待接受、进行中、待验收的数量
        Map<Long, int[]> stat = new HashMap<>();
        for (Task t : list) {
            Integer status = t.getStatus();
            if (status == null) {
                continue;
            }
            if (status == 1 && t.getAssignee_id() != null) {
                stat.computeIfAbsent(t.getAssignee_id(), k -> new int[3])[0]++;
            } else if (status == 2 && t.getAssignee_id() != null) {
                stat.computeIfAbsent(t.getAssignee_id(), k -> new int[3])[1]++;
            } else if (status == 3 && t.getCreator_id() != null) {
                stat.computeIfAbsent(t.getCreator_id(), k -> new int[3])[2]++;
            }
        }

        int sent = 0;
        for (Map.Entry<Long, int[]> entry : stat.entrySet()) {
            int[] v = entry.getValue();
            if (v[0] + v[1] + v[2] == 0) {
                continue;
            }
            String content = "今日待办：待接受 " + v[0] + " 项，进行中 " + v[1] + " 项，待验收 " + v[2] + " 项";
            notifyTask(entry.getKey(), content, "/home/task?type=assigned");
            sent++;
        }
        if (sent > 0) {
            log.info("[任务待办汇总] 本次向 {} 位用户发送待办汇总", sent);
        }
    }
}
