package com.anping.music.utils;

import com.alibaba.fastjson.JSONObject;
import com.anping.music.config.SpringContextHolder;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Anping Sec
 * @date 2022/12/24
 * description:
 */
public class RestTemplateUtil {

    private static final RestTemplate restTemplate;

    static {
        restTemplate = (RestTemplate) SpringContextHolder.getBean("restTemplate");
    }

    /**
     * GET请求
     *
     * @param url     请求地址
     * @param headers 请求头
     * @return request result
     */
    public static String get(String url, HttpHeaders headers) {
        HttpEntity<MultiValueMap<String, Object>> formEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, formEntity, String.class);
        return response.getBody();
    }

    public static ResponseEntity<String> getWithCookie(String url, HttpHeaders headers, List<String> cookies) {
        headers.put(HttpHeaders.COOKIE, cookies);
        HttpEntity<MultiValueMap<String, Object>> formEntity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, formEntity, String.class);
    }

    /**
     * 表单提交
     *
     * @param url  请求地址
     * @param body 请求参数(表单)
     * @return request result
     */
    public static Object post(String url, Map<String, Object> body, HttpHeaders headers, Class clz) {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 组装请求体
        HttpEntity<String> request = new HttpEntity<>(JSONObject.toJSONString(body), headers);
        //发起请求
        ResponseEntity<Object> postForEntity = restTemplate.postForEntity(url, request, clz);
        return postForEntity.getBody();
    }

    public static Object postWithCookie(String url, Map<String, Object> body, HttpHeaders headers, List<String> cookies, Class clz) {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        headers.put(HttpHeaders.COOKIE, cookies);
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 组装请求体
        HttpEntity<String> request = new HttpEntity<>(JSONObject.toJSONString(body), headers);
        //发起请求
        ResponseEntity<Object> postForEntity = restTemplate.postForEntity(url, request, clz);
        return postForEntity.getBody();
    }

    public static ResponseEntity<Object> postFormData(String url,Map<String,Object> param,HttpHeaders headers,List<String> cookies){
        if (headers == null) {
            headers = new HttpHeaders();
        }
        if(cookies!=null && cookies.size()>0){
            headers.put(HttpHeaders.COOKIE, cookies);
        }
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, Object> data = new LinkedMultiValueMap<>();
        for(Map.Entry<String,Object> entry : param.entrySet()){
            data.add(entry.getKey(),entry.getValue());
        }
        HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(data,headers);
        return restTemplate.postForEntity(url, httpEntity, Object.class);
    }

    public static Object postFormDataWithCookie(String url, Map<String, Object> param,HttpHeaders headers,List<String> cookies) {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        if(cookies!=null && cookies.size()>0){
            headers.put(HttpHeaders.COOKIE, cookies);
        }
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, Object> data = new LinkedMultiValueMap<>();
        for(Map.Entry<String,Object> entry : param.entrySet()){
            data.add(entry.getKey(),entry.getValue());
        }
        HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(data,headers);
        return restTemplate.postForObject(url, httpEntity, String.class);
    }

    public static String postFormJson(String urlPath, Map<String, Object> body,List<String> cookies,HttpHeaders headers) {
        String json = JSONObject.toJSONString(body);
        String result = "";
        BufferedReader reader = null;
        try {
            URL url = new URL(urlPath);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setReadTimeout(5000);
            // 设置文件类型:
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("user-agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36");
            // 设置接收类型否则返回415错误
            // conn.setRequestProperty("accept","*/*")此处为暴力方法设置接受所有类型，以此来防范返回415;
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("cookie", cookies.get(0));
            for(Map.Entry<String,List<String>> entry : headers.entrySet()){
                String value = entry.getValue().get(0);
                conn.setRequestProperty(entry.getKey(), URLEncoder.encode(value, StandardCharsets.UTF_8.toString()));
            }
            // 往服务器里面发送数据
            byte[] writeBytes = json.getBytes();
            // 设置文件长度
            conn.setRequestProperty("Content-Length", String.valueOf(writeBytes.length));
            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(json.getBytes());
            outputStream.flush();
            outputStream.close();
            if (conn.getResponseCode() == 200) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                result = reader.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
}
