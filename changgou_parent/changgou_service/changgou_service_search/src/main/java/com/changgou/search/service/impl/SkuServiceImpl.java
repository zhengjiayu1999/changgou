package com.changgou.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.changgou.goods.feign.SkuFeign;
import com.changgou.goods.pojo.Sku;
import com.changgou.search.dao.SkuEsMapper;
import com.changgou.search.pojo.SkuInfo;
import com.changgou.search.service.SkuService;
import entity.Result;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 商品信息业务逻辑实现
 * @author Steven
 * @version 1.0
 * @description com.changgou.search.service.impl
 * @date 2019-9-6
 */
@Service
public class SkuServiceImpl implements SkuService {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private SkuFeign skuFeign;
    @Autowired
    private SkuEsMapper skuEsMapper;

    @Override
    public void importSku() {
        //先通过feign查询正常的sku列表-{name:steven}
        Result<List<Sku>> result = skuFeign.findByStatus("1");
        //数据转换-{name:steven}-先把List转换成json串，再把json串转成List
        List<SkuInfo> skuInfos = JSON.parseArray(JSON.toJSONString(result.getData()), SkuInfo.class);
        //把规格转成Map
        for (SkuInfo info : skuInfos) {
            Map specMap = JSON.parseObject(info.getSpec(), Map.class);
            info.setSpecMap(specMap);
        }
        //导入到索引库中
        skuEsMapper.saveAll(skuInfos);
    }

    @Autowired
    private ElasticsearchTemplate esTemplate;
    @Override
    public Map search(Map<String, String> searchMap) {
        Map map = new HashMap();
        //1、构建基本查询条件
        NativeSearchQueryBuilder builder = builderBasicQuery(searchMap);
        //2、根据查询条件-搜索商品列表
        searchList(map, builder);
        /*//3、跟据查询条件-分组查询商品分类列表
        searchCategoryList(map,builder);
        //4、获取品牌列表
        searchBrandList(map,builder);
        //5、获取规格
        searchSpec(map,builder);*/
        //6、一次性查询分类，品牌，规格
        searchGroup(map,builder);

        return map;
    }

    /**
     * @param map
     * @param builder
     * AggregationBuilders:创建聚合函数工具类
     * term是代表完全匹配，也就是精确查询，搜索前不会再对搜索词进行分词，所以我们的搜索词必须是文档分词集合中的一个
     * NativeSearchQueryBuilder:将连接条件和聚合函数等组合
     * Aggregations:代表一组添加聚合函数统计后的数据
     * Bucket:满足某个条件(聚合)的文档集合
     */
    private void searchGroup(Map map, NativeSearchQueryBuilder builder) {
        builder.addAggregation(AggregationBuilders.terms("group_category").field("categoryName"));
        builder.addAggregation(AggregationBuilders.terms("group_brand").field("brandName"));
        builder.addAggregation(AggregationBuilders.terms("group_spec").field("spec.keyword").size(10000));
        AggregatedPage<SkuInfo> page = esTemplate.queryForPage(builder.build(), SkuInfo.class);
        Aggregations aggregations = page.getAggregations();

        List<String> categoryList = getGroupResult(aggregations, "group_category");
        map.put("categoryList",categoryList);
        List<String> brandList = getGroupResult(aggregations, "group_brand");
        map.put("brandList",brandList);
        List<String> specList = getGroupResult(aggregations, "group_spec");

        Map<String,Set<String>> specMap=new HashMap<>();
        for (String spec : specList) {
            Map<String,String> tempMap=JSON.parseObject(spec,Map.class);
            for (String key : tempMap.keySet()) {
                Set<String> values = specMap.get(key);
                if(values==null){
                    values=new HashSet<String>();
                }
                values.add(tempMap.get(key));
                specMap.put(key,values);
            }
        }
        map.put("specMap",specMap);

    }

    private List<String> getGroupResult(Aggregations aggregations,String group_name){
        StringTerms stringTerms = aggregations.get(group_name);
        List<String> specList =new ArrayList<>();
        for (StringTerms.Bucket bucket : stringTerms.getBuckets()) {
            specList.add(bucket.getKeyAsString());
        }
        return specList;
    }

    /**
     * 根据查询条件-分组查询商品分类列表
     * @param map 结果集包装
     * @param builder 查询条件
     */
    private void searchCategoryList(Map map,NativeSearchQueryBuilder builder){
        //1.设置分组域名-termsAggregationBuilder = AggregationBuilders.terms(别名).field(域名);
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("group_category").field("categoryName");
        //2.添加分组查询参数-builder.addAggregation(termsAggregationBuilder)
        builder.addAggregation(termsAggregationBuilder);
        //3.执行搜索-esTemplate.queryForPage(builder.build(), SkuInfo.class)
        AggregatedPage<SkuInfo> page = esTemplate.queryForPage(builder.build(), SkuInfo.class);
        //4.获取所有分组查询结果集-page.getAggregations()
        Aggregations aggregations = page.getAggregations();
        //5.提取分组结果数据-stringTerms = aggregations.get(填入刚才查询时的别名)
        StringTerms stringTerms = aggregations.get("group_category");
        //6.定义分类名字列表-categoryList = new ArrayList<String>()
        List<String> categoryList = new ArrayList<String>();
        //7.遍历读取分组查询结果-stringTerms.getBuckets().for
        for (StringTerms.Bucket bucket : stringTerms.getBuckets()) {
            //7.1获取分类名字，并将分类名字存入到集合中-bucket.getKeyAsString()
            categoryList.add(bucket.getKeyAsString());
        }
        //8.返回分类数据列表-map.put("categoryList", categoryList)
        map.put("categoryList", categoryList);
    }

    /**
     * 根据查询条件-查询商品列表
     * @param map 结果集包装
     * @param builder 查询条件
     */
    private void searchList(Map map, NativeSearchQueryBuilder builder) {
        //h1.配置高亮查询信息-hField = new HighlightBuilder.Field()
        //h1.1:设置高亮域名-在构造函数中设置
        HighlightBuilder.Field hFieId = new HighlightBuilder.Field("name");
        //h1.2：设置高亮前缀-hField.preTags
        hFieId.preTags("<em style='color:red;'>");
        //h1.3：设置高亮后缀-hField.postTags
        hFieId.postTags("</em>");
        //h1.4：设置碎片大小-hField.fragmentSize
        hFieId.fragmentSize(100);
        //h1.5：追加高亮查询信息-builder.withHighlightFields()
        builder.withHighlightFields(hFieId);

        //3、获取NativeSearchQuery搜索条件对象-builder.build()
        NativeSearchQuery query = builder.build();

        //无高亮查询
        //4.查询数据-esTemplate.queryForPage(条件对象,搜索结果对象)
//        AggregatedPage<SkuInfo> page = esTemplate.queryForPage(query, SkuInfo.class);

        //h2.高亮数据读取-AggregatedPage<SkuInfo> page = esTemplate.queryForPage(query, SkuInfo.class, new SearchResultMapper(){})
        AggregatedPage<SkuInfo> page=esTemplate.queryForPage(query, SkuInfo.class, new SearchResultMapper() {
            @Override
                //h2.1实现mapResults(查询到的结果,数据列表的类型,分页选项)方法
            public <T> AggregatedPage<T> mapResults(SearchResponse searchResponse, Class<T> aClass, Pageable pageable) {
                //h2.2 先定义一组查询结果列表-List<T> list = new ArrayList<T>()
                List<T> list=new ArrayList<>();
                //h2.3 遍历查询到的所有高亮数据-response.getHits().for
                for (SearchHit hit : searchResponse.getHits()) {
                    //h2.3.1 先获取当次结果的原始数据(无高亮)-hit.getSourceAsString()
                    String json = hit.getSourceAsString();
                    //h2.3.2 把json串转换为SkuInfo对象-skuInfo = JSON.parseObject()
                    SkuInfo skuInfo = JSON.parseObject(json, SkuInfo.class);
                    //h2.3.3 获取name域的高亮数据-nameHighlight = hit.getHighlightFields().get("name")
                    HighlightField nameHighlight = hit.getHighlightFields().get("name");

                    //h2.3.4 如果高亮数据不为空-读取高亮数据
                    if(nameHighlight!=null){
                        //h2.3.4.1 定义一个StringBuffer用于存储高亮碎片-buffer = new StringBuffer()
                        StringBuffer stringBuffer = new StringBuffer();

                        //h2.3.4.2 循环组装高亮碎片数据- nameHighlight.getFragments().for(追加数据)
                        for (Text fragment : nameHighlight.getFragments()) {
                            stringBuffer.append(fragment);
                        }
                        //h2.3.4.3 将非高亮数据替换成高亮数据-skuInfo.setName()
                        skuInfo.setName(stringBuffer.toString());
                    }
                    //h2.3.5 将替换了高亮数据的对象封装到List中-list.add((T) esItem)
                    list.add((T)skuInfo);
                }
                //h2.4 返回当前方法所需要参数-new AggregatedPageImpl<T>(数据列表，分页选项,总记录数)
                //h2.4 参考new AggregatedPageImpl<T>(list,pageable,response.getHits().getTotalHits())
                return new AggregatedPageImpl<T>(list,pageable,searchResponse.getHits().getTotalHits());
            }
        });
        //5、包装结果并返回
        int pageNum = query.getPageable().getPageNumber();  //当前页
        int pageSize = query.getPageable().getPageSize();//每页查询的条数
        map.put("pageNum", pageNum);
        map.put("pageSize", pageSize);
        map.put("rows", page.getContent());
        map.put("total", page.getTotalElements());
        map.put("totalPages", page.getTotalPages());

    }

    /**
     * 构建查询条件
     * @param searchMap 用户传入的条件
     * @return
     */
    private NativeSearchQueryBuilder builderBasicQuery(Map<String, String> searchMap) {
        //1、创建查询条件构建器-builder = new NativeSearchQueryBuilder()
        NativeSearchQueryBuilder builder = new NativeSearchQueryBuilder();
        //2、组装查询条件
        if(searchMap != null){
            BoolQueryBuilder boolQueryBuilder=QueryBuilders.boolQuery();
            //2.1关键字搜索-builder.withQuery(QueryBuilders.matchQuery(域名，内容))
            String keywords = searchMap.get("keywords") == null ? "" : searchMap.get("keywords");
            //用户传入的关键字
            if(StringUtils.isNotEmpty(keywords)){
//                builder.withQuery(QueryBuilders.matchQuery("name", keywords));
                boolQueryBuilder.must(QueryBuilders.matchQuery("name",keywords));
            }
            String category = searchMap.get("category")==null?"":searchMap.get("category");
            if(StringUtils.isNotEmpty(category)){
                boolQueryBuilder.must(QueryBuilders.termQuery("categoryName",category));
            }
            String brand=searchMap.get("brand")==null?"":searchMap.get("brand");
            if(StringUtils.isNotEmpty(brand)){
                boolQueryBuilder.must(QueryBuilders.termQuery("brandName",brand));
            }
            //获取所有传入参数
            for (String key : searchMap.keySet()) {
                if(key.startsWith("spec_")){
                    //specMap.规格名字.keyword
                    String specFieId="specMap."+key.substring(5)+".keyword";
                    //规格域
                    boolQueryBuilder.must(QueryBuilders.termQuery(specFieId,searchMap.get(key)));
                }
            }

            //价格区间查询
            String price=searchMap.get("price")==null?"":searchMap.get("price");
            if(StringUtils.isNotEmpty(price)){
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price");
                String[] split = price.split("-");
                //处理价格price>=0
                boolQueryBuilder.must(rangeQueryBuilder.gte(split[0]));
                //传入的价格范围：0-500，而不是3000
                if(split.length>1){
                    //如果传入0-500
                    //处理价格price<=500
                    boolQueryBuilder.must(rangeQueryBuilder.lte(split[1]));
                }
            }
            //多条件匹配查询
            builder.withQuery(boolQueryBuilder);

            //当前页
            Integer pageNum =searchMap.get("pageNum")==null?1:new Integer(searchMap.get("pageNum"));
            Integer pageSize=5;
            PageRequest pageRequest = PageRequest.of(pageNum, pageSize);
            builder.withPageable(pageRequest);


            //排序asc升序/desc降序
            String sortRule=searchMap.get("sortRule")==null?"":searchMap.get("sortRule");
            //排序的域名
           String sortFieId= searchMap.get("sortFieId")==null?"":searchMap.get("sortFieId");
           if(StringUtils.isNotEmpty(sortFieId)){
               builder.withSort(SortBuilders.fieldSort(sortFieId).order(SortOrder.valueOf(sortRule)));
           }
        }
        return builder;
    }

    /**
     * 根据查询条件——分组查询品牌列表
     */
    private void searchBrandList(Map map,NativeSearchQueryBuilder builder){
        //1.设置分组域名-termsAggregationBuilder = AggregationBuilders.terms(别名).field(域名);
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("group_brand").field("brandName");
        //2.添加分组查询参数-builder.addAggregation(termsAggregationBuilder)
        builder.addAggregation(termsAggregationBuilder);
        //3.执行搜索-esTemplate.queryForPage(builder.build(), SkuInfo.class)
        AggregatedPage<SkuInfo> page = esTemplate.queryForPage(builder.build(), SkuInfo.class);
        //4.获取所有分组查询结果集-page.getAggregations()
        Aggregations aggregations = page.getAggregations();
        //5.提取分组结果数据-stringTerms = aggregations.get(填入刚才查询时的别名)
        StringTerms stringTerms = aggregations.get("group_brand");
        //6.定义分类名字列表-categoryList = new ArrayList<String>()
        List<String> brandList = new ArrayList<>();
        //7.遍历读取分组查询结果-stringTerms.getBuckets().for
        for (StringTerms.Bucket bucket : stringTerms.getBuckets()) {
            //7.1获取分类名字，并将分类名字存入到集合中-bucket.getKeyAsString()
            brandList.add(bucket.getKeyAsString());
        }
        //8.返回分类数据列表-map.put("categoryList", categoryList)
        map.put("brandList",brandList);
    }

    /**查询规格
     * @param map
     * @param builder
     */
    public void searchSpec(Map map,NativeSearchQueryBuilder builder){
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("group_spec").field("spec.keyword").size(10000);
        builder.addAggregation(termsAggregationBuilder);
        AggregatedPage<SkuInfo> page = esTemplate.queryForPage(builder.build(), SkuInfo.class);
        Aggregations aggregations = page.getAggregations();
        StringTerms stringTrems = aggregations.get("group_spec");
        List<String> specList=new ArrayList<>();
        for (StringTerms.Bucket bucket : stringTrems.getBuckets()) {
            specList.add(bucket.getKeyAsString());
        }
        Map<String,Set<String>> specMap=new HashMap<>();
        for (String spec : specList) {
            Map<String,String> tempMap=JSON.parseObject(spec,Map.class);
            for (String key : tempMap.keySet()) {
                Set<String> values = specMap.get(key);
                if(values==null){
                    values=new HashSet<String>();
                }
                values.add(tempMap.get(key));
                specMap.put(key,values);
            }
        }
        map.put("specMap",specMap);
    }



}
