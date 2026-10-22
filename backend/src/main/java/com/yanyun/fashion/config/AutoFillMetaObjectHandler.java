package com.yanyun.fashion.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 公共字段自动填充：create_time / update_time。
 *
 * @author yanyun backend
 */
@Component
public class AutoFillMetaObjectHandler implements MetaObjectHandler {

    private static final String CREATE_TIME = "createTime";

    private static final String UPDATE_TIME = "updateTime";

    @Override
    public void insertFill(MetaObject metaObject) {
        Date now = new Date();
        if (metaObject.hasSetter(CREATE_TIME) && getFieldValByName(CREATE_TIME, metaObject) == null) {
            setFieldValByName(CREATE_TIME, now, metaObject);
        }
        if (metaObject.hasSetter(UPDATE_TIME) && getFieldValByName(UPDATE_TIME, metaObject) == null) {
            setFieldValByName(UPDATE_TIME, now, metaObject);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        if (metaObject.hasSetter(UPDATE_TIME)) {
            setFieldValByName(UPDATE_TIME, new Date(), metaObject);
        }
    }
}
