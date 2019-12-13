package com.changgou.order.service.impl;

import com.changgou.goods.feign.SkuFeign;
import com.changgou.goods.feign.SpuFeign;
import com.changgou.goods.pojo.Sku;
import com.changgou.goods.pojo.Spu;
import com.changgou.order.pojo.OrderItem;
import com.changgou.order.service.CartService;
import entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SkuFeign skuFeign;

    @Autowired
    private SpuFeign spuFeign;

    @Override
    public void add(Integer num, Long skuId, String username) {
        if(num <= 0){
            //如果商品数量小于等于0，删除当前商品
            redisTemplate.boundHashOps("Cart_" + username).delete(skuId);
            return;
        }
        //查询SKU
        Result<Sku> resultSku = skuFeign.findSkuById(skuId);
        if (resultSku != null && resultSku.isFlag()) {
            //获取SKU
            Sku sku = resultSku.getData();
            //获取SPU
            Spu spu = spuFeign.findSpuById(sku.getSpuId()).getData();

            //将SKU转换成OrderItem
            OrderItem orderItem = new OrderItem();
            orderItem.setSpuId(sku.getSpuId());
            orderItem.setSkuId(sku.getId());
            orderItem.setName(sku.getName());
            orderItem.setPrice(sku.getPrice());
            orderItem.setNum(num);
            orderItem.setMoney(num * orderItem.getPrice());       //单价*数量
            orderItem.setPayMoney(num * orderItem.getPrice());    //实付金额
            orderItem.setImage(sku.getImage());
            orderItem.setWeight(sku.getWeight() * num);           //重量=单个重量*数量

            //分类ID设置
            orderItem.setCategoryId1(spu.getCategory1Id());
            orderItem.setCategoryId2(spu.getCategory2Id());
            orderItem.setCategoryId3(spu.getCategory3Id());

            /******
             * 购物车数据存入到Redis
             * namespace = Cart_[username]
             * key=skuId
             * value=OrderItem
             */
            redisTemplate.boundHashOps("Cart_" + username).put(skuId, orderItem);
        }
    }

    @Override
    public List<OrderItem> list(String username) {
        //查询所有购物车数据
        List<OrderItem> orderItems = redisTemplate.boundHashOps("Cart_"+username).values();
        return orderItems;
    }
}
