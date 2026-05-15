package com.reggie.reg.common;

import com.reggie.reg.service.ICourseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CourseStatusScheduler {

    @Autowired
    private ICourseInfoService courseService;

    // 每 2 分钟执行一次（生产环境建议 1~5 分钟）
    @Scheduled(cron = "0 */2 * * * ?")
    public void updateExpiredCourses() {
        log.info("开始执行课程状态更新任务...");
        try {
            int count = courseService.updateExpiredToEnded();
            log.info("课程状态更新完成，共更新 {} 条记录", count);
        } catch (Exception e) {
            log.error("课程状态更新任务异常", e);
        }
    }
}