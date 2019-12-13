package com.changgou.filter;

import com.changgou.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关全局权限过滤器
 * @author Steven
 * @description com.changgou.filter
 */
@Component
public class AuthorizeFilter implements GlobalFilter, Ordered {

    //令牌的key
    private static final String AUTHORIZE_TOKEN = "Authorization";

    //登录url
    private static final String USER_LOGIN_URL = "http://localhost:9001/oauth/login";

    /**
     * 拦截请求过滤规则逻辑
     * @param exchange
     * @param chain
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //1、获取Request、Response对象-exchange.get...
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        //2、获取请求的URI-request.getURI().getPath()
        String uri = request.getURI().getPath();
        //3、如果是登录请求-uri.startsWith，放行-chain.filter
        //if(uri.startsWith("/api/user/login")){
        if(!URLFilter.hasAuthorize(uri)){//不想认证的地址
            return chain.filter(exchange);
        }else{ //4、如果是非登录请求
            //4.1 获取前端传入的令牌-从请求头中获取-request.getHeaders().getFirst
            String token = request.getHeaders().getFirst(AUTHORIZE_TOKEN);
            //4.2 如果头信息中没有，从请求参数中获取-request.getQueryParams().getFirst
            if(StringUtils.isEmpty(token)){
                token = request.getQueryParams().getFirst(AUTHORIZE_TOKEN);
            }
            //4.3 如果请求参数中没有，从cookie中获取-request.getCookies().getFirst
            if(StringUtils.isEmpty(token)){
                HttpCookie first = request.getCookies().getFirst(AUTHORIZE_TOKEN);
                if(first != null){
                    token = first.getValue();
                }
            }
            //4.4 如果以上方式都取不到令牌-返回405错误-response.setStatusCode(405)-return response.setComplete
            if(StringUtils.isEmpty(token)){
                //返回405，错误表示方法不允许访问
//                response.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
                response.setStatusCode(HttpStatus.SEE_OTHER);
                String url=USER_LOGIN_URL+"?FROM="+request.getURI();
                response.getHeaders().set("Location",url);//把请求地址返回
                return response.setComplete();
            }else{  // 4.5 如果获取到了令牌，解析令牌-JwtUtil.parseJWT，放行-chain.filter(exchange)
                try {
                    //Claims claims = JwtUtil.parseJWT(token);
                    //4.5.1解析成功-把令牌返回-request.mutate().header(key,value)
                    request.mutate().header(AUTHORIZE_TOKEN,"bearer "+token);
                } catch (Exception e) {
                    e.printStackTrace();
                    //无效的认证
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return response.setComplete();
                }
                return chain.filter(exchange);
            }
        }
    }

    /**
     * 过滤器执行顺序设置
     * @return
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
