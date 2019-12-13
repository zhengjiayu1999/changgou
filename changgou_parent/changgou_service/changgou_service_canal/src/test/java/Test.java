import com.alibaba.fastjson.JSON;
import com.changgou.CanalApplication;
import com.changgou.canal.mq.queue.TopicQueue;
import entity.Message;
import javafx.application.Application;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CanalApplication.class)

public class Test {
    @Autowired
    private RabbitTemplate rabbitTemplate;


    /***
     * Topic消息发送
     * @param message
     */
    @org.junit.Test
    public void sendMessage(){
        Message message = new Message(2, "1148477943362625536", TopicQueue.TOPIC_QUEUE_SPU,TopicQueue.TOPIC_EXCHANGE_SPU);
        rabbitTemplate.convertAndSend(message.getExechange(), message.getRoutekey(), JSON.toJSONString(message));
    }

}
