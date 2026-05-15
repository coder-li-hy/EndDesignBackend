
package com.reggie.reg;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(
        exclude = {RedisAutoConfiguration.class}
)
@MapperScan(
        basePackages = {"com.reggie.reg.mapper"}
)
@ServletComponentScan
@EnableTransactionManagement
@EnableCaching
@EnableScheduling
public class ReggieApplication {
    private static final Logger log = LoggerFactory.getLogger(ReggieApplication.class);

    public ReggieApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(ReggieApplication.class, args);
        log.info("项目启动成功。。。");
    }
}