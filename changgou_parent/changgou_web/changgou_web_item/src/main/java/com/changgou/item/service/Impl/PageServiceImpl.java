package com.changgou.item.service.Impl;

import com.changgou.goods.feign.CategoryFeign;
import com.changgou.goods.feign.SpuFeign;
import com.changgou.goods.pojo.Goods;
import com.changgou.goods.pojo.Spu;
import com.changgou.item.service.PageService;
import entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@Service
public class PageServiceImpl implements PageService {
    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private SpuFeign spuFeign;

    @Autowired
    private CategoryFeign categoryFeign;

    @Value("${pagepath}")
    private String pagepath;

    /**
     * 构建数据模型
     * @param spuId
     * @return
     */
    private Map<String,Object> buildDateModel(Long spuId){
        //构建数据
        Map<String,Object> dataMap=new HashMap<>();
        //获取spu和sku列表
        Result<Goods> result=spuFeign.findById(spuId);
        Spu spu=result.getData().getSpu();

        //获取分类信息
        dataMap.put("category1",categoryFeign.findById(spu.getCategory1Id()).getData());
        dataMap.put("category2",categoryFeign.findById(spu.getCategory2Id()).getData());
        dataMap.put("category3",categoryFeign.findById(spu.getCategory3Id()).getData());

        if (spu.getImages()!=null){
            dataMap.put("imageList",spu.getImages().split(","));
        }

        //获取规格数据
        dataMap.put("skuList",result.getData().getSkuList());
        return dataMap;
    }

    @Override
    public void createPageHtml(Long spuId) {
        try {
            //1、创建上下文对象-context = new Context()
            Context context=new Context();
            //构建数据模型
            Map<String,Object> map=this.buildDateModel(spuId);
            context.setVariables(map);
            //2、识别并生成静态页目录-File.exists
            File dir = new File(pagepath);
            if(!dir.exists()){
                //创建级联目录
                dir.mkdirs();
            }
            //3、创建静态页面文件对象=dest=new File(dir,spuId+".html")
            File dest=new File(dir,spuId+".html");
            //4、创建文件输出对象-out = new PrintWriter(dest,"UTF-8")
            PrintWriter out=new PrintWriter(dest,"UTF-8");
            //5、输出文件-templateEngine.process(模板名称,内容上下文,输出对象)
            templateEngine.process("item",context,out);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
