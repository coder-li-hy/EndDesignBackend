package com.reggie.reg.service;

import com.reggie.reg.entity.CourseQa;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 问答互动表 服务类
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
public interface ICourseQaService extends IService<CourseQa> {
    public boolean submitQa(CourseQa qa);

}
