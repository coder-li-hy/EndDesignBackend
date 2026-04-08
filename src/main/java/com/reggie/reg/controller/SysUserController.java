package com.reggie.reg.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.reg.common.R;
import com.reggie.reg.dto.SysUserDto;
import com.reggie.reg.entity.SysUser;
import com.reggie.reg.service.ISysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
                request.getSession().setAttribute("sys_user", sys.getUserId());
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
    public SysUserController(final ISysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }
}
