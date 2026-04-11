package com.reggie.reg.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.reg.common.R;
import com.reggie.reg.entity.SystemConfig;
import com.reggie.reg.service.ISystemConfigService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * <p>
 * 系统配置表 前端控制器
 * </p>
 *
 * @author lihy
 * @since 2026-04-08
 */
@RestController
@RequiredArgsConstructor
public class SystemConfigController {
    private final ISystemConfigService configService;

    /**
     * 分页查询配置列表
     * GET /api/admin/config?configKey=&description=&page=1&size=10
     */
    @GetMapping("/admin/config")
    public R<Page<SystemConfig>> listConfigs(
            @RequestParam(required = false) String configKey,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        LambdaQueryWrapper<SystemConfig> query = new LambdaQueryWrapper<>();
        query.like(StringUtils.isNotBlank(configKey), SystemConfig::getConfigKey, configKey);
        query.like(StringUtils.isNotBlank(description), SystemConfig::getDescription, description);
        query.orderByDesc(SystemConfig::getConfigId);

        Page<SystemConfig> result = configService.page(new Page<>(page, size), query);
        return R.success(result);
    }

    /**
     * 添加配置
     * POST /api/admin/config
     */
    @PostMapping("/admin/config")
    public R<String> addConfig(@RequestBody SystemConfig config) {
        // 校验配置键唯一
        if (configService.count(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, config.getConfigKey())) > 0) {
            return R.error("配置键已存在");
        }
        configService.save(config);
        return R.success("添加成功");
    }

    /**
     * 更新配置（只允许修改 configValue 和 description）
     * PUT /api/admin/config/{configId}
     */
    @PutMapping("/admin/config/{configId}")
    public R<String> updateConfig(@PathVariable Integer configId, @RequestBody SystemConfig dto) {
        SystemConfig config = configService.getById(configId);
        if (config == null) return R.error("配置不存在");

        // 只更新允许的字段
        config.setConfigValue(dto.getConfigValue());
        config.setDescription(dto.getDescription());
        // update方法只更新非null字段
        configService.updateById(config);

        return R.success("更新成功");
    }

    /**
     * 删除配置
     * DELETE /api/admin/config/{configId}
     */
    @DeleteMapping("/admin/config/{configId}")
    public R<String> deleteConfig(@PathVariable Integer configId) {
        SystemConfig config = configService.getById(configId);
        if (config == null) return R.error("配置不存在");

        // 保护系统核心配置
        if (isSystemConfig(config.getConfigKey())) {
            return R.error("系统核心配置不能删除");
        }

        configService.removeById(configId);
        return R.success("删除成功");
    }

    /**
     * 刷新缓存（预留接口）
     * POST /api/admin/config/refresh
     */
    @PostMapping("/refresh")
    public R<String> refreshCache() {
        // TODO: 后续集成 Redis 时实现
        return R.success("开发中");
    }

    /**
     * 判断是否为系统核心配置
     */
    private boolean isSystemConfig(String configKey) {
        return Arrays.asList("GLOBAL_SELECTION_SWITCH", "SUBMISSION_LOCK", "MAINTENANCE_MODE")
                .contains(configKey);
    }

}
