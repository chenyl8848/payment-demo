package com.cyl.payment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cyl.payment.entity.PaymentInfo;
import com.cyl.payment.enums.PayType;
import com.cyl.payment.mapper.PaymentInfoMapper;
import com.cyl.payment.service.PaymentInfoService;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
@Slf4j
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

    @Override
    public void createPaymentInfo(String plainText) {
        log.info("记录支付日志");

        Gson gson = new Gson();
        HashMap hashMap = gson.fromJson(plainText, HashMap.class);

        // 订单号
        String orderNo = (String) hashMap.get("out_trade_no");
        // 微信支付订单号
        String transactionId = (String) hashMap.get("transaction_id");
        // 交易类型
        String tradeType = (String) hashMap.get("trade_type");
        // 交易状态
        String tradeState = (String) hashMap.get("trade_state");

        HashMap<String, Object> amount = (HashMap<String, Object>) hashMap.get("amount");
        // 用户支付金额
        Integer payerTotal = ((Double) amount.get("payer_total")).intValue();

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setPaymentType(PayType.WXPAY.getType());
        paymentInfo.setTransactionId(transactionId);
        paymentInfo.setTradeType(tradeType);
        paymentInfo.setTradeState(tradeState);
        paymentInfo.setPayerTotal(payerTotal);
        paymentInfo.setContent(plainText);

        baseMapper.insert(paymentInfo);

    }
}
