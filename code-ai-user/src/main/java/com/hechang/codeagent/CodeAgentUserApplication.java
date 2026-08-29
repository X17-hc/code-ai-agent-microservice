package com.hechang.codeagent;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@MapperScan("com.hechang.codeagent.mapper")
@ComponentScan("com.hechang")
@EnableDubbo
public class CodeAgentUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeAgentUserApplication.class, args);
    }

}
