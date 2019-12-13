package com.changgou.pay.service.impl;

import com.alibaba.fastjson.JSON;
import com.changgou.pay.service.WeixinPayService;
import com.github.wxpay.sdk.WXPayUtil;
import entity.HttpClient;
import entity.TokenDecode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付实现
 * @author Steven
 * @description com.changgou.pay.service.impl
 */
@Service
public class WeixinPayServiceImpl implements WeixinPayService {
    @Value("${weixin.appid}")
    private String appid;
    @Value("${weixin.partner}")
    private String partner;
    @Value("${weixin.notifyurl}")
    private String notifyurl;
    @Value("${weixin.partnerkey}")
    private String partnerkey;
    /*@Override
    public Map createNative(String out_trade_no, String total_fee) {
        Map map = new HashMap();
        try {
            //1、包装微信接口需要的参数
            Map param = new HashMap();
            param.put("appid", appid);  //公众号ID
            param.put("mch_id", partner);  //商户号
            param.put("nonce_str", WXPayUtil.generateNonceStr()); //随机字符串
            param.put("body", "畅购");  //商品描述，扫码后用户看到的商品信息
            param.put("out_trade_no", out_trade_no); //订单号
            param.put("total_fee", total_fee);  //订单总金额，单位为分
            param.put("spbill_create_ip", "127.0.0.1");  //终端IP，只要附合ip地址规范，可以随意写
            param.put("notify_url", notifyurl);  //回调地址
            param.put("trade_type", "NATIVE");  //交易类型，NATIVE 扫码支付
            //2、生成xml，通过httpClient发送请求得到数据
            String xmlParam = WXPayUtil.generateSignedXml(param, partnerkey);
            System.out.println("正在调起统一下单接口，请求参数:" + xmlParam);
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");//请求微信url获取结果
            httpClient.setHttps(true);//是否是https的协议
            httpClient.setXmlParam(xmlParam);//发送xml数据
            httpClient.post();//执行post请求
            //3、解析结果
            String xmlResult = httpClient.getContent();//获取结果
            System.out.println("调起统一下单接口成功，返回结果：" + xmlResult);
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xmlResult);
            map.put("code_url", resultMap.get("code_url"));//支付地址
            map.put("total_fee", total_fee);//总金额
            map.put("out_trade_no",out_trade_no);//订单号
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }*/

    @Override
    public Map createNative(Map<String, String> paramMap) {
        Map map = new HashMap();
        try {
            //1、包装微信接口需要的参数
            Map param = new HashMap();
            param.put("appid", appid);  //公众号ID
            param.put("mch_id", partner);  //商户号
            param.put("nonce_str", WXPayUtil.generateNonceStr()); //随机字符串
            param.put("body", "畅购");  //商品描述，扫码后用户看到的商品信息
            param.put("out_trade_no", paramMap.get("out_trade_no")); //订单号
            param.put("total_fee", paramMap.get("total_fee"));  //订单总金额，单位为分
            param.put("spbill_create_ip", "127.0.0.1");  //终端IP，只要附合ip地址规范，可以随意写
            param.put("notify_url", notifyurl);  //回调地址
            param.put("trade_type", "NATIVE");  //交易类型，NATIVE 扫码支付

            //附加参数
            Map<String, String> attachMap = new HashMap<String,String>();
            attachMap.put("exchange", paramMap.get("exchange"));
            attachMap.put("routingKey", paramMap.get("routingKey"));
            attachMap.put("username", paramMap.get("username"));
            param.put("attach", JSON.toJSONString(attachMap));


            //2、生成xml，通过httpClient发送请求得到数据
            String xmlParam = WXPayUtil.generateSignedXml(param, partnerkey);
            System.out.println("正在调起统一下单接口，请求参数:" + xmlParam);
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            httpClient.setHttps(true);
            httpClient.setXmlParam(xmlParam);
            httpClient.post();
            //3、解析结果
            String xmlResult = httpClient.getContent();
            System.out.println("调起统一下单接口成功，返回结果：" + xmlResult);
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xmlResult);
            map.put("code_url", resultMap.get("code_url"));//支付地址
            map.put("total_fee", paramMap.get("total_fee"));//总金额
            map.put("out_trade_no",paramMap.get("out_trade_no"));//订单号
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    @Override
    public Map queryPayStatus(String out_trade_no) {
        Map map = new HashMap();
        try {
            //1、包装微信接口需要的参数
            Map <String,String> param = new HashMap();
            param.put("appid", appid);  //公众号ID
            param.put("mch_id", partner);  //商户号
            param.put("nonce_str", WXPayUtil.generateNonceStr()); //随机字符串
            param.put("out_trade_no", out_trade_no); //订单号

            //2、生成xml，通过httpClient发送请求得到数据
            String xmlParam = WXPayUtil.generateSignedXml(param, partnerkey);
            System.out.println("正在调起统一下单接口，请求参数:" + xmlParam);
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/orderquery");//请求微信url获取结果
            httpClient.setHttps(true);//是否是https的协议
            httpClient.setXmlParam(xmlParam);//发送xml数据
            httpClient.post();//执行post请求
            //3、解析结果
            String xmlResult = httpClient.getContent();//获取结果
            System.out.println("调起统一下单接口成功，返回结果：" + xmlResult);
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xmlResult);
            return resultMap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



}
