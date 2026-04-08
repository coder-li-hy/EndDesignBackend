package com.reggie.reg;

import com.reggie.reg.entity.Employee;
import com.reggie.reg.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class Demo {
    //将empservice接口对象注入
    @Autowired
    private EmployeeService employeeService;

    @Test
    public void testLogin() {
        // 2.根据页面提交的用户名username查询数据库
        List<Employee> employeeList = employeeService.lambdaQuery().select().eq(Employee::getUsername, "admin").list();
        //输出查询结果
        employeeList.stream().forEach(System.out::println);
    }
}
