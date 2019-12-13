package com.changgou.goods.feign;

import com.changgou.goods.pojo.Goods;
import com.changgou.goods.pojo.Spu;
import entity.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(name = "goods")
@RequestMapping("spu")
public interface SpuFeign {

    /**
     * 根据id查询spu与sku列表
     * @param id
     * @return
     */
    @GetMapping("goods/{id}")
    public Result<Goods> findById(@PathVariable("id") Long id);

    @GetMapping("/{id}")
    public Result<Spu> findSpuById(@PathVariable("id") Long id);
}
