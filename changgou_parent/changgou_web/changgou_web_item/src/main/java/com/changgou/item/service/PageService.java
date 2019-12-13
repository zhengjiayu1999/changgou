package com.changgou.item.service;

public interface PageService {

    /**
     * 根据商品Id生成静态页面
     * @param spuId
     */
    public void createPageHtml(Long spuId);
}
