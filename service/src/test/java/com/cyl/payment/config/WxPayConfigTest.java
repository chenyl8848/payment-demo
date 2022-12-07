package com.cyl.payment.config;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author cyl
 * @date 2022-11-27 11:04
 * @description
 */
@SpringBootTest
public class WxPayConfigTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(WxPayConfigTest.class);

    @Autowired
    private WxPayConfig wxPayConfig;

    @Test
    public void testGetPrivateKey() {
//        PrivateKey privateKey = wxPayConfig.getPrivateKey(wxPayConfig.getPrivateKeyPath());
//        LOGGER.info("商户私钥信息:{}", privateKey);
    }
}
