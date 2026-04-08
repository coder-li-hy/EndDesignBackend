package com.reggie.reg.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.reg.entity.Employee;
import com.reggie.reg.mapper.EmployeeMapper;
import com.reggie.reg.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {
    //注入employeeService服务对象
    @Autowired
    private EmployeeMapper employeeMapper;

}
