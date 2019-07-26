package com.fis.web;

import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import tk.mybatis.spring.annotation.MapperScan;

@MapperScan(basePackages = {"com.fis.web"})
@SpringBootApplication(scanBasePackages = {"com.fis.web"})
@EnableDubbo
public class Application extends SpringBootServletInitializer {

    /**
     * 外部容器启动的时候，需要重写此方法
     *
     * @param application
     * @return
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
