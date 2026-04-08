package com.reggie.reg.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.reg.common.R;
import com.reggie.reg.entity.Employee;
import com.reggie.reg.service.EmployeeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;


@Slf4j
@RestController
@RequestMapping("/employee")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;

    /**
     * 员工登录方法
     *
     * @param request  将登录员工的id放在session中一份
     * @param employee
     * @return
     */
    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee) {
        // 1.将用户提交的密码进行md5加密处理
        // 1.1获取由前端传来的用户密码
        String password = employee.getPassword();
        // 1.2将用户传来的密码加密为md5密码传给原来的password
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        // 2.根据页面提交的用户名username查询数据库
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        // 2.1设置查询条件
        queryWrapper.eq(Employee::getUsername, employee.getUsername());
        //2.2 进行查询
        Employee emp = employeeService.getOne(queryWrapper);
        //说明，因为数据库中已经指定username唯一，所以这里使用getOne方法

        if (emp == null || emp.getStatus() == 0) {
            //如果没有查询到这一用户, 或者该用户已经被禁用
            //返回登陆失败数据
            return R.error("登陆失败");
        }
        //4.进行密码比对，如果不一致则返回登录失败结果
        if (!emp.getPassword().equals(password)) {
            //如果密码比对不一致
            //返回失败信息
            return R.error("登陆失败");
        }

        //5.查看员工状态，如果员工为已经禁用状态，返回员工已禁用结果
        if (emp.getStatus() == 0) {
            return R.error("登陆");
        }

        // 6.登陆成功，将员工id存入session并返回登陆成功结果
        request.getSession().setAttribute("employee", emp.getId());

        return R.success(emp);
    }

    /**
     * 用户请求退出的方法
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request) {
        //清理session中保存的id
        request.getSession().removeAttribute("employee");
        return R.success("退出成功");
    }

//    @GetMapping("/page")
//    public void aaa() {
//    }

    /**
     * 新增员工操作
     * 将前端提交的json新增用户数据封装到employee对象中，并调用service保存
     *
     * @param employee
     * @param request  获取当前请求信息
     */
    @PostMapping
    public R<String> save(@RequestBody Employee employee, HttpServletRequest request) {
//        //0.判断数据库中有咩有重复的账号
//        Employee one = employeeService.lambdaQuery().eq(Employee::getUsername, employee.getUsername()).one();
//        if (one != null) {
//            log.info("新增员工失败，员工账号已存在");
//            return R.error("新增员工失败，员工账号已存在");
//        }
        //1.补充员工信息
        //1.1 获取当前登录操作员工id信息
        // 获取当前登录用户id
        Long id = (Long) request.getSession().getAttribute("employee");
        // 对密码进行md5加密
        employee.setPassword(DigestUtils.md5DigestAsHex(new String("123456").getBytes()));
        // 设置创建用户
        employee.setCreateUser(id);
        // 设置创建时间
        //employee.setCreateTime(LocalDateTime.now());
        // 设置更新时间
        //employee.setUpdateTime(LocalDateTime.now());
        // 设置更新用户
        employee.setUpdateUser(id);
        log.info("新增员工，员工信息：{}", employee.toString());
//        try {
        employeeService.save(employee);
//        } catch (Exception e) {
//            log.info("新增员工失败，员工信息：{}", employee.toString());
//            return R.error("新增员工失败");
//        }
        //2.保存员工信息
        //3.返回新增成功结果

        return R.success("新增员工成功");
    }

    /**
     * 所有员工信息分页查询展示结果
     * 默认page和pagesize是1和10
     *
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(@RequestParam Integer page, @RequestParam Integer pageSize, String name) {
        // 1.构造分页构造器
        log.info("page={},pageSize={},name={}", page, pageSize, name);
        // 2.构造分页构造器
        Page pageInfo = new Page(page, pageSize);
        // 3.构造条件构造器
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        // 3.1添加过滤条件，若用户名不为空，则根据用户名进行查询
        // 如果不为空，使用name字段进行模糊查询
        queryWrapper.like(StringUtils.isNotEmpty(name), Employee::getName, name).orderByDesc(Employee::getUpdateTime);
        // 使用queryWrapper进行分页查询，将分页信息传入employeeService的page方法
        employeeService.page(pageInfo, queryWrapper);
        // 返回查询结果
        return R.success(pageInfo);
    }


    /**
     * 对于修改员工的所有请求封装的通用方法
     * 根据id修改员工信息
     *
     * @param employee 封装传过来的修改信息
     * @param request  获取当前请求的信息
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody Employee employee, HttpServletRequest request) {
        log.info(employee.toString());
        long id1 = Thread.currentThread().getId();
        log.info("线程id:{}", id1);
        //补充更新信息
        //获取当前登录用户id
        Long id = (Long) request.getSession().getAttribute("employee");
        employee.setUpdateUser(id);

        //补充更新时间
        employee.setUpdateTime(LocalDateTime.now());

        try {
            employeeService.updateById(employee);
        } catch (Exception e) {
            return R.error("修改员工信息失败");
        }
        return R.success("修改员工信息成功");
    }

    /**
     * 根据路径参数id查询回显用户数据
     * 由R<Employee>返回
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<Employee> getById(@PathVariable Long id) {
        log.info("根据id查询员工信息，id={}", id);

        Employee employee = employeeService.getById(id);

        if (employee != null) {
            return R.success(employee);
        }

        return R.error("没有查询到对应员工信息");
    }
}
