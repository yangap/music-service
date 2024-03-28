package com.anping.music.service;

import com.alibaba.fastjson.JSONObject;
import com.anping.music.entity.QqCookie;
import com.anping.music.entity.WyyCookie;
import com.anping.music.service.impl.QqServiceImpl;
import com.anping.music.utils.CookieCache;
import com.anping.music.utils.RestTemplateUtil;
import com.anping.music.utils.WyyMusicUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Anping Sec
 * @date 2023/07/03
 * description:
 */
@Component
@Slf4j
public class TokenRefresh {
    @Autowired
    private CookieCache cookieCache;

    @Autowired
    private QqServiceImpl qqService;

    public void wyyRefresh() {
        log.info("start to refresh!");
        WyyCookie wyyCookie = cookieCache.getWyyCookie();
        String url = "https://music.163.com/eapi/login/token/refresh";
        JSONObject param = new JSONObject();
        param.put("deviceId", "1702e6c32fc5ff54d961250caefb0294");
        param.put("os", wyyCookie.getOs());
        param.put("verifyId", 1);
        JSONObject header = new JSONObject();
        String requestId = "1598671" + (int) (Math.random() * 10);
        header.put("os", wyyCookie.getOs());
        header.put("appver", wyyCookie.getAppver());
        header.put("deviceId", wyyCookie.getDeviceId());
        header.put("requestId", requestId);
        header.put("clientSign", "");
        header.put("osver", wyyCookie.getOsver());
        header.put("Nm-GCore-Status", "1");
        header.put("MG-Product-Name", "music");
        param.put("header", header);
        param.put("cookieToken", wyyCookie.getMUSIC_U());
        Map<String, Object> encrypt = WyyMusicUtils.eapiEncrypt("/api/login/token/refresh", param.toString());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) NeteaseMusicDesktop/2.3.14.999");
        RestTemplateUtil.postFormData(url, encrypt, httpHeaders, Collections.singletonList(wyyCookie.getCookie()));
        log.info("refresh success!");
    }

    public boolean qqRefresh() {
        QqCookie qqCookie = cookieCache.getQqCookie();
        String uin = qqCookie.getUin();
        String qm_keyst = qqCookie.getQm_keyst();
        String accessToken = qqCookie.getPsrf_qqaccess_token();
        String refreshToken = qqCookie.getPsrf_qqrefresh_token();
        log.info("start to refresh qq token...uin[{}] qm_key_st[{}]", uin, qm_keyst);
        JSONObject data = new JSONObject();
        JSONObject req1 = new JSONObject();
        req1.put("module", "QQConnectLogin.LoginServer");
        req1.put("method", "QQLogin");
        JSONObject param = new JSONObject();
        req1.put("param", param);
        param.put("musickey", qqCookie.getQqmusic_key());
        param.put("access_token", accessToken);
        param.put("refresh_token", refreshToken);
        param.put("musicid", Integer.parseInt(uin));
        data.put("req1", req1);
        Object resultObject = qqService.musicsFcgApi(data);
        if (resultObject != null) {
            String resultStr = resultObject.toString();
            log.info("call refresh result: {}", resultStr);
            JSONObject result = JSONObject.parseObject(resultStr);
            Object req11 = result.get("req1");
            if (req11 != null) {
                JSONObject jsonObject = JSONObject.parseObject(req11.toString());
                Object resultData = jsonObject.get("data");
                if (resultData != null) {
                    String musicKey = ((JSONObject) resultData).getString("musickey");
                    Integer musickeyCreateTime = ((JSONObject) resultData).getInteger("musickeyCreateTime");
                    Integer keyExpiresIn = ((JSONObject) resultData).getInteger("keyExpiresIn");
                    log.info("refresh key: {}.time from {} to {}", musicKey, musickeyCreateTime, musickeyCreateTime + keyExpiresIn);
                    if (StringUtils.isNotEmpty(musicKey)) {
                        qqCookie.setQm_keyst(musicKey);
                        qqCookie.refreshTime(musickeyCreateTime, keyExpiresIn);
                        return true;
                    }
                } else {
                    log.error("response data is null...");
                }
            } else {
                log.error("refresh result error...");
            }
        } else {
            log.error("no response...");
        }
        return false;
    }

    public void startService() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                log.info("refresh task access...");
                if (cookieCache.qqAccess()) {
                    boolean refreshResult = false;
                    try {
                        refreshResult = qqRefresh();
                    } catch (Exception e) {
                        log.error("qq refresh error.timer stop...,{}", e.toString());
                    }
                    if (!refreshResult) {
                        timer.cancel();
                    }
                }
            }
        }, 1000, 23 * 3600 * 1000L);
    }
}
