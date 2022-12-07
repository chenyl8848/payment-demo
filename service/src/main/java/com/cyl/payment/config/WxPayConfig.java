package com.cyl.payment.config;

import com.wechat.pay.contrib.apache.httpclient.WechatPayHttpClientBuilder;
import com.wechat.pay.contrib.apache.httpclient.auth.PrivateKeySigner;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Credentials;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Validator;
import com.wechat.pay.contrib.apache.httpclient.cert.CertificatesManager;
import com.wechat.pay.contrib.apache.httpclient.util.PemUtil;
import lombok.Data;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;

/**
 * @author cyl
 * @date 2022-11-27 10:38
 * @description wx 支付配置文件
 */
@Configuration
// 读取配置文件
@PropertySource(value = "classpath:wxpay.properties")
// 读取配置文件的前缀
@ConfigurationProperties(prefix = "wxpay")
@Data
public class WxPayConfig {

    /**
     * 商户号
     */
    private String mchId;

    /**
     * 商户 API 证书序列号
     */
    private String mchSerialNo;

    /**
     * 商户私钥文件
     */
    private String privateKeyPath;

    /**
     * API V3 密钥
     */
    private String apiV3Key;

    /**
     * appid
     */
    private String appid;

    /**
     * 微信服务器地址
     */
    private String domain;

    /**
     * 接收结果通知地址
     */
    private String notifyDomain;

    /**
     * APIv2密钥
     */
    private String partnerKey;

    /**
     * 获取商户私钥文件
     *
     * @param privateKeyPath 私钥文件路径
     * @return
     */
    private PrivateKey getPrivateKey(String privateKeyPath) {
        try {
            return PemUtil.loadPrivateKey(new FileInputStream(privateKeyPath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("私钥文件不存在", e);
        }
    }

    /**
     * 获取签名验证器
     *
     * @return
     */
    @Bean
    public Verifier getVerifier() {
        // 获取证书管理器实例
        CertificatesManager certificatesManager = CertificatesManager.getInstance();

        // 获取商户私钥
        PrivateKey privateKey = getPrivateKey(privateKeyPath);

        // 创建私钥签名对象
        PrivateKeySigner privateKeySigner = new PrivateKeySigner(mchSerialNo, privateKey);

        // 创建身份认证对象
        WechatPay2Credentials wechatPay2Credentials = new WechatPay2Credentials(mchId, privateKeySigner);

        // 向证书管理器增加需要自动更新平台证书的商户信息
        try {
            certificatesManager.putMerchant(mchId, wechatPay2Credentials, apiV3Key.getBytes(StandardCharsets.UTF_8));

            // ... 若有多个商户号，可继续调用putMerchant添加商户信息

            // 从证书管理器中获取verifier
            return certificatesManager.getVerifier(mchId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("获取签名验证器失败", e);
        }

    }

    /**
     * 获取 http 请求对象
     *
     * @param verifier
     * @return
     */
    @Bean
    public CloseableHttpClient getWXPayClient(Verifier verifier) {

        // 获取商户私钥
        PrivateKey privateKey = getPrivateKey(privateKeyPath);

        WechatPayHttpClientBuilder builder = WechatPayHttpClientBuilder.create()
                .withMerchant(mchId, mchSerialNo, privateKey)
                .withValidator(new WechatPay2Validator(verifier));
        // ... 接下来，你仍然可以通过builder设置各种参数，来配置你的HttpClient

        // 通过WechatPayHttpClientBuilder构造的HttpClient，会自动的处理签名和验签，并进行证书自动更新
        CloseableHttpClient httpClient = builder.build();

        return httpClient;
    }

}
