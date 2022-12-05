package com.cyl.payment.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * @author cyl
 * @date 2022-11-26 16:26
 * @description
 */
@Data
// 设置链式操作，setter 方法会返回对应的实体
@Accessors(chain = true)
public class R {

    // 响应码
    private Integer code;

    // 响应消息
    private String message;

    // 响应体
    private Map<String, Object> data = new HashMap<>();

    public static R ok() {
        R r = new R();

        r.setCode(0);
        r.setMessage("success");
        return r;
    }

    public static R error() {
        R r = new R();

        r.setCode(-1);
        r.setMessage("fail");
        return r;
    }

    public R data(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

}
