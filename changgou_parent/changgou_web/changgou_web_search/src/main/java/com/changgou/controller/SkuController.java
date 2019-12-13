package com.changgou.controller;

import com.changgou.search.feign.SkuFeign;
import entity.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping("search")
public class SkuController {
    @Autowired
    SkuFeign skuFeign;

    /**
     * 搜索商品
     * 注意此处的@GetMapping()要添加list的url请求，不然会跟SkuFeign中的请求url冲突
     */
    @GetMapping("list")
    public String search(@RequestParam(required = false) Map searchMap, Model model){
        Map result = skuFeign.search(searchMap);
        model.addAttribute("result",result);
        //返回查询条件
        model.addAttribute("searchMap",searchMap);
        //返回url
        model.addAttribute("url",getUrl(searchMap));

        //返回分页参数
        Page page=new Page(
                new Long(result.get("total").toString()),
                new Integer(result.get("pageNum").toString()),
                new Integer(result.get("pageSize").toString())
        );
        model.addAttribute("page",page);
        return "search";
    }

    /**将获取的参数Map集合装换url
     * @param searchMap
     * @return
     */
    private String getUrl(Map<String,String> searchMap){
        // /search/list?category=笔记本&brand=华为
        String url="/search/list";
        //有参数
        if(searchMap!=null){
            url+="?";
            for (String key : searchMap.keySet()) {
                //如果是排序的参数，不拼接到url上，便于下次换种方式排序
                if(key.indexOf("sort") > -1|| "pageNum".equals(key)){
                    continue;
                }
                url+=key+"="+searchMap.get(key)+"&";
            }
            url=url.substring(0,url.length()-1);
        }
        return url;
    }
}
