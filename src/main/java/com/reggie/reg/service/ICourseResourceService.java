package com.reggie.reg.service;

import com.reggie.reg.entity.CourseResource;
import com.baomidou.mybatisplus.extension.service.IService;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * 课程资源表 服务类
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
public interface ICourseResourceService extends IService<CourseResource> {

    /**
     * 上传课程资源（自动创建审核记录）
     */
    public boolean saveResource(CourseResource resource);


}
