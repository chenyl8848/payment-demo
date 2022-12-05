package com.cyl.payment.service.impl;

import com.cyl.payment.entity.Product;
import com.cyl.payment.mapper.ProductMapper;
import com.cyl.payment.service.ProductService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

}
