package com.anping.music.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.anping.music.controller.MusicController;
import com.anping.music.entity.*;
import com.anping.music.service.CatchService;
import com.anping.music.utils.CookieCache;
import com.anping.music.utils.RestTemplateUtil;
import com.anping.music.utils.WyyApi;
import com.anping.music.utils.WyyMusicUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author Anping Sec
 * @date 2023/04/10
 * description:
 */
@Service("wyy")
@Slf4j
public class WyyServiceImpl implements CatchService {

    public static final String SEARCH_URL = "https://music.163.com/weapi/cloudsearch/get/web";

    public static final String LISTEN_URL = "https://music.163.com/weapi/song/enhance/player/url/v1";

    public static final String PLUS_LISTEN_URL = "https://interface.music.163.com/eapi/song/enhance/player/url/v1";

    public static final String LYRIC_URL = "https://music.163.com/weapi/song/lyric";

    public static Map<String, Integer> taskRunning = new ConcurrentHashMap<>();


    private ExecutorService threadPool = Executors.newFixedThreadPool(1);

    @Value("${music.level}")
    private String level;

    @Autowired
    CookieCache cookieCache;

    @Autowired
    private WyyApi wyyApi;

    @Override
    public HttpHeaders get() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36");
        headers.add("origin", "https://music.163.com");
        return headers;
    }

    @Override
    public List<MusicInfo> findList(Page page) {
        JSONObject param = new JSONObject();
        int offset = (page.getPageNum() - 1) * page.getPageSize();
        if (offset > 0) {
            param.put("offset", String.valueOf(offset));
        }
        param.put("s", page.getKey());
        param.put("limit", String.valueOf(page.getPageSize()));
        param.put("csrf_token", cookieCache.getWyyCookie().getCsrfToken());
        param.put("type", "1");
        param.put("total", "false");
        if (page.getPageNum() > 1) {
            param.put("hlposttag", "</span>");
            param.put("hlpretag", "<span class=\"s-fc7\">");
        }
        Map<String, Object> encryptParam = WyyMusicUtils.weapiEncrypt(param.toString());
        List<String> cookies = new ArrayList<>();
        cookies.add(cookieCache.getVipCookie());
        String url = SEARCH_URL + "?csrf_token=" + cookieCache.getWyyCookie().getCsrfToken();
        Object resultObject = RestTemplateUtil.postFormDataWithCookie(url, encryptParam, this.get(), cookies);
        List<MusicInfo> musicInfos = new ArrayList<>();
        if (resultObject != null && !"".equals(resultObject.toString())) {
            JSONObject jsonObject = JSONObject.parseObject(resultObject.toString());
            JSONArray list = (JSONArray) ((JSONObject) jsonObject.get("result")).get("songs");
            if (list != null && list.size() > 0) {
                for (Object e : list) {
                    JSONObject item = (JSONObject) e;
                    MusicInfo musicInfo = new MusicInfo();
                    musicInfo.setMid(item.getString("id"));
                    musicInfo.setTitle(item.getString("name"));
                    JSONArray ar = (JSONArray) item.get("ar");
                    String name = ar.stream().map(e2 -> ((JSONObject) e2).getString("name")).collect(Collectors.joining("/"));
                    musicInfo.setSingers(name);
                    JSONObject al = (JSONObject) item.get("al");
                    musicInfo.setAlbumName(al.getString("name"));
                    musicInfo.setPic(al.getString("picUrl"));
                    musicInfo.setSource("wyy");
                    musicInfos.add(musicInfo);
                }
            }
        }
        return musicInfos;
    }

    @Override
    public MusicInfo getListenDetail(String mid, Long songID) {
        JSONObject param = new JSONObject();
        param.put("csrf_token", cookieCache.getWyyCookie().getCsrfToken());
        param.put("encodeType", "aac");
        param.put("ids", "[" + mid + "]");
        param.put("level", level);
        String url = PLUS_LISTEN_URL;
        List<String> cookies = new ArrayList<>();
        cookies.add(cookieCache.getVipCookie());
        Object resultObject = RestTemplateUtil.postFormDataWithCookie(url, WyyMusicUtils.eapiEncryptWithHeader("/api/song/enhance/player/url/v1", param), this.get(), cookies);
        JSONObject info = JSONObject.parseObject(resultObject.toString());
        JSONArray jsonArray = (JSONArray) info.get("data");
        MusicInfo musicInfo = new MusicInfo();
        musicInfo.setMid(mid);
        if (jsonArray != null && jsonArray.size() > 0) {
            String listenUrl = ((JSONObject) jsonArray.get(0)).getString("url");
            musicInfo.setResourceUrl(listenUrl);
        }
        JSONObject lyricJo = getLyric(mid);
        String lyric = ((JSONObject) lyricJo.get("lrc")).getString("lyric");
        musicInfo.setLyric(lyric);
        return musicInfo;
    }

    public JSONObject getLyric(String id) {
        JSONObject lyParam = new JSONObject();
        lyParam.put("id", Long.valueOf(id));
        lyParam.put("csrf_token", cookieCache.getWyyCookie().getCsrfToken());
        lyParam.put("lv", -1);
        lyParam.put("tv", -1);
        String url = LYRIC_URL + "?csrf_token=" + cookieCache.getWyyCookie().getCsrfToken();
        Object lyricResultObject = RestTemplateUtil.postFormDataWithCookie(url, WyyMusicUtils.weapiEncrypt(lyParam.toString()), this.get(), null);
        return JSONObject.parseObject(lyricResultObject.toString());
    }

    @Override
    public String getByMV(MusicSource musicSource) {
        return null;
    }

    public boolean isRunning(String uid) {
        return taskRunning.containsKey(uid);
    }

    public void syncUserSheet(WyyUserParam wyyUserParam) {
        String uid = wyyUserParam.getUid();
        String userCookie = wyyUserParam.getUserCookie();
        log.info("syncUserSheet uid[{}]", uid);
        taskRunning.put(uid, 0);
        threadPool.submit(() -> {
            try {
                List<Map<String, Object>> sheetList = wyyApi.findPlayList(uid, userCookie);
                String loveSheetId = null;
                if (sheetList != null && sheetList.size() > 0) {
                    for (Map<String, Object> sheet : sheetList) {
                        String sheetId = sheet.get("id").toString();
                        String sheetName = sheet.get("name").toString();
                        if (sheetName.endsWith("喜欢的音乐")) {
                            loveSheetId = sheetId;
                        }
                        if (wyyUserParam.isSyncSheet()) {
                            log.info("start to sync song sheet [{}]", sheetName);
                            SyncParam syncParam = new SyncParam(uid, sheetId, userCookie, null);
                            wyyApi.syncSheet(syncParam);
                            int sec = 2 + (int) (Math.random() * 10);
                            Thread.sleep(sec * 1000);
                        }
                    }
                    List<MusicInfo> addMusics = wyyUserParam.getAddMusics();
                    if (loveSheetId != null) {
                        SyncParam syncParam = new SyncParam(uid, loveSheetId, userCookie, addMusics);
                        wyyApi.downloadMusicsToSheet(syncParam);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Integer successNum = taskRunning.remove(uid);
                log.info("nickname[{}] upload cloud success total num: {}", wyyUserParam.getNickname(), successNum);
            }
        });
    }

}
