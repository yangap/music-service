package com.anping.music.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Anping Sec
 * @date 2022/12/20
 * description:
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(){
        RestTemplate restTemplate = new RestTemplate(getClientHttpRequestFactory());
        List<HttpMessageConverter<?>> messageConverters = restTemplate.getMessageConverters();
        //设置编码
        messageConverters.set(1,new StringHttpMessageConverter(StandardCharsets.UTF_8));
        restTemplate.setMessageConverters(messageConverters);
        return restTemplate;
    }

    /**
     * 使用OkHttpClient作为底层客户端
     * @return 创建工厂对象
     */
    private ClientHttpRequestFactory getClientHttpRequestFactory(){
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
        return new OkHttp3ClientHttpRequestFactory(okHttpClient);
    }
}
