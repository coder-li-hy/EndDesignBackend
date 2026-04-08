package com.reggie.reg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.reg.entity.Employee;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {

}
