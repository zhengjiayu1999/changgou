package com.changgou;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@SpringBootApplication
@EnableEurekaClient
public class GatewayWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayWebApplication.class,args);
    }

    /**
     * 我们可以根据IP来限流，比如每个IP每秒钟只能请求一次，
     * 在GatewayWebApplication定义key的获取，获取客户端IP，将IP作为key
     * @return
     */
    @Bean(name = "ipKeyResolver")
    public KeyResolver getKeyResolver(){
        return new KeyResolver() {
            @Override
            public Mono<String> resolve(ServerWebExchange exchange) {
                //获取请求客户端ip
                String ip = exchange.getRequest().getRemoteAddress().getHostString();
                //以ip限流
                return Mono.just(ip);
            }
        };
    }
}
