package com.cyl.payment.controller;

import com.cyl.payment.entity.Product;
import com.cyl.payment.service.ProductService;
import com.cyl.payment.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * @author cyl
 * @date 2022-11-25 22:50
 * @description
 */
@Api(tags = "商品管理")
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @ApiOperation(value = "测试接口")
    @GetMapping("test")
    public R test() {
        return R.ok().data("data", "hello world").data("now", new Date());
    }

    @ApiOperation("商品列表")
    @GetMapping("/list")
    public R list() {
        List<Product> list = productService.list();
        return R.ok().data("productList", list);

    }
}
