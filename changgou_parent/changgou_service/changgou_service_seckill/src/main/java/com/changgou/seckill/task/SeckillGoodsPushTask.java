package com.changgou.seckill.task;

import com.changgou.seckill.dao.SeckillGoodsMapper;
import com.changgou.seckill.pojo.SeckillGoods;
import entity.DateUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
public class SeckillGoodsPushTask {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 30秒执行一次
     */
    @Scheduled(cron = "0/30 * * * * *")
    public void loadGoodsPushRedis(){
        System.out.println("定时任务被调度了...");
        //获取时间段集合
        /*2019091820
          2019091822
          2019091900
          2019091902
          2019091904
        * */
        List<Date> dateMenus = DateUtil.getDateMenus();
        //循环时间段
        for (Date startTime : dateMenus) {
            String extName = DateUtil.data2str(startTime, DateUtil.PATTERN_YYYYMMDDHH);
            //根据时间段数据查询对应的秒杀商品
            Example example=new Example(SeckillGoods.class);
            Example.Criteria criteria=example.createCriteria();
            //商品必须通过审核1
            criteria.andEqualTo("status","1");
            //库存>0
            criteria.andGreaterThan("stockCount",0);
            //开始时间<=活动开始时间
            criteria.andGreaterThanOrEqualTo("startTime",startTime);
            //活动结束时间<开始时间+2小时
            criteria.andLessThan("endTime",DateUtil.addDateHour(startTime,2));
            //排除之前已经加载到Redis缓存中的商品数据
            Set keys = redisTemplate.boundHashOps("SeckillGoods_" + extName).keys();
            if(keys!=null&&keys.size()>0){
                criteria.andNotIn("id",keys);
            }
            //查询数据
            List<SeckillGoods> seckillGoods = seckillGoodsMapper.selectByExample(example);
            System.out.println(extName+"时段导入商品个数为:"+seckillGoods.size());
            //将秒杀的商品数据加入到Redis缓存中
            for (SeckillGoods seckillGood : seckillGoods) {
                redisTemplate.boundHashOps("SeckillGoods_"+extName).put(seckillGood.getId(),seckillGood);

                //方式二：使用加redis的自减，先把库存存到Redis
                redisTemplate.boundHashOps("SeckillGoodsCount").increment(seckillGood.getId(),seckillGood.getStockCount());
            }
        }
    }

    /***
     * 将商品ID存入到数组中
     * @param len:长度
     * @param id :值
     */
    public Long[] pushIds(int len,Long id){
        Long[] ids = new Long[len];
        for (int i = 0; i <ids.length ; i++) {
            ids[i]=id;
        }
        return ids;
    }

}
