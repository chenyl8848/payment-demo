package com.cyl.payment.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author cyl
 * @date 2022-11-27 17:00
 * @description
 */
@AllArgsConstructor
@Getter
public enum OrderStatus {

    /**
     * 未支付
     */
    NOTPAY("未支付"),

    /**
     * 支付成功
     */
    SUCCESS("支付成功"),

    /**
     * 超时已关闭
     */
    CLOSED("超时已关闭"),

    /**
     * 用户已取消
     */
    CANCEL("用户已取消"),

    /**
     * 退款中
     */
    REFUND_PROCESSING("退款中"),

    /**
     * 退款成功
     */
    REFUND_SUCCESS("退款成功"),

    /**
     * 退款异常
     */
    REFUND_ABNORMAL("退款异常");

    /**
     * 订单状态类型
     */
    private final String type;

}
