package com.anping.music.utils;

import com.alibaba.fastjson.JSONObject;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Anping Sec
 * @date 2022/09/19
 * description:
 */
public class Utils {
    private static final String UTF_8 = "UTF-8";
    private static final String JSON_CHARSET = "application/json; charset=UTF-8";

    public static String post(String urlPath, Map<String, Object> body) {
        String json = JSONObject.toJSONString(body);
        String result = "";
        BufferedReader reader = null;
        try {
            java.net.URL url = new java.net.URL(urlPath);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Connection", "Keep-Alive");
            // 设置文件类型:
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            // 设置接收类型否则返回415错误
            // conn.setRequestProperty("accept","*/*")此处为暴力方法设置接受所有类型，以此来防范返回415;
            conn.setRequestProperty("accept", "*/*");
            // 往服务器里面发送数据
            if (json != null) {
                byte[] writebytes = json.getBytes();
                // 设置文件长度
                conn.setRequestProperty("Content-Length", String.valueOf(writebytes.length));
                OutputStream outwritestream = conn.getOutputStream();
                outwritestream.write(json.getBytes());
                outwritestream.flush();
                outwritestream.close();
            }
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

    public static String get(String url, JSONObject param, HttpHeaders headers) {
        StringBuilder result = new StringBuilder();
        BufferedReader reader = null;
        URL uri = null;
        InputStream is = null;
        InputStreamReader isr = null;
        try {
            url += "?" + jsonToForm(param);
            uri = new java.net.URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) uri.openConnection();
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);
            conn.setRequestProperty("Connection", "Keep-Alive");
            // 设置接收类型否则返回415错误
            conn.setRequestProperty("accept", "*/*");
            Set<Map.Entry<String, List<String>>> entries = headers.entrySet();
            for (Map.Entry<String, List<String>> entry : entries) {
                List<String> value = entry.getValue();
                String val = String.join(";", value);
                conn.setRequestProperty(entry.getKey(), val);
            }
            conn.connect();
            is = conn.getInputStream();
            isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String str = null;
            while ((str = br.readLine()) != null) {
                result.append(str);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e) {
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        return result.toString();
    }

    public static void writeJson2Response(HttpServletResponse response, String respStr) {
        PrintWriter writer = null;
        response.setContentType(JSON_CHARSET);
        response.setCharacterEncoding(UTF_8);
        try {
            writer = response.getWriter();
            writer.println(respStr);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        response.setStatus(200);
    }

    /**
     * 获取用户真实IP地址，不使用request.getRemoteAddr()的原因是有可能用户使用了代理软件方式避免真实IP地址,
     * 可是，如果通过了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP值
     *
     * @return ip
     */
    public static String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个ip值，第一个ip才是真实ip
            if (ip.indexOf(",") != -1) {
                ip = ip.split(",")[0];
            }
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 封装JDK自带的UUID, 通过Random数字生成, 中间无-分割.
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }


    public static String jsonToForm(JSONObject data) {
        StringBuilder sb = new StringBuilder();
        Set<Map.Entry<String, Object>> entries = data.entrySet();
        boolean first = true;
        for (Map.Entry<String, Object> entry : entries) {
            if (!first) {
                sb.append("&");
            } else {
                first = false;
            }
            sb.append(entry.getKey());
            sb.append("=");
            String val = entry.getValue().toString();
            try {
                val = URLEncoder.encode(val, StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            sb.append(val);
        }
        return sb.toString();
    }

    public static void mySleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
