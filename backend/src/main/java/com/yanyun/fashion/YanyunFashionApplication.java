package com.yanyun.fashion;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 燕云十六声 · 古风时装搭配 服务端启动类。
 *
 * <p>服务地址：http://127.0.0.1:8080/jeecg-boot</p>
 *
 * @author yanyun backend
 */
@SpringBootApplication
@EnableTransactionManagement
@MapperScan("com.yanyun.fashion.mapper")
public class YanyunFashionApplication {

    public static void main(String[] args) {
        SpringApplication.run(YanyunFashionApplication.class, args);
    }
}
