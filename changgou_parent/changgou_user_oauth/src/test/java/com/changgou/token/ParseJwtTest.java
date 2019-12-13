package com.changgou.token;

import org.junit.Test;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;

/*****
 * @Author: shenkunlin
 * @Date: 2019/7/7 13:48
 * @Description: com.changgou.token
 *  使用公钥解密令牌数据
 ****/
public class ParseJwtTest {

    /***
     * 校验令牌
     */
    @Test
    public void testParseToken(){
        //令牌
        String token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlcyI6IlJPTEVfVklQLFJPTEVfVVNFUiIsIm5hbWUiOiJpdGhlaW1hIiwiaWQiOiIxIn0.BrQaIy-uL5T9c25ZRpIj4iMqcOEboLgO9tmM-8dTr6sPe6KfLa7cZFTKwIk9CfVZ4UBo7KojrosK5qf9vGJEs31bokUVQOGQa6-G5T9gUoVYrFAQqJ-tUMjSc_apaxJKSmy_rwD3rnf0czc-hCk9M2QHss1hRBtVVnFTyJ4kgJMMjJIC2yLPOUw9h45l8p2xfWvb0g7MuGLaI0TTieuOB88i1HMDCJtlICwswddnV0yLWcyc0dOPz13lwaFEIrRWyDIEj-7ULoHLS5nkTKdqb4DNmkuVOHm3Fzqo-XIlXReSwlSidTfuyjrLsSl2XLmgG9i_ZWFixUK0h4LLoKYSgw";

        //公钥
        String publickey = "-----BEGIN PUBLIC KEY-----MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuoawM5YRA4YATuVidJTN3LjpkyJluO2mLson5ytU9ArKCrgllEe4US97U1errhicgTSLx2mArPR+yNZR7DpaOlp+/x28C0sEyly+SBUew1J0eqtClM5cX9v+1WN7e4pFjekqu4OSYPOhnoTWbnvhVmlzFqH5Uu9K+9r2C9F0qVfqpY0+ek9ehW1ejsAAMc9DOqdI/zFTOyfFHkYJ5OKmqZrYOSHLXzEcjSL5hXDPIttcGbtwBoxn2xZkQN6HzoBKuXwOS+dx+UhMHq4wr1sw822pZztF7kFpawXIPsLO5XuXXEONPBd3Jedqy50/0HVGNmimhLDaTxW2W1P38IlZ4wIDAQAB-----END PUBLIC KEY-----";

        //校验Jwt
        Jwt jwt = JwtHelper.decodeAndVerify(token, new RsaVerifier(publickey));

        //获取Jwt原始内容
        String claims = jwt.getClaims();
        System.out.println(claims);
        //jwt令牌
        String encoded = jwt.getEncoded();
        System.out.println(encoded);
    }
}
