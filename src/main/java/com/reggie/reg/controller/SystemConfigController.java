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
     * 获取系统配置列表
     * @param configKey 配置键，可选参数
     * @param description 配置描述，可选参数
     * @param page 页码，默认值为1
     * @param size 每页大小，默认值为10
     * @return 返回分页后的系统配置列表
     */
    @GetMapping("/admin/config")
    public R<Page<SystemConfig>> listConfigs(
            @RequestParam(required = false) String configKey,    // 配置键，非必需参数
            @RequestParam(required = false) String description,    // 配置描述，非必需参数
            @RequestParam(defaultValue = "1") Integer page,        // 页码，默认为1
            @RequestParam(defaultValue = "10") Integer size) {     // 每页大小，默认为10

        // 创建查询条件构造器
        LambdaQueryWrapper<SystemConfig> query = new LambdaQueryWrapper<>();
        // 添加配置键的模糊查询条件，如果configKey不为空
        query.like(StringUtils.isNotBlank(configKey), SystemConfig::getConfigKey, configKey);
        // 添加配置描述的模糊查询条件，如果description不为空
        query.like(StringUtils.isNotBlank(description), SystemConfig::getDescription, description);
        // 按配置ID降序排序
        query.orderByDesc(SystemConfig::getConfigId);

        // 执行分页查询
        Page<SystemConfig> result = configService.page(new Page<>(page, size), query);
        // 返回查询结果
        return R.success(result);
    }


    /**
     * 添加系统配置接口
     * @param config 系统配置信息，通过请求体传递
     * @return 返回操作结果，R对象包含操作状态和消息
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
     * 更新系统配置接口
     * @param configId 配置ID，路径变量
     * @param dto 系统配置数据传输对象，包含要更新的配置信息
     * @return 返回操作结果，成功或失败信息
     */
    @PutMapping("/admin/config/{configId}")
    public R<String> updateConfig(@PathVariable Integer configId, @RequestBody SystemConfig dto) {
        // 根据ID查询配置信息
        SystemConfig config = configService.getById(configId);
        // 如果配置不存在，返回错误信息
        if (config == null) return R.error("配置不存在");

        // 只更新允许的字段
        config.setConfigValue(dto.getConfigValue());  // 设置配置值
        config.setDescription(dto.getDescription()); // 设置配置描述
        // update方法只更新非null字段，执行更新操作
        configService.updateById(config);

        // 返回成功信息
        return R.success("更新成功");
    }


    /**
     * 删除系统配置接口
     * @param configId 配置ID，路径变量
     * @return 返回操作结果，R对象封装了操作状态和消息
     */
    @DeleteMapping("/admin/config/{configId}")
    public R<String> deleteConfig(@PathVariable Integer configId) {
        // 根据配置ID查询配置信息
        SystemConfig config = configService.getById(configId);
        // 如果配置不存在，返回错误信息
        if (config == null) return R.error("配置不存在");

        // 保护系统核心配置，检查是否为系统核心配置
        if (isSystemConfig(config.getConfigKey())) {
            // 如果是系统核心配置，返回错误信息
            return R.error("系统核心配置不能删除");
        }

        // 执行删除操作
        configService.removeById(configId);
        // 返回成功信息
        return R.success("删除成功");
    }


    /**
     * 刷新缓存接口
     * 该接口用于处理缓存刷新请求，目前处于开发阶段
     * @return 返回操作结果信息，当前返回"开发中"提示
     */
    @PostMapping("/refresh")
    public R<String> refreshCache() {
        // TODO: 后续集成 Redis 时实现
        // 当前仅返回成功状态和"开发中"提示信息
        return R.success("开发中");
    }


    /**
     * 判断给定的配置键是否为系统配置项
     * @param configKey 需要检查的配置键
     * @return 如果是系统配置项返回true，否则返回false
     */
    private boolean isSystemConfig(String configKey) {
        // 创建一个包含所有系统配置键的列表，并检查给定的配置键是否在该列表中
        return Arrays.asList("GLOBAL_SELECTION_SWITCH", "SUBMISSION_LOCK", "MAINTENANCE_MODE")
                .contains(configKey);
    }

}
