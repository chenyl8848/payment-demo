package com.cyl.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author cyl
 * @date 2022-11-25 22:58
 * @description
 */
@Configuration
// 启用swagger2
@EnableSwagger2
public class Swagger2Config {

    @Bean
    public Docket docket() {

        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(new ApiInfoBuilder()
                        .title("支付案例接口文档")
                        .description("微信/支付宝支付Demo")
                        .version("1.0.0")
                        .build());
    }
}
