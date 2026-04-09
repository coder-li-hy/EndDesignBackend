package com.reggie.reg.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.reg.common.R;
import com.reggie.reg.dto.BatchDeleteDTO;
import com.reggie.reg.dto.CourseDTO;
import com.reggie.reg.dto.ImportResult;
import com.reggie.reg.dto.SysUserDto;
import com.reggie.reg.entity.CourseInfo;
import com.reggie.reg.entity.SysUser;
import com.reggie.reg.service.ISysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class SysUserController {
    private static final Logger log = LoggerFactory.getLogger(SysUserController.class);
    private final ISysUserService sysUserService;

    @PostMapping({"/auth/login"})
    public R<SysUser> login(HttpServletRequest request, @RequestBody(required = true) SysUserDto sysUserDto) {
        String password = sysUserDto.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(SysUser::getUsername, sysUserDto.getUsername());
        SysUser sys = (SysUser)this.sysUserService.getOne(queryWrapper);
        log.info("{}", sys);
        if (sys != null && !sys.getStatus().equals("DISABLED")) {
            if (!sys.getPasswordHash().equals(password)) {
                log.info("登陆失败 用户名或密码错误");
                return R.error("NOT_LOGIN");
            } else {
                // 存一份当前登录用户的Id
                request.getSession().setAttribute("sys_user", sys.getUserId());
                // 存一份当前登录用户的角色
                request.getSession().setAttribute("sys_user_role", sys.getRole());
                return R.success(sys);
            }
        } else {
            log.info("登陆失败 用户名或密码错误");
            return R.error("NOT_LOGIN");
        }
    }

    @GetMapping({"/auth/info"})
    public R<SysUser> getUserInfo(HttpServletRequest request) {
        Integer id = (Integer)request.getSession().getAttribute("sys_user");
        log.info("当前登录用户id为：{}", id);
        SysUser sysUser = (SysUser)this.sysUserService.getById(id);
        // 清空密码不传给前端
        sysUser.setPasswordHash((String)null);
        return R.success(sysUser);
    }

    @PutMapping({"/auth/password"})
    public R<String> updatePassword(HttpServletRequest request, @RequestBody SysUserDto sysUserDto) {
        Integer id = (Integer)request.getSession().getAttribute("sys_user");
        log.info("当前登录用户id为：{}", id);
        SysUser sysUser = (SysUser)this.sysUserService.getById(id);
        String password = DigestUtils.md5DigestAsHex(sysUserDto.getOldpassword().getBytes());
        if (!sysUser.getPasswordHash().equals(password)) {
            return R.error("密码错误");
        } else {
            String newPassword = DigestUtils.md5DigestAsHex(sysUserDto.getNewpassword().getBytes());
            sysUser.setPasswordHash(newPassword);
            this.sysUserService.updateById(sysUser);
            // 清空当前sesion
            request.getSession().invalidate();
            return R.success("密码修改成功");
        }
    }

    @PutMapping("/auth/profile")
    public R<String> updateProfile(HttpServletRequest request, @RequestBody SysUserDto dto) {
        Integer id = (Integer) request.getSession().getAttribute("sys_user");
        if (id == null) {
            return R.error("未登录");
        }

        SysUser sysUser = sysUserService.getById(id);
        if (sysUser == null) {
            return R.error("用户不存在");
        }

        // 只更新允许修改的字段
        if (dto.getEmail() != null) {
            sysUser.setEmail(dto.getEmail());
        }

        if (dto.getPhone() != null) {
            sysUser.setPhone(dto.getPhone());
        }

        sysUserService.updateById(sysUser);
        return R.success("信息更新成功");
    }

    // 后端：/auth/logout
    @PostMapping("/auth/logout")
    public R<String> logout(HttpServletRequest request) {
        // 销毁 Session（关键！否则别人拿到 Cookie 还能用）
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return R.success("退出成功");
    }


    // 以下为未验证区域

    /**
     * 分页查询用户列表（支持搜索过滤）
     * GET /admin/users?username=&role=&status=&page=1&size=10
     */
    @GetMapping("/admin/users/page")
    public R<Page<SysUser>> listUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<>();

        // 动态条件拼接
        query.like(username != null && !username.isEmpty(), SysUser::getUsername, username);
        query.eq(role != null && !role.isEmpty(), SysUser::getRole, role);
        query.eq(status != null && !status.isEmpty(), SysUser::getStatus, status);

        // 排除密码字段，按创建时间倒序
        query.select(SysUser::getUserId, SysUser::getUsername, SysUser::getRole,
                        SysUser::getStatus, SysUser::getEmail, SysUser::getPhone,
                        SysUser::getCreateTime)
                .orderByDesc(SysUser::getCreateTime);

        Page<SysUser> result = sysUserService.page(new Page<>(page, size), query);
        return R.success(result);
    }

    /**
     * 根据 ID 查询用户详情（编辑时回填数据）
     * GET /admin/users/{userId}
     */
    @GetMapping("/admin/{userId}")
    public R<SysUser> getUserById(@PathVariable Integer userId) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return R.error("用户不存在");
        }
        // 不返回密码
        user.setPasswordHash(null);
        return R.success(user);
    }

    /**
     * 添加新用户
     * POST /admin/users
     */
    @PostMapping("/admin/users")
    public R<String> addUser(@RequestBody SysUserDto dto) {
        // 1. 校验用户名是否已存在
        LambdaQueryWrapper<SysUser> checkQuery = new LambdaQueryWrapper<>();
        checkQuery.eq(SysUser::getUsername, dto.getUsername());
        if (sysUserService.count(checkQuery) > 0) {
            return R.error("用户名已存在");
        }

        // 2. 构建用户实体
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        // MD5 加密密码（默认密码或用户输入）
        String rawPwd = dto.getPassword() != null ? dto.getPassword() : "123456";
        user.setPasswordHash(DigestUtils.md5DigestAsHex(rawPwd.getBytes()));
        user.setRole(dto.getRole());
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());

        // 3. 保存
        boolean saved = sysUserService.save(user);
        return saved ? R.success("用户添加成功") : R.error("添加失败");
    }

    /**
     * 更新用户信息
     * PUT /admin/users/{userId}
     */
    @PutMapping("/admin/users/{userId}")
    public R<String> updateUser(@PathVariable Integer userId, @RequestBody SysUserDto dto) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return R.error("用户不存在");
        }

        // 保护管理员账号不能被修改关键信息
        if ("admin".equals(user.getUsername())) {
            // 只允许修改邮箱和手机
            if (dto.getEmail() != null) user.setEmail(dto.getEmail());
            if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        } else {
            // 普通用户可以修改更多信息
            if (dto.getRole() != null) user.setRole(dto.getRole());
            if (dto.getStatus() != null) user.setStatus(dto.getStatus());
            if (dto.getEmail() != null) user.setEmail(dto.getEmail());
            if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        }

        boolean updated = sysUserService.updateById(user);
        return updated ? R.success("用户更新成功") : R.error("更新失败");
    }

    // ========== 状态管理接口 ==========
    // 内部 DTO：只接收 status 字段
    @Data
    private static class StatusDTO {
        private String status;  // ACTIVE 或 DISABLED
    }
    /**
     * 修改用户状态（禁用/启用）
     * PUT /admin/users/{userId}/status
     */
    @PutMapping("/admin/users/{userId}/status")
    public R<String> updateStatus(@PathVariable Integer userId, @RequestBody StatusDTO dto) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return R.error("用户不存在");
        }

        // 保护管理员账号不能被禁用
        if ("admin".equals(user.getUsername())) {
            return R.error("管理员账号不能禁用");
        }

        user.setStatus(dto.getStatus());
        boolean updated = sysUserService.updateById(user);

        String action = "ACTIVE".equals(dto.getStatus()) ? "启用" : "禁用";
        return updated ? R.success("用户已" + action) : R.error("操作失败");
    }

    /**
     * 删除单个用户
     * DELETE /admin/users/{userId}
     */
    @DeleteMapping("/admin/users/{userId}")
    public R<String> deleteUser(@PathVariable Integer userId) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return R.error("用户不存在");
        }

        // 保护管理员账号
        if ("admin".equals(user.getUsername())) {
            return R.error("管理员账号不能删除");
        }

        boolean deleted = sysUserService.removeById(userId);
        return deleted ? R.success("删除成功") : R.error("删除失败");
    }

    /**
     * 批量删除用户
     * DELETE /admin/users/batch
     */
    @DeleteMapping("/admin/users/batch")
    public R<String> batchDelete(@RequestBody BatchDeleteDTO dto) {
        List<Integer> userIds = dto.getUserIds();
        if (userIds == null || userIds.isEmpty()) {
            return R.error("请选择要删除的用户");
        }

        // 过滤掉管理员账号
        List<Integer> safeIds = userIds.stream()
                .filter(id -> {
                    // 检查删除过程中有无管理员账号
                    SysUser user = sysUserService.getById(id);
                    return user != null && !"admin".equals(user.getUsername());
                })
                .collect(Collectors.toList());

        if (safeIds.isEmpty()) {
            return R.error("无法删除管理员账号");
        }

        boolean deleted = sysUserService.removeBatchByIds(safeIds);
        return deleted ? R.success("批量删除成功") : R.error("删除失败");
    }

    // ========== 密码管理接口 ==========

    /**
     * 重置用户密码
     * POST /admin/users/{userId}/reset-pwd
     */
    @PostMapping("/admin/users/{userId}/reset-pwd")
    public R<String> resetPassword(@PathVariable Integer userId) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return R.error("用户不存在");
        }

        // 保护管理员账号
        if ("admin".equals(user.getUsername())) {
            return R.error("管理员密码不能重置");
        }

        // 重置为默认密码 123456（MD5 加密）
        String defaultPwd = DigestUtils.md5DigestAsHex("123456".getBytes());
        user.setPasswordHash(defaultPwd);

        boolean updated = sysUserService.updateById(user);
        return updated ? R.success("密码已重置为 123456") : R.error("重置失败");
    }

    // ========== 批量导入接口 ==========

    /**
     * 下载导入模板
     * GET /admin/users/template
     */
    @GetMapping("/admin/users/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.malformations-officedocument.spreadsheet.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=用户导入模板.xlsx");

        // 使用 EasyExcel 生成模板（需要添加依赖）
        // 如果不想用 EasyExcel，可以返回一个静态文件
        // 这里先返回空文件占位，你可以根据需求实现
        response.getOutputStream().close();
    }

    /**
     * 批量导入用户
     * POST /admin/users/import
     */
    @PostMapping("/admin/users/import")
    public R<ImportResult> importUsers(@RequestParam("file") MultipartFile file) {
        // 校验文件
        if (file.isEmpty()) {
            return R.error("文件不能为空");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !(fileName.endsWith(".xlsx") || fileName.endsWith(".xls"))) {
            return R.error("只支持 Excel 文件");
        }

        try {
            // 使用 EasyExcel 解析文件（需要添加依赖）
            // 这里先返回模拟结果，你需要根据实际需求实现解析逻辑
            ImportResult result = new ImportResult();
            result.setSuccessCount(0);
            result.setFailCount(0);
            result.setFailMessages(List.of("请实现文件解析逻辑"));

            return R.success(result);

        } catch (Exception e) {
            log.error("导入失败", e);
            return R.error("导入失败：" + e.getMessage());
        }
    }

    // ========== 权限校验拦截器（可选增强） ==========

    /**
     * 校验当前操作者是否为管理员
     * 私有方法，供其他接口调用
     */
    private R<String> checkAdminRole(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return R.error("NOT_LOGIN");
        }

        String role = (String) session.getAttribute("sys_user_role");
        if (!"ADMIN".equals(role)) {
            return R.error("无权访问");
        }
        return null;  // 校验通过
    }
    public SysUserController(final ISysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }
}
