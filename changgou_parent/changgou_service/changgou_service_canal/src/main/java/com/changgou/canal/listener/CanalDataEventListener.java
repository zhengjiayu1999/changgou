package com.changgou.canal.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.changgou.canal.mq.queue.TopicQueue;
import com.changgou.canal.mq.send.TopicMessageSender;
import com.changgou.content.feign.ContentFeign;
import com.changgou.content.pojo.Content;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import com.xpand.starter.canal.annotation.*;
import entity.Message;
import entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

/**
 * 实现数据库变更监听
 * @description com.changgou.canal.listener
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@CanalEventListener
public class CanalDataEventListener {
    @Autowired
    private ContentFeign contentFeign;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private TopicMessageSender topicMessageSender;


    /***
     * 获取某个列的值
     * @param rowData
     * @param name
     * @return
     */
    public String getColumn(CanalEntry.RowData rowData , String name){
        //操作后的数据
        for (CanalEntry.Column column : rowData.getBeforeColumnsList()) {
            String columnName = column.getName();
            if(columnName.equalsIgnoreCase(name)){
                return column.getValue();
            }
        }
        //操作前的数据
        for (CanalEntry.Column column : rowData.getBeforeColumnsList()) {
            String columnName = column.getName();
            if(columnName.equalsIgnoreCase(name)){
                return column.getValue();
            }
        }
        return null;
    }



    /***
     * 规格、分类数据修改监听
     * 同步数据到Redis
     * @param eventType
     * @param rowData
     */
    @ListenPoint(destination = "example", schema = "changgou_goods", table = {"tb_spu"}, eventType = {CanalEntry.EventType.UPDATE,CanalEntry.EventType.DELETE})
    public void onEventCustomSpu(CanalEntry.EventType eventType, CanalEntry.RowData rowData) {
        //操作类型
        int number = eventType.getNumber();
        //操作的数据
        String id = getColumn(rowData,"id");
        //封装Message
        Message message = new Message(number, id, TopicQueue.TOPIC_QUEUE_SPU,TopicQueue.TOPIC_EXCHANGE_SPU);
        //发送消息
        topicMessageSender.sendMessage(message);
    }


    @ListenPoint(destination = "example",schema = "changgou_content",table = "tb_content",eventType =
            {CanalEntry.EventType.INSERT,CanalEntry.EventType.UPDATE,CanalEntry.EventType.DELETE})
    public void onEventContent(CanalEntry.EventType eventType,CanalEntry.RowData rowData){
        System.out.println("广告监听");
        String categoryId="";
        if(eventType==CanalEntry.EventType.INSERT){
            //categoryId字段在表中第二列，下标填1
            categoryId=rowData.getAfterColumns(1).getValue();
        }else if(eventType==CanalEntry.EventType.UPDATE){
            categoryId=rowData.getAfterColumns(1).getValue();
            //修改了，就要把修改前的广告分类缓存也更新
            String categoryIdBefore=rowData.getBeforeColumns(1).getValue();
            if(!categoryIdBefore.equals(categoryId)){
                Result<List<Content>> result = contentFeign.findByCategory(new Long(categoryIdBefore));
                if(result.getData()!=null){
                    //更新缓存
                    stringRedisTemplate.boundValueOps("content_"+categoryIdBefore).set(JSON.toJSONString(result.getData()));
                }
            }
        }else {//删除操作，取修改前的值
            categoryId = rowData.getBeforeColumns(1).getValue();
        }
        //根据categoryId查询所有广告列表
        Result<List<Content>> result=contentFeign.findByCategory(new Long(categoryId));
        if(result.getData()!=null){
            //更新缓存
            stringRedisTemplate.boundValueOps("content_"+categoryId).set(JSON.toJSONString(result.getData()));
        }
    }

   /* *//**
     * 新增监听
     * @InsertListenPoint：新增监听
     * CanalEntry.EventType:变更操作类型
     * CanalEntry.RowData：此次变更的数据
     *//*
    @InsertListenPoint
    public void onEventInsert(CanalEntry.EventType eventType,CanalEntry.RowData rowData){
        //rowData.getBeforeColumnsList():数据变更前的内容
        //rowData.getAfterColumnsList()：数据变更后的内容
        System.out.println("新增监听");
        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
            System.out.println(column.getName()+":"+column.getValue());
        }
    }

    *//**
     * 修改监听
     * @UpdateListenPoint：更新监听
     * CanalEntry.EventType:变更操作类型
     * CanalEntry.RowData：此次变更的数据
     *//*
    @UpdateListenPoint
    public void onEventUpdate(CanalEntry.EventType eventType,CanalEntry.RowData rowData){
        //rowData.getBeforeColumnsList():数据变更前的内容
        //rowData.getAfterColumnsList()：数据变更后的内容
        System.out.println("---------更新监听--------");
        int i = 0;
        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
            //获取修改前数据
            CanalEntry.Column beforeColumns = rowData.getBeforeColumns(i);
            //如果修改了字段
            if(!beforeColumns.getValue().equals(column.getValue())) {
                System.out.print("修改了字段:" + column.getName() + "   ");
                System.out.println(beforeColumns.getValue() + "-->" + column.getValue());
            }
            i++;
        }
    }

    *//**
     * 删除监听
     * @DeleteListenPoint：删除监听
     * CanalEntry.EventType:变更操作类型
     * CanalEntry.RowData：此次变更的数据
     *//*
    @DeleteListenPoint
    public void onEventDelete(CanalEntry.EventType eventType,CanalEntry.RowData rowData){
        //rowData.getBeforeColumnsList():数据变更前的内容
        //rowData.getAfterColumnsList()：数据变更后的内容
        System.out.println("---------删除监听--------");
        for (CanalEntry.Column column : rowData.getBeforeColumnsList()) {
            System.out.println(column.getName() + ":" + column.getValue());
        }
    }

    *//**
     * 自定义监听
     * @ListenPoint：自定义监听
     * destination：必须使用canal.properties配置文件中canal.destinations属性的名字
     * schema：监听的数据库
     * table：监听的表
     * eventType：监听的操作类型
     *//*
    @ListenPoint(destination = "example",schema = "changgou_content",table = "tb_content",eventType = CanalEntry.EventType.DELETE)
    public void onEventCustomUpdate(CanalEntry.EventType eventType,CanalEntry.RowData rowData){
        System.out.println("---------自定义监听--------");
        for (CanalEntry.Column column : rowData.getBeforeColumnsList()) {
            System.out.println(column.getName() + ":" + column.getValue());
        }
    }*/

}
