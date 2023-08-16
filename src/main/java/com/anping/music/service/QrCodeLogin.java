package com.anping.music.service;

import com.alibaba.fastjson.JSONObject;
import com.anping.music.utils.RestTemplateUtil;
import com.anping.music.utils.Utils;
import com.anping.music.utils.WyyMusicUtils;
import com.anping.music.utils.result.ResponseResult;
import com.anping.music.utils.result.ResultUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author Anping Sec
 * @date 2023/08/15
 * description:
 */
@Component
@Slf4j
public class QrCodeLogin {

    public static final String STR = ";";
    private long lastGetKey = System.currentTimeMillis();
    private long lastGetStatus = System.currentTimeMillis();

    public static final String[] labels = {"__csrf", "NMTID", "MUSIC_U", "MUSIC_R_T", "MUSIC_A_T", "Max-Age", "Expires"};

    public synchronized String getKey(HttpHeaders headers) {
        long now = System.currentTimeMillis();
        if (now - lastGetKey < 3000) {
            Utils.mySleep(3000);
        }
        lastGetKey = now;
        String url = "https://music.163.com/weapi/login/qrcode/unikey";
        JSONObject param = new JSONObject();
        param.put("type", 1);
        Object resultObject = RestTemplateUtil.postFormDataWithCookie(url, WyyMusicUtils.weapiEncrypt(param.toString()), headers, null);
        if (resultObject != null) {
            JSONObject resultJson = JSONObject.parseObject(resultObject.toString());
            log.info("getKey result：{}", resultJson);
            if ("200".equals(resultJson.getString("code"))) {
                return resultJson.getString("unikey");
            }
        }
        return null;
    }

    public synchronized ResponseResult<Object> loginStatus(String key, HttpHeaders headers) {
        long now = System.currentTimeMillis();
        if (now - lastGetStatus < 3000) {
            Utils.mySleep(3000);
        }
        lastGetStatus = now;
        log.info("loginStatus {}", key);
        String url = "https://music.163.com/weapi/login/qrcode/client/login";
        JSONObject param = new JSONObject();
        param.put("type", 1);
        param.put("key", key);
        ResponseEntity<Object> responseEntity = RestTemplateUtil.postFormData(url, WyyMusicUtils.weapiEncrypt(param.toString()), headers, null);
        Object body = responseEntity.getBody();
        if (body == null) {
            return ResultUtil.error("response error.please retry!");
        }
        Map map = (Map) body;
        //800 二维码过期，801等待扫码
        if (!"803".equals(map.get("code").toString())) {
            log.info("scan code detail：{}", map);
            return ResultUtil.build(((Integer) map.get("code")), map.get("message").toString(), null);
        }
        List<String> list = responseEntity.getHeaders().get("set-cookie");
        StringBuilder sb = new StringBuilder();
        if (list != null) {
            boolean[] f = new boolean[labels.length];
            int findNum = 0;
            for (String cookie : list) {
                for (int i = 0; i < f.length; i++) {
                    if (!f[i]) {
                        String kv = cleanCookie(cookie, labels[i]);
                        if (StringUtils.isNotEmpty(kv)) {
                            f[i] = true;
                            findNum++;
                            if (findNum > 1) {
                                sb.append(STR);
                            }
                            sb.append(kv);
                        }
                    }
                }
            }
        }
        log.info("scan success.detail：{}", sb.toString());
        return ResultUtil.success("ok!", sb.toString());
    }

    private String cleanCookie(String cookie, String key) {
        int startIndex = cookie.indexOf(key);
        if (startIndex > -1) {
            int endIndex = cookie.indexOf(STR, startIndex);
            if (endIndex == -1) {
                endIndex = cookie.length();
            }
            return cookie.substring(startIndex, endIndex);
        }
        return "";
    }

}
