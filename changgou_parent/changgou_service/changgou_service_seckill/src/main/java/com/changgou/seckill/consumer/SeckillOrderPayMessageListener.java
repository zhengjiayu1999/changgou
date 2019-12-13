package com.changgou.seckill.consumer;

import com.alibaba.fastjson.JSON;
import com.changgou.seckill.service.SeckillOrderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;


    /**
     * 秒杀支付消息监听器
     * @author Steven
     * @description com.changgou.seckill.consumer
     */
    @Component
    public class SeckillOrderPayMessageListener {

        @Autowired
        private SeckillOrderService seckillOrderService;

        @RabbitListener(queues = "${mq.pay.queue.seckillorder}")
        public void payListener(String msg) {
            //将数据转成Map
            Map<String, String> result = JSON.parseObject(msg, Map.class);
            System.out.println("收到消息，参数为：" + result);
            //return_code=SUCCESS
            String return_code = result.get("return_code");

            //返回状态码 result_code=SUCCESS/FAIL，修改订单状态
            if ("success".equalsIgnoreCase(return_code)) {
                //业务结果
                String result_code = result.get("result_code");
                //获取订单号
                String out_trade_no = result.get("out_trade_no");
                //交易流水号
                String transaction_id = result.get("transaction_id");

                //附加参数
                Map<String, String> attachMap = JSON.parseObject(result.get("attach"), Map.class);
                //用户名
                String username = attachMap.get("username");

                //业务结果-SUCCESS/FAIL，为success时，支付成功
                if ("success".equalsIgnoreCase(result_code)) {
                    //修改订单状态  out_trade_no
                    seckillOrderService.updatePayStatus(out_trade_no,transaction_id,username);
                } else {
                    //订单删除
                    seckillOrderService.closeOrder(username);
                }
            }
        }
    }
