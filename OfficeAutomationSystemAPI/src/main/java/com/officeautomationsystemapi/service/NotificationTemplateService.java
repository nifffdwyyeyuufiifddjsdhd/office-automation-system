package com.officeautomationsystemapi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.officeautomationsystemapi.entity.NotificationTemplates;
import com.officeautomationsystemapi.repository.NotificationTemplatesRepository;
import com.officeautomationsystemapi.utilt.TemplateRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

// 发通知前先取模板标题正文，查不到就用代码里的默认文案
@Service
public class NotificationTemplateService {

    @Autowired
    private NotificationTemplatesRepository notificationTemplatesRepository;

    public record Rendered(String title, String content) {
    }

    public Rendered render(String templateCode, Map<String, String> vars, String fallbackTitle, String fallbackContent) {
        try {
            NotificationTemplates template = notificationTemplatesRepository.selectOne(
                    new QueryWrapper<NotificationTemplates>()
                            .eq("template_code", templateCode)
                            .eq("status", 1)
                            .last("LIMIT 1"));
            if (template == null) {
                return new Rendered(fallbackTitle, fallbackContent);
            }
            String title = TemplateRenderer.render(template.getTitle_template(), vars);
            String content = TemplateRenderer.render(template.getContent_template(), vars);
            return new Rendered(blank(title) ? fallbackTitle : title, blank(content) ? fallbackContent : content);
        } catch (Exception e) {
            // 模板表异常不影响业务，直接用默认文案
            return new Rendered(fallbackTitle, fallbackContent);
        }
    }

    private boolean blank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
