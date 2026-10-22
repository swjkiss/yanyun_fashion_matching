package com.yanyun.fashion.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Jackson 配置：时间格式 yyyy-MM-dd HH:mm:ss、时区 Asia/Shanghai、null 字段保留。
 *
 * @author yanyun backend
 */
@Configuration
public class JacksonConfig {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private static final String TIME_ZONE = "Asia/Shanghai";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer yanyunJacksonCustomizer() {
        return builder -> {
            builder.timeZone(TimeZone.getTimeZone(TIME_ZONE));
            builder.simpleDateFormat(DATE_PATTERN);
            builder.serializationInclusion(JsonInclude.Include.ALWAYS);
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        };
    }
}
