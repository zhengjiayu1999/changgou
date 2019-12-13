package com.changgou.order.consumer;

import com.alibaba.fastjson.JSON;
import com.changgou.order.service.OrderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 订单消息监听器
 * @author Steven
 * @description com.changgou.order.consumer
 */
@Component
public class OrderPayMessageListener {
    @Autowired
    private OrderService orderService;

    @RabbitListener(queues = "${mq.pay.queue.order}")
    public void payListener(String msg) {
        //将数据转成Map
        Map<String, String> result = JSON.parseObject(msg, Map.class);

        //return_code=SUCCESS
        String return_code = result.get("return_code");

        //返回状态码 result_code=SUCCESS/FAIL，修改订单状态
        if (return_code.equalsIgnoreCase("success")) {
            //业务结果
            String result_code = result.get("result_code");
            //获取订单号
            String out_trade_no = result.get("out_trade_no");
            //获取交易流水号
            String transaction_id = result.get("transaction_id");
            //业务结果-SUCCESS/FAIL，为success时，支付成功
            if (result_code.equalsIgnoreCase("success")) {
                //修改订单状态  out_trade_no
                orderService.updateStatus(out_trade_no,transaction_id);
                System.out.println("修改状态完成");
            } else {
                //订单删除
                orderService.deleteOrder(out_trade_no);
            }
        }
    }
}