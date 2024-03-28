package com.anping.music.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.anping.music.entity.*;
import com.anping.music.service.CatchService;
import com.anping.music.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Anping Sec
 * @date 2023/03/06
 * description:
 */
@Service(MusicSource.QQ)
@Slf4j
public class QqServiceImpl implements CatchService {
    public static final String PLAY_PREFIX = "http://ws6.stream.qqmusic.qq.com/";
    public static final String LIST_URL_V2 = "http://c.y.qq.com/soso/fcgi-bin/client_search_cp";

    @Autowired
    private CookieCache cookieCache;

    @Override
    public HttpHeaders get() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");
        httpHeaders.set("Origin", "https://y.qq.com");
        httpHeaders.set("Referer", "https://y.qq.com/");
        return httpHeaders;
    }

    public MusicInfo bestMatch(String title, String singers, String albumName, String level) {
        Page<MusicInfo> page = new Page<>();
        String key = title + "-" + singers;
        page.setKey(key);
        log.info("[{}] start to get source from qq", key);
        Page<MusicInfo> pages = findList(page);
        List<MusicInfo> list = pages.getData();
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
            ListenDetailParam listenDetailParam = new ListenDetailParam(bestMatch.getMid(), level);
            MusicInfo listenDetail = getListenDetail(listenDetailParam);
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
    public Page<MusicInfo> findList(Page<MusicInfo> page) {
        return findListV2(page);
    }

    public Page<MusicInfo> findListV2(Page<MusicInfo> page) {
        JSONObject param = new JSONObject();
        param.put("format", "json");
        int pageNum = page.getPageNum(), pageSize = page.getPageSize();
        param.put("p", pageNum);
        param.put("n", pageSize);
        param.put("w", page.getKey());
        param.put("cr", 1);
        param.put("g_tk", 5381);
        param.put("t", 0);
        if (page.getTotal() > 0 && page.getTotal() - (long) (pageNum - 1) * pageSize > 0) {
            return page;
        }
        List<String> cookies = new ArrayList<>();
        String cookie = cookieCache.getQqVipCookie();
        cookies.add(cookie);
        String responseStr = RestTemplateUtil.getWithCookie(LIST_URL_V2, param, this.get(), cookies);
        List<MusicInfo> list = new ArrayList<>();
        if (!responseStr.isEmpty()) {
            JSONObject result = JSONObject.parseObject(responseStr);
            JSONObject dataJson = ((JSONObject) result.get("data"));
            if (dataJson != null) {
                JSONObject song = (JSONObject) dataJson.get("song");
                if (song != null) {
                    Integer total = song.getInteger("totalnum");
                    page.setTotal(total);
                    JSONArray songList = (JSONArray) song.get("list");
                    if (songList != null) {
                        for (Object o : songList) {
                            JSONObject item = (JSONObject) o;
                            String mid = item.getString("songmid");
                            String title = item.getString("songname");
                            Long songId = item.getLong("songid");
                            String albumMid = item.getString("albummid");
                            String pic = this.getAlbumPic(albumMid, null);
                            String albumName = item.getString("albumname");
                            JSONArray singers = (JSONArray) item.get("singer");
                            String singerNames = singers.stream().map(k -> ((JSONObject) k).get("name").toString()).collect(Collectors.joining("/"));
                            MusicInfo musicInfo = new MusicInfo();
                            musicInfo.setMid(mid);
                            musicInfo.setTitle(title);
                            musicInfo.setSongID(songId);
                            musicInfo.setPic(pic);
                            musicInfo.setAlbumName(albumName);
                            musicInfo.setSingers(singerNames);
                            musicInfo.setInterval(item.getInteger("interval"));
                            List<MusicQuality> qualityList = musicInfo.getQualityList();
                            addIfAbsent(item, "size128", MusicQuality.AP_128, qualityList);
                            addIfAbsent(item, "size320", MusicQuality.AP_320, qualityList);
                            addIfAbsent(item, "sizeflac", MusicQuality.AP_FLAC, qualityList);
                            musicInfo.setSource(MusicSource.QQ);
                            list.add(musicInfo);
                        }
                    }
                }
            }
        }
        page.setData(list);
        return page;
    }

    private void addIfAbsent(JSONObject item, String key, MusicQuality musicQuality, List<MusicQuality> qualityList) {
        Integer val = item.getInteger(key);
        if (val != null && val > 0) {
            qualityList.add(musicQuality);
        }
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
    public MusicInfo getListenDetail(ListenDetailParam listenDetailParam) {
        String levelParam = listenDetailParam.getLevel();
        String floorLevel = findFloorLevel(listenDetailParam.getMid(), levelParam);
        listenDetailParam.setLevel(floorLevel);
        if (!levelParam.equals(floorLevel)) {
            log.info("search level[{}]. match level[{}]", levelParam, floorLevel);
        }
        String level = listenDetailParam.getLevel();
        if (!MusicQuality.AP_128.getLevel().equals(level)) {
            return getListenDetail_V2(listenDetailParam);
        }
        return getListenDetail_V1(listenDetailParam);
    }

    public MusicInfo getListenDetail_V1(ListenDetailParam listenDetailParam) {
        String mid = listenDetailParam.getMid();
        Long songID = listenDetailParam.getSongID();
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
        musicInfo.setSource(MusicSource.QQ);
        musicInfo.setSongID(songID);
        musicInfo.setLevel(MusicQuality.AP_128.getLevel());
        String purl = info.getString("purl");
        String listenUrl = StringUtils.isNotEmpty(purl) ? PLAY_PREFIX + purl : "";
        musicInfo.setResourceUrl(listenUrl);
        return musicInfo;
    }

    public JSONObject getDetailInfo(String mid) {
        String url = "http://u.y.qq.com/cgi-bin/musicu.fcg";
        JSONObject content = new JSONObject();
        JSONObject data = new JSONObject();
        JSONObject songInfo = new JSONObject();
        songInfo.put("module", "music.pf_song_detail_svr");
        songInfo.put("method", "get_song_detail_yqq");
        JSONObject param = new JSONObject();
        param.put("song_mid", mid);
        songInfo.put("param", param);
        data.put("songinfo", songInfo);
        content.put("data", data);
        String responseStr = getV2(url, content);
        JSONObject detail = new JSONObject();
        if (!responseStr.isEmpty()) {
            JSONObject result = JSONObject.parseObject(responseStr);
            JSONObject song = (JSONObject) result.get("songinfo");
            if (song != null) {
                detail = (JSONObject) song.get("data");
            }
        }
        return detail;
    }

    /**
     * 找到最接近查询音质的可用音质
     */
    public String findFloorLevel(String mid, String searchLevel) {
        if (MusicQuality.AP_128.getLevel().equals(searchLevel)) {
            return searchLevel;
        }
        JSONObject detailInfo = getDetailInfo(mid);
        if (detailInfo == null) return null;
        JSONObject tractInfo = (JSONObject) detailInfo.get("track_info");
        if (tractInfo != null) {
            JSONObject file = (JSONObject) tractInfo.get("file");
            if (file != null) {
                Integer lessLoss = file.getInteger("size_flac");
                Integer high = file.getInteger("size_320mp3");
                boolean flac = lessLoss != null && lessLoss > 0;
                boolean mp3 = high != null && high > 0;
                if (MusicQuality.AP_FLAC.getLevel().equals(searchLevel)) {
                    if (flac) return MusicQuality.AP_FLAC.getLevel();
                }
                if (mp3) return MusicQuality.AP_320.getLevel();
            }
        }
        return MusicQuality.AP_128.getLevel();
    }

    public MusicInfo getListenDetail_V2(ListenDetailParam listenDetailParam) {
        String level = listenDetailParam.getLevel();
        String mid = listenDetailParam.getMid();
        Long songID = listenDetailParam.getSongID();
        MusicQuality musicQuality = MusicQuality.getQuality(level);
        MusicInfo musicInfo = new MusicInfo();
        musicInfo.setMid(mid);
        musicInfo.setSongID(songID);
        musicInfo.setSource(MusicSource.QQ);
        if (musicQuality == null) return musicInfo;
        String file = musicQuality.getS() + mid + mid + musicQuality.getSuffix();
        QqCookie qqCookie = cookieCache.getQqCookie();
        String uin = qqCookie.getUin();
        JSONObject dataBody = new JSONObject();
        dataBody.put("-", "getplaysongvkey");
        dataBody.put("g_tk", 5381);
        dataBody.put("loginUin", uin);
        dataBody.put("hostUin", 0);
        dataBody.put("format", "json");
        dataBody.put("inCharset", "utf8");
        dataBody.put("outCharset", "utf-8¬ice=0");
        dataBody.put("platform", "yqq.json");
        dataBody.put("needNewCode", 0);
        JSONObject data = new JSONObject();
        JSONObject comm = new JSONObject();
        comm.put("uin", uin);
        comm.put("format", "json");
        comm.put("ct", 19);
        comm.put("cv", 0);
        comm.put("authst", qqCookie.getQqmusic_key());
        JSONObject req_0 = new JSONObject();
        req_0.put("module", "vkey.GetVkeyServer");
        req_0.put("method", "CgiGetVkey");
        JSONObject param = new JSONObject();
        JSONArray files = new JSONArray();
        files.add(file);
        param.put("filename", files);
        int guid = (int) (Math.random() * 1000000);
        param.put("guid", String.valueOf(guid));
        JSONArray songs = new JSONArray();
        songs.add(mid);
        param.put("songmid", songs);
        JSONArray songType = new JSONArray();
        songType.add(0);
        param.put("songtype", songType);
        param.put("uin", uin);
        param.put("loginflag", 1);
        param.put("platform", "20");
        req_0.put("param", param);
        data.put("req0", req_0);
        data.put("comm", comm);
        dataBody.put("data", data);
        String responseStr = getV2("https://u.y.qq.com/cgi-bin/musicu.fcg", dataBody);
        if (!responseStr.isEmpty()) {
            musicInfo.setLevel(level);
            JSONObject result = JSONObject.parseObject(responseStr);
            JSONObject req0 = ((JSONObject) result.get("req0"));
            if (req0 != null) {
                JSONObject resData = (JSONObject) req0.get("data");
                if (resData != null) {
                    Object midUrlObjectArr = resData.get("midurlinfo");
                    if (midUrlObjectArr != null) {
                        JSONArray midUrlInfoArr = (JSONArray) midUrlObjectArr;
                        if (!midUrlInfoArr.isEmpty()) {
                            String purl = ((JSONObject) midUrlInfoArr.get(0)).getString("purl");
                            musicInfo.setResourceUrl(PLAY_PREFIX + purl);
                        }
                    }
                }
            }
            if (songID != null) {
                String lyric = getLyric(mid);
                musicInfo.setLyric(lyric);
            }
        }
        return musicInfo;
    }

    public String getLyric(String mid) {
        String url = "http://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg";
        JSONObject param = new JSONObject();
        param.put("songmid", mid);
        param.put("pcachetime", System.currentTimeMillis());
        param.put("g_tk", 5381);
        param.put("loginUin", 0);
        param.put("hostUin", 0);
        param.put("inCharset", "utf8");
        param.put("outCharset", "utf-8");
        param.put("notice", 0);
        param.put("platform", "yqq");
        param.put("needNewCode", 0);
        String responseStr = getV2(url, param);
        String lyric = "";
        if (!responseStr.isEmpty()) {
            int s = responseStr.indexOf("{"), e = responseStr.indexOf("}");
            responseStr = responseStr.substring(s, e + 1);
            JSONObject result = JSONObject.parseObject(responseStr);
            lyric = result.getString("lyric");
        }
        return lyric;
    }

    public Object musicsFcgApi(JSONObject formData) {
        String baseUrl = "https://u.y.qq.com/cgi-bin/musics.fcg";
        long now = System.currentTimeMillis();
        baseUrl += "?_=" + now;
        baseUrl += "&sign=" + QqEncrypt.getSign(JSONObject.toJSONString(formData));
        List<String> cookies = new ArrayList<>();
        String cookie = cookieCache.getQqVipCookie();
        cookies.add(cookie);
        return RestTemplateUtil.postFormJson(baseUrl, formData, cookies, this.get());
    }

    private String getV2(String url, JSONObject param) {
        List<String> cookies = new ArrayList<>();
        String cookie = cookieCache.getQqVipCookie();
        cookies.add(cookie);
        return RestTemplateUtil.getWithCookie(url, param, this.get(), cookies);
    }
}
