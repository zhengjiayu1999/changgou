package com.changgou.seckill.task;

import com.changgou.seckill.dao.SeckillGoodsMapper;
import com.changgou.seckill.dao.SeckillOrderMapper;
import com.changgou.seckill.pojo.SeckillGoods;
import com.changgou.seckill.pojo.SeckillOrder;
import com.changgou.seckill.utils.SeckillStatus;
import entity.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class MultiThreadingCreateOrder {
    @Autowired
    private SeckillOrderMapper seckillOrderMapper;
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private IdWorker idWorker;

    @Async
    public void createOrder() {
        SeckillStatus seckillStatus = (SeckillStatus) redisTemplate.boundListOps("SeckillOrderQueue").rightPop();

        //库存精确显示
        Long count = redisTemplate.boundHashOps("SeckillGoodsCount").increment(seckillStatus.getGoodsId(), -1);

        if (seckillStatus != null) {
            //获取时间区间
            String time = seckillStatus.getTime();
            //用户名登录
            String username = seckillStatus.getUsername();
            //用户抢购商品
            Long goodsId = seckillStatus.getGoodsId();
            //1.获取商品数据
            SeckillGoods goods = (SeckillGoods) redisTemplate.boundHashOps("SeckillGoods_" + time).get(goodsId);

            //2.如果没有库存，则直接抛出异常
            if (goods == null || count <= 0) {
                throw new RuntimeException("你来晚了一步，商品已抢购一空!");
            }
            //3.如果有库存，则创建秒杀商品订单
            SeckillOrder seckillOrder = new SeckillOrder();
            seckillOrder.setId(idWorker.nextId());
            seckillOrder.setSeckillId(goodsId);
            seckillOrder.setMoney(goods.getCostPrice());
            seckillOrder.setUserId(username);
            seckillOrder.setCreateTime(new Date());
            seckillOrder.setStatus("0");
            //将秒杀订单存入到Redis中
            redisTemplate.boundHashOps("SeckillOrder").put(username, seckillOrder);

            //4.扣减库存
            goods.setStockCount(count.intValue());

            //5.判断当前商品是否还有库存
            if (count <= 0) {
                //并且将商品数据同步到MySQL中
                seckillGoodsMapper.updateByPrimaryKeySelective(goods);
                //如果没有库存,则清空Redis缓存中该商品
                redisTemplate.boundHashOps("SeckillGoods_" + time).delete(goodsId);
            } else {
                //如果有库存，则将扣减库存后的goods重新放入redis
                redisTemplate.boundHashOps("SeckillGoods_" + time).put(goodsId, goods);
            }

            //抢单成功 将1状态（排队中）变成2状态（等待支付）
            seckillStatus.setStatus(2);
            seckillStatus.setOrderId(seckillOrder.getId());
            seckillStatus.setMoney(new Float(seckillOrder.getMoney()));//记录金额
            //更新redis状态
            redisTemplate.boundHashOps("UserQueueStatus").put(username,seckillStatus);
        }
    }
}
