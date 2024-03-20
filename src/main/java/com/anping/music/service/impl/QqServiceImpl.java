package com.anping.music.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.anping.music.entity.MusicInfo;
import com.anping.music.entity.MusicSource;
import com.anping.music.entity.Page;
import com.anping.music.service.CatchService;
import com.anping.music.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Anping Sec
 * @date 2023/03/06
 * description:
 */
@Service("qq")
@Slf4j
public class QqServiceImpl implements CatchService {

    public static final String PLAY_PREFIX = "http://ws6.stream.qqmusic.qq.com/";

    @Value("${static.path}")
    private String staticUrl;

    @Autowired
    private CookieCache cookieCache;

    @Autowired
    private FFmpeg fFmpeg;

    @Override
    public HttpHeaders get() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");
        httpHeaders.set("Origin", "https://y.qq.com");
        httpHeaders.set("Referer", "https://y.qq.com/");
        return httpHeaders;
    }

    public MusicInfo get(String title, String singers, String albumName) {
        Page page = new Page();
        String key = title + "-" + singers;
        page.setKey(key);
        log.info("[{}] start to get source from qq", key);
        List<MusicInfo> list = findList(page);
        MusicInfo bestMatch = null;
        String[] wyySingers = singers.split(",");
        for (MusicInfo musicInfo : list) {
            String[] qqSingers = musicInfo.getSingers().split("/");
            if (StringUtils.isEmpty(musicInfo.getAlbumName()) || wyySingers.length != qqSingers.length) continue;
            int score = 0;
            for (String wyyS : wyySingers) {
                for (String qqS : qqSingers) {
                    if (getTransferName(wyyS).equals(getTransferName(qqS))) {
                        score++;
                        break;
                    }
                }
            }
            if (score == wyySingers.length) {
                if (title.replaceAll(" ", "").equals(musicInfo.getTitle().replaceAll(" ", ""))) {
                    bestMatch = musicInfo;
                    break;
                }
            }
        }
        if (bestMatch != null) {
            log.info("{} {} match from qq, album match [{}]", bestMatch.getSingers(), bestMatch.getTitle(), albumName.equals(bestMatch.getAlbumName()));
            Utils.mySleep(500);
            MusicInfo listenDetail = getListenDetail(bestMatch.getMid(), null);
            bestMatch.setResourceUrl(listenDetail.getResourceUrl());
            return bestMatch;
        }
        log.warn("{} no source.", title);
        return null;
    }

    private String getTransferName(String str) {
        String strCopy = str.replaceAll(" ", "");
        int index = strCopy.indexOf("（");
        if (index > -1) {
            return strCopy.substring(0, index);
        }
        index = strCopy.indexOf("(");
        return index > -1 ? strCopy.substring(0, index) : strCopy;
    }

    @Override
    public List<MusicInfo> findList(Page page) {
        JSONObject formData = new JSONObject();
        JSONObject req_1 = new JSONObject();
        req_1.put("method", "DoSearchForQQMusicDesktop");
        req_1.put("module", "music.search.SearchCgiService");
        JSONObject param = new JSONObject();
        param.put("searchid", "65722113602762407");
        param.put("remoteplace", "txt.yqq.top");
        param.put("search_type", 0);
        param.put("query", page.getKey());
        param.put("page_num", page.getPageNum());
        param.put("num_per_page", page.getPageSize());
        req_1.put("param", param);
        formData.put("req_1", req_1);
        Object res = musicsFcgApi(formData);
        JSONObject result = JSONObject.parseObject(res.toString());
        JSONObject data = (JSONObject) ((JSONObject) result.get("req_1")).get("data");
        if (data == null) {
            param.put("num_per_page", 10);
            res = musicsFcgApi(formData);
            result = JSONObject.parseObject(res.toString());
            data = (JSONObject) ((JSONObject) result.get("req_1")).get("data");
        }
        List<MusicInfo> musicInfos = new ArrayList<>();
        if (data == null) return musicInfos;
        JSONObject body = (JSONObject) data.get("body");
        JSONArray list = (JSONArray) ((JSONObject) body.get("song")).get("list");
        for (Object e : list) {
            JSONObject jo = (JSONObject) e;
            String mid = jo.getString("mid");
            String title = jo.getString("title");
            JSONObject mv = (JSONObject) jo.get("mv");
            String mvUrl = "mv/" + mv.getString("vid");
            JSONArray singers = (JSONArray) jo.get("singer");
            String singerNames = singers.stream().map(k -> ((JSONObject) k).get("name").toString()).collect(Collectors.joining("/"));
            MusicInfo musicInfo = new MusicInfo();
            musicInfo.setMid(mid);
            musicInfo.setSongID(jo.getLong("id"));
            musicInfo.setTitle(title);
            musicInfo.setSingers(singerNames);
            musicInfo.setInterval(jo.getInteger("interval"));
            JSONObject album = (JSONObject) jo.get("album");
            String pic = this.getAlbumPic(album.getString("pmid"), null);
            musicInfo.setPic(pic);
            musicInfo.setAlbumName(album.getString("name"));
            musicInfo.setSource("qq");
            musicInfo.setMvUrl(mvUrl);
            musicInfos.add(musicInfo);
        }
        return musicInfos;
    }

    public String getAlbumPic(String alBumPMid, Integer t) {
        if (t == null) {
            t = 300;
        }
        String pic = "http://y.qq.com/music/photo_new/T002R";
        pic += t;
        pic += "x";
        pic += t;
        pic += "M000";
        pic += alBumPMid;
        pic += ".jpg?max_age=2592000";
        return pic;
    }

    @Override
    public MusicInfo getListenDetail(String mid, Long songID) {
        JSONObject body = new JSONObject();
        JSONObject req_6 = new JSONObject();
        req_6.put("module", "vkey.GetVkeyServer");
        req_6.put("method", "CgiGetVkey");
        JSONObject param = new JSONObject();
        JSONArray songType = new JSONArray();
        JSONArray songs = new JSONArray();
        songs.add(mid);
        param.put("songmid", songs);
        songType.add(0);
        param.put("guid", "191699105");
        param.put("songtype", songType);
        param.put("uin", cookieCache.getQqCookie().getUin());
        param.put("loginflag", 1);
        req_6.put("param", param);
        body.put("req_6", req_6);
        if (songID != null) {
            //lyric
            JSONObject req_1 = new JSONObject();
            req_1.put("module", "music.musichallSong.PlayLyricInfo");
            req_1.put("method", "GetPlayLyricInfo");
            JSONObject lyricParam = new JSONObject();
            lyricParam.put("songMID", mid);
            lyricParam.put("songID", songID);
            req_1.put("param", lyricParam);
            body.put("req_1", req_1);
        }
        Object res = musicsFcgApi(body);
        JSONObject result = JSONObject.parseObject(res.toString());
        MusicInfo musicInfo = new MusicInfo();
        if (songID != null) {
            String encodeLyric = ((JSONObject) ((JSONObject) result.get("req_1")).get("data")).getString("lyric");
            String lyric = new String(Base64.getDecoder().decode(encodeLyric.getBytes()));
            musicInfo.setLyric(lyric);
        }
        JSONArray midUrlInfo = (JSONArray) ((JSONObject) ((JSONObject) result.get("req_6")).get("data")).get("midurlinfo");
        JSONObject info = (JSONObject) midUrlInfo.get(0);
        musicInfo.setMid(mid);
        musicInfo.setSongID(songID);
        String purl = info.getString("purl");
        String listenUrl = StringUtils.isNotEmpty(purl) ? PLAY_PREFIX + purl : "";
        musicInfo.setResourceUrl(listenUrl);
        return musicInfo;
    }

    public Object musicsFcgApi(JSONObject formData) {
        String baseUrl = "https://u.y.qq.com/cgi-bin/musics.fcg";
//        String baseUrl = "https://u6.y.qq.com/cgi-bin/musics.fcg";
        long now = System.currentTimeMillis();
        baseUrl += "?_=" + now;
        baseUrl += "&sign=" + QqEncrypt.getSign(JSONObject.toJSONString(formData));
        List<String> cookies = new ArrayList<>();
        String cookie = cookieCache.getQqVipCookie();
        cookies.add(cookie);
        return RestTemplateUtil.postFormJson(baseUrl, formData, cookies, this.get());
    }

    @Override
    public String getByMV(MusicSource musicSource) {
        MusicInfo musicInfo = getMusicInfo(musicSource.getPageUrl());
        String ss = musicSource.getSs();
        String to = musicSource.getTo();
        String cmd = "ffmpeg -i " + musicInfo.getResourceUrl() + " ";
        if (StringUtils.isNotEmpty(ss)) {
            cmd += " -ss " + ss + " ";
        }
        if (StringUtils.isNotEmpty(to)) {
            cmd += " -to " + to + " ";
        }
        cmd = setInfo(cmd, musicInfo);
        cmd += " -acodec libmp3lame -aq 0 ";
        File dir = new File(staticUrl);
        dir.mkdirs();
        cmd += "\"" + musicInfo.getLocalUrl() + "\"";
        fFmpeg.executeCommand(cmd, "", true);
        return musicInfo.getLocalUrl();
    }

    private String setInfo(String cmd, MusicInfo musicInfo) {
        cmd += " -metadata title=\"" + musicInfo.getTitle() + "\" ";
        cmd += " -metadata artist=\"" + musicInfo.getSingers() + "\" ";
        cmd += " -metadata description=\"\" ";
        cmd += " -metadata compatible_brands=\"\" ";
        cmd += " -metadata minor_version=\"\" ";
        cmd += " -metadata major_brand=\"\" ";
        return cmd;
    }

    private MusicInfo getMusicInfo(String pageUrl) {
        String start = "mv/";
        String aid = pageUrl.substring(pageUrl.indexOf(start) + start.length());
        Map<String, Object> body = new HashMap<>();
        JSONObject mvInfo = new JSONObject();
        mvInfo.put("module", "music.video.VideoData");
        mvInfo.put("method", "get_video_info_batch");
        JSONObject param1 = new JSONObject();
        JSONArray vidlist = new JSONArray();
        vidlist.add(aid);
        param1.put("vidlist", vidlist);
        JSONArray required = new JSONArray();
        required.add("name");
        required.add("singers");
        param1.put("required", required);
        mvInfo.put("param", param1);

        JSONObject mvUrl = new JSONObject();
        mvUrl.put("module", "music.stream.MvUrlProxy");
        mvUrl.put("method", "GetMvUrls");
        JSONObject param2 = new JSONObject();
        JSONArray aids = new JSONArray();
        aids.add(aid);
        param2.put("vids", aids);
        mvUrl.put("param", param2);
        body.put("mvInfo", mvInfo);
        body.put("mvUrl", mvUrl);
        Object response = RestTemplateUtil.post("https://u.y.qq.com/cgi-bin/musicu.fcg", body, null, String.class);
        JSONObject result = JSONObject.parseObject(response.toString());
        JSONObject mvInfo1 = (JSONObject) result.get("mvInfo");
        JSONObject info = ((JSONObject) ((JSONObject) (mvInfo1.get("data"))).get(aid));
        MusicInfo musicInfo = new MusicInfo();
        musicInfo.setTitle(info.getString("name"));
        JSONArray singers = (JSONArray) info.get("singers");
        String singerNames = singers.stream().map(e -> ((JSONObject) e).getString("name")).collect(Collectors.joining("/"));
        musicInfo.setSingers(singerNames);
        JSONObject mvUrl1 = (JSONObject) result.get("mvUrl");
        JSONArray mp4s = (JSONArray) ((JSONObject) ((JSONObject) mvUrl1.get("data")).get(aid)).get("mp4");
        for (Object e : mp4s) {
            JSONArray urls = (JSONArray) ((JSONObject) e).get("freeflow_url");
            if (urls != null && urls.size() > 0) {
                musicInfo.setResourceUrl(urls.get(0).toString());
                break;
            }
        }
        musicInfo.setLocalUrl(staticUrl + "/" + musicInfo.getTitle() + ".mp3");
        return musicInfo;
    }
}
