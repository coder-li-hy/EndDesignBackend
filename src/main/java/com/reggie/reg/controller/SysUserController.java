package com.reggie.reg.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.reggie.reg.common.R;
import com.reggie.reg.dto.*;
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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class SysUserController {
    private static final Logger log = LoggerFactory.getLogger(SysUserController.class);
    private final ISysUserService sysUserService;

    private static final String DEFAULT_PASSWORD_HASH = "e10adc3949ba59abbe56e057f20f883e";

    /**
     * 处理用户登录请求的接口
     * @param request HTTP请求对象，用于获取会话信息
     * @param sysUserDto 前端传来的用户登录信息，包含用户名和密码
     * @return 返回登录结果，成功返回用户信息，失败返回错误信息
     */
    @PostMapping({"/auth/login"})
    public R<SysUser> login(HttpServletRequest request, @RequestBody(required = true) SysUserDto sysUserDto) {
        // 获取前端传来的密码
        String password = sysUserDto.getPassword();
        // 对密码进行加密，使用MD5算法
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        // 构造lambda条件查询条件，用于查询用户信息
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper();
        // 设置查询条件：根据用户名查询
        queryWrapper.eq(SysUser::getUsername, sysUserDto.getUsername());
        // 执行查询，获取用户信息
        SysUser sys = (SysUser) this.sysUserService.getOne(queryWrapper);
        // 打印用户信息到日志
        log.info("{}", sys);
        // 如果没有找到该用户 用户没有被禁用
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


    /**
     * 获取当前登录用户信息的接口
     * @param request HTTP请求对象，用于获取会话信息
     * @return 返回一个包含用户信息的R对象，R是统一响应封装类
     */
    @GetMapping({"/auth/info"})
    public R<SysUser> getUserInfo(HttpServletRequest request) {
        // 获取当前用户的id
        Integer id = (Integer) request.getSession().getAttribute("sys_user");
        log.info("当前登录用户id为：{}", id);
        SysUser sysUser = (SysUser) this.sysUserService.getById(id);
        // 清空密码不传给前端
        sysUser.setPasswordHash((String) null);
        return R.success(sysUser);
    }


    /**
     * 处理用户修改密码的请求
     * @param request HTTP请求对象，用于获取会话信息
     * @param sysUserDto 包含用户旧密码和新密码的数据传输对象
     * @return 返回操作结果，R类型封装了操作状态和消息
     */
    @PutMapping({"/auth/password"})
    public R<String> updatePassword(HttpServletRequest request, @RequestBody SysUserDto sysUserDto) {
        // 获取当前登录用户ID
        Integer id = (Integer) request.getSession().getAttribute("sys_user");
        log.info("当前登录用户id为：{}", id);
        // 从数据库查询当前登录用户的相关信息
        SysUser sysUser = (SysUser) this.sysUserService.getById(id);
        // 获取前端传入的用户的旧密码 并进行md5加密
        String password = DigestUtils.md5DigestAsHex(sysUserDto.getOldpassword().getBytes());
        // 判断前端传入的旧密码是否正确
        if (!sysUser.getPasswordHash().equals(password)) {
            return R.error("密码错误");
        } else {
            // 如果判断成功 即旧密码正确
            // 将用户端的新密码进行md5加密
            String newPassword = DigestUtils.md5DigestAsHex(sysUserDto.getNewpassword().getBytes());
            // 设置用户的新密码（md5模式）
            sysUser.setPasswordHash(newPassword);
            // 更新当前用户信息
            this.sysUserService.updateById(sysUser);
            // 清空当前sesion
            request.getSession().invalidate();
            return R.success("密码修改成功");
        }
    }



    /**
     * 更新用户资料接口
     * @param request HTTP请求对象，用于获取当前登录用户信息
     * @param dto 包含用户更新信息的DTO对象
     * @return 返回操作结果，成功或失败信息
     */
    @PutMapping("/auth/profile")
    public R<String> updateProfile(HttpServletRequest request, @RequestBody SysUserDto dto) {
        // 获取当前登录用户ID，从session中获取
        Integer id = (Integer) request.getSession().getAttribute("sys_user");
        // 如果没有找到用户则返回未登录错误
        if (id == null) {
            return R.error("未登录");
        }

        // 如果该登录id不存在
        SysUser sysUser = sysUserService.getById(id);
        if (sysUser == null) {
            return R.error("用户不存在");
        }

        // 只更新允许修改的字段
        if (dto.getEmail() != null) {
            sysUser.setEmail(dto.getEmail());
        }

        // 如果前端传入了电话号码则进行更新
        if (dto.getPhone() != null) {
            sysUser.setPhone(dto.getPhone());
        }

        // 更新用户信息
        sysUserService.updateById(sysUser);
        return R.success("信息更新成功");
    }


    /**
     * 处理用户登出请求的POST接口
     * @param request HTTP请求对象，用于获取Session信息
     * @return 返回一个响应对象R，包含"退出成功"的提示信息
     */
    @PostMapping("/auth/logout")
    public R<String> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false); // 获取当前Session，如果不存在则不创建
        if (session != null) { // 检查Session是否存在
            session.invalidate(); // 使Session失效，实现用户登出
        }
        return R.success("退出成功"); // 返回成功响应，提示用户已成功退出
    }



    /**
     * 分页查询用户列表接口
     * @param username 用户名（可选参数）
     * @param role 角色（可选参数）
     * @param status 状态（可选参数）
     * @param page 页码（默认值为1）
     * @param size 每页大小（默认值为10）
     * @return 返回分页查询结果，包含用户列表数据
     */
    @GetMapping("/admin/users/page")
    public R<Page<SysUser>> listUsers(
            @RequestParam(required = false) String username,  // 用户名参数，非必需
            @RequestParam(required = false) String role,      // 角色参数，非必需
            @RequestParam(required = false) String status,    // 状态参数，非必需
            @RequestParam(defaultValue = "1") Integer page,    // 页码参数，默认值为1
            @RequestParam(defaultValue = "10") Integer size) { // 每页大小参数，默认值为10

        // 创建Lambda查询包装器，用于构建数据库查询条件
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<>();

        // 动态条件拼接：根据参数是否为空来决定是否添加查询条件
        // 使用lambda表达式指定查询字段和条件值
        query.like(username != null && !username.isEmpty(), SysUser::getUsername, username);
        query.eq(role != null && !role.isEmpty(), SysUser::getRole, role);
        query.eq(status != null && !status.isEmpty(), SysUser::getStatus, status);

        // 排除密码字段，按创建时间倒序
        query.select(SysUser::getUserId, SysUser::getUsername, SysUser::getRole,
                        SysUser::getStatus, SysUser::getEmail, SysUser::getPhone,
                        SysUser::getCreateTime)
                .orderByDesc(SysUser::getCreateTime);

        // 执行分页查询 page由baomidou提供
        Page<SysUser> result = sysUserService.page(new Page<>(page, size), query);
        return R.success(result);
    }


    /**
     * 根据用户ID获取用户信息
     * @param userId 用户ID
     * @return 返回用户信息，不包含密码
     */
    @GetMapping("/admin/{userId}")
    public R<SysUser> getUserById(@PathVariable Integer userId) {
        // 调用服务层方法根据ID查询用户
        SysUser user = sysUserService.getById(userId);
        // 根据id查询用户
        if (user == null) {
            // 如果用户不存在，返回错误信息
            return R.error("用户不存在");
        }
        // 不返回密码，将密码哈希值设为null
        user.setPasswordHash(null);
        // 返回成功响应和用户信息
        return R.success(user);
    }


    /**
     * 管理员添加用户接口
     * @param dto 包含用户信息的DTO对象
     * @return 返回操作结果，成功或失败信息
     */
    @PostMapping("/admin/users")
    public R<String> addUser(@RequestBody SysUserDto dto) {
        // 1. 校验用户名是否已存在
        LambdaQueryWrapper<SysUser> checkQuery = new LambdaQueryWrapper<>();
        // 构造查询条件 根据用户名查询 判断是否已经存在 保证用户名的唯一性
        checkQuery.eq(SysUser::getUsername, dto.getUsername());
        if (sysUserService.count(checkQuery) > 0) {
            return R.error("用户名已存在");
        }

        // 2. 构建用户实体 方便一次性添加
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        // MD5 加密密码（默认密码或用户输入） 判断是否输入 如果用户没有输入 则默认123456
        String rawPwd = dto.getPassword() != null ? dto.getPassword() : "123456";
        // 设置加密之后的密码
        user.setPasswordHash(DigestUtils.md5DigestAsHex(rawPwd.getBytes()));
        user.setRole(dto.getRole());
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());

        // 3. 保存用户信息到数据库
        boolean saved = sysUserService.save(user);
        // 根据保存结果返回相应的成功或失败信息
        return saved ? R.success("用户添加成功") : R.error("添加失败");
    }


    /**
     * 更新用户信息的接口
     * @param userId 用户ID，路径变量
     * @param dto 包含用户更新信息的DTO对象
     * @return 返回操作结果，成功或失败信息
     */
    @PutMapping("/admin/users/{userId}")
    public R<String> updateUser(@PathVariable Integer userId, @RequestBody SysUserDto dto) {
        // 根据用户id从数据库查询用户信息
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
            // 实际上管理员并不能赋予其他用户管理员权限 前端没有给予权限 后端没有校验
            if (dto.getRole() != null) {
                // 实际上管理员并不能赋予其他用户管理员权限
                if (dto.getRole().equals("ADMIN")) {
                    return R.error("无权限赋予管理员权限");
                }
                user.setRole(dto.getRole());

            }
            // 更新用户状态
            if (dto.getStatus() != null) user.setStatus(dto.getStatus());
            // 更新用户邮件
            if (dto.getEmail() != null) user.setEmail(dto.getEmail());
            // 更新用户手机
            if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        }
        // 更新用户信息到数据库
        boolean updated = sysUserService.updateById(user);
        return updated ? R.success("用户更新成功") : R.error("更新失败");
    }

    // 该类的内部dto 接受用户的状态
    @Data
    private static class StatusDTO {
        private String status;  // ACTIVE 或 DISABLED
    }


    /**
     * 更新用户状态接口
     * @param userId 用户ID，路径变量
     * @param dto 包含状态信息的DTO对象
     * @return 返回操作结果，包含成功或失败信息
     */
    @PutMapping("/admin/users/{userId}/status")
    public R<String> updateStatus(@PathVariable Integer userId, @RequestBody StatusDTO dto) {
        // 根据用户ID查询用户信息
        SysUser user = sysUserService.getById(userId);
        // 如果用户不存在，返回错误信息
        if (user == null) {
            return R.error("用户不存在");
        }

        // 保护管理员账号不能被禁用
        if ("admin".equals(user.getUsername())) {
            return R.error("管理员账号不能禁用");
        }

        // 更新用户状态
        user.setStatus(dto.getStatus());
        // 执行更新操作
        boolean updated = sysUserService.updateById(user);

        // 根据状态值确定操作类型（启用/禁用）
        String action = "ACTIVE".equals(dto.getStatus()) ? "启用" : "禁用";
        // 根据更新结果返回相应的响应信息
        return updated ? R.success("用户已" + action) : R.error("操作失败");
    }


    /**
     * 删除用户接口
     * @param userId 用户ID，通过路径变量传递
     * @return 返回操作结果，R类型封装了操作状态和消息
     */
    @DeleteMapping("/admin/users/{userId}")
    public R<String> deleteUser(@PathVariable Integer userId) {
        // 根据用户ID查询用户信息
        SysUser user = sysUserService.getById(userId);
        // 如果用户不存在，返回错误信息
        if (user == null) {
            return R.error("用户不存在");
        }

        // 保护管理员账号，防止误删
        if ("admin".equals(user.getUsername())) {
            return R.error("管理员账号不能删除");
        }

        // 执行删除操作，并根据删除结果返回相应的响应
        boolean deleted = sysUserService.removeById(userId);
        return deleted ? R.success("删除成功") : R.error("删除失败");
    }


    /**
     * 批量删除用户接口
     * @param dto 包含要删除的用户ID列表的传输对象
     * @return 返回操作结果，成功或失败信息
     */
    @DeleteMapping("/admin/users/batch")
    public R<String> batchDelete(@RequestBody BatchDeleteDTO dto) {
        // 获取要删除的用户ID列表
        List<Integer> userIds = dto.getUserIds();
        // 检查用户ID列表是否为空
        if (userIds == null || userIds.isEmpty()) {
            return R.error("请选择要删除的用户 无法删除管理员账号");
        }

        // 过滤掉管理员账号
        // 使用Stream流处理用户ID列表，过滤掉管理员账号
        List<Integer> safeIds = userIds.stream() // 创建流
                .filter(id -> {
                    // 检查删除过程中有无管理员账号
                    // 根据ID获取用户信息
                    SysUser user = sysUserService.getById(id);
                    // 如果用户存在且用户身份不为admin则放过
                    return user != null && !"admin".equals(user.getUsername());
                })
                .collect(Collectors.toList()); // 将过滤后的结果收集为列表

        // 检查过滤后的ID列表是否为空（即全是管理员账号）
        if (safeIds.isEmpty()) {
            return R.error("无法删除管理员账号");
        }

        // 执行批量删除操作
        boolean deleted = sysUserService.removeBatchByIds(safeIds);
        // 根据删除结果返回相应的响应信息
        return deleted ? R.success("批量删除成功") : R.error("删除失败");
    }



    /**
     * 重置用户密码的接口
     * @param userId 用户ID，通过路径变量传递
     * @return 返回操作结果，成功或失败信息
     */
    @PostMapping("/admin/users/{userId}/reset-pwd")
    public R<String> resetPassword(@PathVariable Integer userId) {
        // 根据用户id从数据库提取相关用户信息
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return R.error("用户不存在");
        }

        // 保护管理员账号，不允许重置管理员密码
        if ("admin".equals(user.getUsername())) {
            return R.error("管理员密码不能重置");
        }

        // 重置为默认密码 123456（MD5 加密）
        String defaultPwd = DigestUtils.md5DigestAsHex("123456".getBytes());
        user.setPasswordHash(defaultPwd);

        // 更新用户 重置密码
        boolean updated = sysUserService.updateById(user);
        return updated ? R.success("密码已重置为 123456") : R.error("重置失败");
    }

    /**
     * 1. 下载导入模板
     * GET /admin/users/template
     */
    @GetMapping("/admin/users/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=用户导入模板.csv");

        // 使用 UTF-8 BOM，确保 Excel 打开中文不乱码
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8));

        // 写入表头
        writer.println("用户名,密码,角色,邮箱,手机号,状态");
        writer.println("teacher_test,123456,TEACHER,teacher@test.edu,13800138000,ACTIVE");
        writer.println("student_test,,STUDENT,student@test.edu,13900139000");
        writer.println("备注：密码和状态可选，空则使用默认值");

        writer.flush();
        writer.close();
    }

    /**
     * 2. 批量导入用户（CSV 格式）
     * POST /admin/users/import
     */
    @PostMapping("/admin/users/import")
    public R<Map<String, Object>> importUsers(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        try {
            // 1. 权限校验：只有管理员可导入
            Integer currentUserId = (Integer) request.getSession().getAttribute("sys_user");
            String currentUserRole = (String) request.getSession().getAttribute("sys_user_role");
            if (currentUserId == null || !"ADMIN".equals(currentUserRole)) {
                return R.error("无权操作");
            }

            // 2. 文件校验
            if (file.isEmpty()) {
                return R.error("文件不能为空");
            }
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
                return R.error("只支持 CSV 格式文件");
            }
            if (file.getSize() > 5 * 1024 * 1024) {  // 5MB
                return R.error("文件大小不能超过 5MB");
            }

            // 3. 解析 CSV
            List<UserImportDTO> importList = new ArrayList<>();
            try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                List<String[]> rows = reader.readAll();
                if (rows.size() < 2) {
                    return R.error("CSV 文件内容为空");
                }

                // 跳过表头（第 0 行），从第 1 行开始解析
                for (int i = 1; i < rows.size() && i <= 101; i++) {  // 最多导入 100 条
                    String[] row = rows.get(i);
                    if (row.length < 3) continue;  // 至少需要用户名、角色

                    UserImportDTO dto = new UserImportDTO();
                    dto.setUsername(row[0].trim());
                    dto.setPassword(row.length > 1 && row[1] != null ? row[1].trim() : "");
                    dto.setRole(row.length > 2 && row[2] != null ? row[2].trim() : "");
                    dto.setEmail(row.length > 3 && row[3] != null ? row[3].trim() : "");
                    dto.setPhone(row.length > 4 && row[4] != null ? row[4].trim() : "");
                    dto.setStatus(row.length > 5 && row[5] != null ? row[5].trim() : "");

                    // 空状态默认为 ACTIVE
                    if (dto.getStatus() == null || dto.getStatus().isEmpty()) {
                        dto.setStatus("ACTIVE");
                    }

                    importList.add(dto);
                }
            }

            if (importList.isEmpty()) {
                return R.error("未解析到有效数据");
            }

            // 4. 校验 + 批量插入
            int successCount = 0;
            int failCount = 0;
            List<String> errorMessages = new ArrayList<>();

            // 预查已存在的用户名（避免重复插入）
            List<String> usernames = importList.stream().map(UserImportDTO::getUsername).toList();
            Set<String> existingUsernames = new HashSet<>(
                    sysUserService.list(new LambdaQueryWrapper<SysUser>().in(SysUser::getUsername, usernames))
                            .stream().map(SysUser::getUsername).toList()
            );

            List<SysUser> insertList = new ArrayList<>();

            for (int i = 0; i < importList.size(); i++) {
                UserImportDTO dto = importList.get(i);
                int rowNum = i + 2;  // CSV 行号（跳过表头）

                // 4.1 单行校验
                String errorMsg = dto.validate();
                if (errorMsg != null) {
                    failCount++;
                    errorMessages.add("第" + rowNum + "行: " + errorMsg);
                    continue;
                }

                // 4.2 用户名唯一校验
                if (existingUsernames.contains(dto.getUsername())) {
                    failCount++;
                    errorMessages.add("第" + rowNum + "行: 用户名已存在: " + dto.getUsername());
                    continue;
                }

                // 4.3 构建用户实体
                SysUser user = new SysUser();
                user.setUsername(dto.getUsername());
                // 密码：空则用默认 123456 的 MD5，否则加密（简化：直接存默认，实际应加密）
                user.setPasswordHash(DEFAULT_PASSWORD_HASH);
                user.setRole(dto.getRole());
                user.setEmail(dto.getEmail().isEmpty() ? null : dto.getEmail());
                user.setPhone(dto.getPhone().isEmpty() ? null : dto.getPhone());
                user.setStatus(dto.getStatus());
                user.setCreateTime(LocalDateTime.now());

                insertList.add(user);
                existingUsernames.add(dto.getUsername());  // 避免本批次内重复
                successCount++;
            }

            // 4.4 批量插入
            if (!insertList.isEmpty()) {
                sysUserService.saveBatch(insertList);
            }

            // 5. 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            if (!errorMessages.isEmpty()) {
                result.put("errors", errorMessages.subList(0, Math.min(10, errorMessages.size())));  // 最多返回 10 条错误
            }

            return R.success(result);

        } catch (CsvException e) {
            System.err.println("CSV parse error: " + e.getMessage());
            return R.error("CSV 文件解析失败，请检查格式");
        } catch (Exception e) {
            System.err.println("Import users error: " + e.getMessage());
            return R.error("导入失败: " + e.getMessage());
        }
    }




    /**
     * 检查当前用户是否具有管理员角色的方法
     * @param request HttpServletRequest对象，用于获取session信息
     * @return 返回R<String>对象，如果校验失败则返回错误信息，校验通过则返回null
     */
    private R<String> checkAdminRole(HttpServletRequest request) {
        // 获取当前session 如果没有session则不重新创建
        HttpSession session = request.getSession(false);
        // 如果当前session未找到
        if (session == null) {
            return R.error("NOT_LOGIN");
        }
        // 获取当前用户角色
        String role = (String) session.getAttribute("sys_user_role");

        // 检查用户角色是否为管理员
        if (!"ADMIN".equals(role)) {
            return R.error("无权访问");
        }
        return null;  // 校验通过
    }


    public SysUserController(final ISysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }
}
