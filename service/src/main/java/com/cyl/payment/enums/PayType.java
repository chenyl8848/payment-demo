package com.cyl.payment.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author cyl
 * @date 2022-11-27 16:58
 * @description
 */
@AllArgsConstructor
@Getter
public enum PayType {

    /**
     * 微信
     */
    WXPAY("微信"),

    /**
     * 支付宝
     */
    ALIPAY("支付宝");

    /**
     * 支付类型
     */
    private final String type;


}
