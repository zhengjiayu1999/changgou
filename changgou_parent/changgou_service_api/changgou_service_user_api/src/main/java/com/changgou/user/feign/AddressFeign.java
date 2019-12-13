package com.changgou.user.feign;

import com.changgou.user.pojo.Address;
import entity.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * @author Steven
 * @description com.changgou.user.feign
 */
@FeignClient(name = "user")
@RequestMapping("address")
public interface AddressFeign {
    /****
     * 用户收件地址
     */
    @GetMapping(value = "/user/list")
    public Result<List<Address>> list();
}