package com.cyl.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cyl.payment.entity.OrderInfo;
import com.cyl.payment.entity.RefundInfo;
import com.cyl.payment.mapper.RefundInfoMapper;
import com.cyl.payment.service.OrderInfoService;
import com.cyl.payment.service.RefundInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cyl.payment.util.OrderNoUtil;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class RefundInfoServiceImpl extends ServiceImpl<RefundInfoMapper, RefundInfo> implements RefundInfoService {

    @Autowired
    private OrderInfoService orderInfoService;

    @Override
    public RefundInfo createRefundByOrderNo(String orderNo, String reason) {
        // 根据订单号获取订单信息
        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);

        if (Objects.isNull(orderInfo)) {
            throw new RuntimeException("订单不存在");
        }

        // 根据订单号生成退款订单
        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setOrderNo(orderNo);
        refundInfo.setRefundNo(OrderNoUtil.getRefundNo());
        refundInfo.setTotalFee(orderInfo.getTotalFee());
        refundInfo.setRefund(orderInfo.getTotalFee());
        refundInfo.setReason(reason);

        baseMapper.insert(refundInfo);

        return refundInfo;
    }

    @Override
    public void updateRefund(String content) {
        // 将 gson 字符串转成 map
        Gson gson = new Gson();
        Map<String, Object> resultMap = gson.fromJson(content, HashMap.class);

        // 微信支付退款单编号
        String refundId = (String) resultMap.get("refund_id");
        String outRefundNo = (String) resultMap.get("out_refund_no");
        // 退款状态 -- 查询退款和申请退款中的参数
        String status = (String) resultMap.get("status");
        // 退款状态 -- 退款回调中的参数
        String refundStatus = (String) resultMap.get("refund_status");

        // 根据退款单号更新退款单
        // 设置要修改的退款单
        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setRefundId(refundId);

        LambdaQueryWrapper<RefundInfo> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(RefundInfo::getRefundNo, outRefundNo);

        if (!StringUtils.isEmpty(status)) {
            refundInfo.setRefundStatus(status);
            refundInfo.setContentReturn(content);
        }
        if (!StringUtils.isEmpty(refundStatus)) {
            refundInfo.setRefundStatus(refundStatus);
            refundInfo.setContentNotify(content);
        }

        baseMapper.update(refundInfo, queryWrapper);
    }
}
