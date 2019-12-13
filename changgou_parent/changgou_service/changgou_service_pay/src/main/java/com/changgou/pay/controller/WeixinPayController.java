package com.changgou.pay.controller;

import com.alibaba.fastjson.JSON;
import com.changgou.pay.service.WeixinPayService;
import com.github.wxpay.sdk.WXPayUtil;
import entity.Result;
import entity.StatusCode;
import entity.TokenDecode;
import org.apache.commons.io.IOUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/weixin/pay")
@CrossOrigin
public class WeixinPayController {
    @Autowired
    private WeixinPayService weixinPayService;

    @Value("${mq.pay.exchange.order}")
    private String exchange;
    @Value("${mq.pay.queue.order}")
    private String queue;
    @Value("${mq.pay.routing.key}")
    private String routing;

    @Value("${mq.pay.queue.seckillorder}")
    private String seckillorder;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    /***
     * 创建二维码
     * @param paramMap {
     *                 out_trade_no 订单号,
     *                 total_fee 金额(分),
     *                 exchange 交换机,
     *                 routingKey 路由Key
     *                 }
     * @return
     */
    @RequestMapping(value = "/create/native")
    public Result createNative(@RequestParam Map<String,String> paramMap){
        paramMap.put("username", TokenDecode.getUserInfo().get("username"));
        Map<String,String> resultMap = weixinPayService.createNative(paramMap);
        return new Result(true, StatusCode.OK,"创建二维码预付订单成功！",resultMap);
    }

    @RequestMapping("createSeckillQueue")
    public String createSeckillQueue(){
        //发送MQ消息-用于创建秒杀队列
        rabbitTemplate.convertAndSend(exchange,seckillorder, "{'flag':'ok'}");
        return "ok";
    }

    /***
     * 查询支付状态
     */
    @GetMapping(value = "/status/query")
    public Result queryStatus(String out_trade_no){
        Map<String,String> resultMap = weixinPayService.queryPayStatus(out_trade_no);
        return new Result(true,StatusCode.OK,"查询状态成功！",resultMap);
    }

    /***
     * 支付回调
     * 支付完成后，微信会把相关支付结果及用户信息通过数据流的形式发送给商户，
     * 商户需要接收处理，并按文档规范返回应答
     * @param request
     * @return
     */
    @RequestMapping(value = "/notify/url")
    public String notifyUrl(HttpServletRequest request){
        try {
            //1.读取支付回调数据
            InputStream inStream = request.getInputStream();
            //2.使用Apache IOUtils把输入转换成字符
            String result = IOUtils.toString(inStream,"UTF-8");
            //将xml字符串转换成Map结构
            Map<String, String> map = WXPayUtil.xmlToMap(result);
            System.out.println("从微信接收的消息"+map);


            //读取附加消息-交换机与队列
            Map<String,String> attachMap = JSON.parseObject(map.get("attach"), Map.class);
            exchange = attachMap.get("exchange");
            routing = attachMap.get("routingKey");
            //发送MQ消息
            rabbitTemplate.convertAndSend(exchange,routing, JSON.toJSONString(map));
            System.out.println("发送队列消息成功");

            //3.包装响应数据设置
            Map respMap = new HashMap();
            respMap.put("return_code","SUCCESS");
            respMap.put("return_msg","OK");
            return WXPayUtil.mapToXml(respMap);
        } catch (Exception e) {
            e.printStackTrace();
            //记录错误日志
        }
        return null;
    }
}
