package com.anping.music.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.anping.music.entity.*;
import com.anping.music.service.FileCleaner;
import com.anping.music.service.QrCodeLogin;
import com.anping.music.service.UploadCloudSuccess;
import com.anping.music.service.impl.QqServiceImpl;
import com.anping.music.service.impl.WyyServiceImpl;
import com.anping.music.utils.result.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * @author Anping Sec
 * @date 2023/06/25
 * description:
 */
@Component
@Slf4j
public class WyyApi {

    @Value("${static.path}")
    private String staticDir;

    @Autowired
    private CookieCache cookieCache;

    @Autowired
    private QqServiceImpl qqService;

    @Autowired
    private FileCleaner fileCleaner;

    @Autowired
    QrCodeLogin qrCodeLogin;

    public String getKey() {
        return qrCodeLogin.getKey(get());
    }

    public ResponseResult<Object> getCookie(String key) {
        return qrCodeLogin.loginStatus(key, get());
    }

    /**
     * 同步歌单
     */
    public void syncSheet(SyncParam syncParam) {
        syncParam.setMusicInfoList(this.songList(syncParam.getSheetId(), syncParam.getUserCookie()));
        this.pushSongsToCloud(syncParam, (musicInfo, syncParamSuccess) -> {
            String uid = syncParamSuccess.getUid();
            match(uid, musicInfo, syncParamSuccess.getUserCookie());
            WyyServiceImpl.taskRunning.put(uid, WyyServiceImpl.taskRunning.get(uid) + 1);
        });
    }

    public void downloadMusicsToSheet(SyncParam syncParam) {
        if (syncParam.getSheetId() != null && syncParam.getMusicInfoList() != null && !syncParam.getMusicInfoList().isEmpty()) {
            this.pushSongsToCloud(syncParam, (musicInfo, syncParamSuccess) -> {
                String title = musicInfo.getTitle();
                String uid = syncParam.getUid();
                log.info("title[{}] uploadId[{}] add sheet", title, musicInfo.getUploadId());
                if (MusicSource.WYY.equals(musicInfo.getSource()) && musicInfo.isBelongWyySheet()) {
                    match(syncParamSuccess.getUid(), musicInfo, syncParamSuccess.getUserCookie());
                } else {
                    batchAddSheet(syncParamSuccess.getSheetId(), Collections.singletonList(musicInfo), syncParamSuccess.getUserCookie());
                }
                WyyServiceImpl.taskRunning.put(uid, WyyServiceImpl.taskRunning.get(uid) + 1);
                Utils.mySleep(500);
            });
        }
    }

    public List<MusicInfo> getDailyPushByUser(String userCookie) {
        String url = "https://music.163.com/weapi/v3/discovery/recommend/songs";
        List<String> cookies = Collections.singletonList(userCookie);
        JSONObject param = new JSONObject();
        Object resObject = RestTemplateUtil.postFormDataWithCookie(url, WyyMusicUtils.weapiEncrypt(param.toString()), get(), cookies);
        JSONObject res = JSONObject.parseObject(resObject.toString());
        List<MusicInfo> list = new ArrayList<>();
        if ("200".equals(res.getString("code"))) {
            JSONArray musicSources = (JSONArray) ((JSONObject) res.get("data")).get("dailySongs");
            if (musicSources != null) {
                for (Object musicSource : musicSources) {
                    JSONObject m = (JSONObject) musicSource;
                    MusicInfo musicInfo = new MusicInfo();
                    musicInfo.setMid(m.getString("id"));
                    musicInfo.setTitle(m.getString("name"));
                    JSONArray singersArr = (JSONArray) m.get("ar");
                    String singers = singersArr.stream().map(e -> ((JSONObject) e).getString("name")).collect(Collectors.joining("/"));
                    musicInfo.setSingers(singers);
                    JSONObject album = (JSONObject) m.get("al");
                    String al = album.getString("name");
                    musicInfo.setAlbumName(al);
                    musicInfo.setPic(album.getString("picUrl"));
                    musicInfo.setSource(MusicSource.WYY);
                    list.add(musicInfo);
                }
            }
        }
        return list;
    }

    /**
     * 查询歌单列表
     *
     * @param uid
     * @param cookie
     * @return
     */
    public List<Sheet> findPlayList(String uid, String cookie) {
        if (StringUtils.isEmpty(uid) || StringUtils.isEmpty(cookie)) {
            return new ArrayList<>();
        }
        String url = "https://music.163.com/weapi/user/playlist";
        JSONObject param = new JSONObject();
        param.put("uid", uid);
        param.put("limit", 30);
        param.put("offset", 0);
        param.put("includeVideo", true);
        List<String> cookies = Collections.singletonList(cookie);
        Object resObject = RestTemplateUtil.postFormDataWithCookie(url, WyyMusicUtils.weapiEncrypt(param.toString()), get(), cookies);
        JSONObject result = JSONObject.parseObject(resObject.toString());
        JSONArray jsonArray = (JSONArray) result.get("playlist");
        List<Sheet> list = new ArrayList<>();
        for (Object o : jsonArray) {
            JSONObject e = (JSONObject) o;
            if (uid.equals(e.get("userId").toString())) {
                Sheet sheet = new Sheet();
                sheet.setId(e.getLong("id"));
                sheet.setName(e.getString("name"));
                sheet.setCoverImg(e.getString("coverImgUrl"));
                sheet.setStatus(e.getInteger("status"));
                if (sheet.getStatus() == 0) {
                    list.add(sheet);
                }
            }
        }
        return list;
    }

    public String getUid(String cookie) {
        String url = "https://music.163.com/weapi/nuser/account/get";
        JSONObject result = JSONObject.parseObject(RestTemplateUtil.postFormDataWithCookie(url, WyyMusicUtils.weapiEncrypt("{}"), get(),
                Collections.singletonList(cookie)).toString());
        JSONObject account = (JSONObject) result.get("account");
        return account != null ? account.getString("id") : null;
    }

    public boolean existUser(WyyUserParam wyyUserParam) {
        String url = "https://music.163.com/weapi/v1/user/detail/" + wyyUserParam.getUid();
        try {
            JSONObject result = JSONObject.parseObject(RestTemplateUtil.postFormDataWithCookie(url, WyyMusicUtils.weapiEncrypt("{}"), get(),
                    Collections.singletonList(cookieCache.getVipCookie())).toString());
            if (result != null) {
                JSONObject profile = (JSONObject) result.get("profile");
                if (profile != null) {
                    String nickname = profile.getString("nickname");
                    wyyUserParam.setNickname(nickname);
                    log.info("check uid[{}] user:{}", wyyUserParam.getUid(), nickname);
                }
            }
            return result != null && !"".equals(result.get("level").toString());
        } catch (Exception e) {
            log.error(e.toString());
        }
        return false;
    }

    /**
     * 歌单id
     *
     * @param sheetId
     * @return {id:"",name:""}
     */
    public List<MusicInfo> songList(String sheetId, String userCookie) {
        if (StringUtils.isEmpty(sheetId) || StringUtils.isEmpty(userCookie)) {
            return new ArrayList<>();
        }
        String allUrl = "https://music.163.com/weapi/v6/playlist/detail";
        JSONObject param = new JSONObject();
        param.put("id", sheetId);
        param.put("limit", "9999");
        param.put("offset", "0");
        List<String> cookies = Collections.singletonList(userCookie);
        JSONObject resultObject = JSONObject.parseObject(RestTemplateUtil.postFormDataWithCookie(allUrl, WyyMusicUtils.weapiEncrypt(param.toString()), get(), cookies).toString());
        JSONArray trackIds = (JSONArray) ((JSONObject) resultObject.get("playlist")).get("trackIds");
        JSONArray ids = new JSONArray();
        for (Object o : trackIds) {
            JSONObject item = (JSONObject) o;
            String trackId = item.getString("id");
            JSONObject jo = new JSONObject();
            jo.put("id", trackId);
            ids.add(jo);
        }
        param = new JSONObject();
        param.put("c", ids.toString());
        String url = "https://music.163.com/weapi/v3/song/detail";
        JSONObject result = JSONObject.parseObject(RestTemplateUtil.postFormDataWithCookie(url, WyyMusicUtils.weapiEncrypt(param.toString()), get(), cookies).toString());
        JSONArray songs = (JSONArray) result.get("songs");
        List<MusicInfo> list = new ArrayList<>();
        for (Object song : songs) {
            JSONObject jo = (JSONObject) song;
            MusicInfo musicInfo = new MusicInfo();
            musicInfo.setMid(jo.getString("id"));
            musicInfo.setTitle(jo.getString("name"));
            JSONArray singers = (JSONArray) jo.get("ar");
            String singer = "";
            if (singers != null && singers.size() > 0) {
                singer = singers.stream().map(e -> {
                    Object name = ((JSONObject) e).get("name");
                    return name != null && !"".equals(name.toString()) ? name.toString() : "";
                }).collect(Collectors.joining(","));
            }
            musicInfo.setSingers(singer);
            JSONObject al = (JSONObject) jo.get("al");
            musicInfo.setAlbumName(al.getString("name"));
            musicInfo.setSource(MusicSource.WYY);
            musicInfo.setBelongWyySheet(true);
            list.add(musicInfo);
        }
        return list;
    }

    /**
     * {"ids":"[33894312]","br":999000}
     */
    public void pushSongsToCloud(SyncParam syncParam, UploadCloudSuccess cloudSuccess) {
        String uid = syncParam.getUid();
        List<MusicInfo> musicInfos = syncParam.getMusicInfoList();
        String sheetId = syncParam.getSheetId();
        String userCookie = syncParam.getUserCookie();
        log.info("uid[{}] to sheet[{}]", uid, sheetId);
        //该用户听不了的音乐
        List<MusicInfo> musicInfoList = filterNotAvailableMusic(musicInfos, userCookie);
        List<MusicInfo> addSheet = filterAvailableMusicAndNotToSheet(musicInfos, musicInfoList);
        for (MusicInfo m : addSheet) {
            cloudSuccess.call(m, syncParam);
        }
        if (!musicInfoList.isEmpty()) {
            //成功获取歌曲源的数据
            List<MusicInfo> listenUrls = findListenUrl(musicInfoList, syncParam.getLevel());
            try {
                List<MusicInfo> retryList = this.downloadAndUploadCloudV2(listenUrls, syncParam, cloudSuccess);
                if (!retryList.isEmpty()) {
                    log.info("num {} failList will retry...", retryList.size());
                }
                Utils.mySleep(2000);
                downloadAndUploadCloud(retryList, syncParam, cloudSuccess);
            } catch (Exception e) {
                log.error(e.toString());
            } finally {
                fileCleaner.clean();
            }
        }
    }

    public List<MusicInfo> downloadAndUploadCloudV2(List<MusicInfo> musicInfoList, SyncParam syncParam, UploadCloudSuccess cloudSuccess) {
        log.info("downloadAndUploadCloudV2 sync music size: {}", musicInfoList.size());
        List<MusicInfo> failedList = new ArrayList<>();
        int len = musicInfoList.size();
        if (len > 0) {
            int syncLimit = 10;
            int p = len % syncLimit != 0 ? len / syncLimit + 1 : len / syncLimit;
            for (int i = 0; i < p; i++) {
                int from = i * syncLimit;
                List<MusicInfo> list;
                if (i < p - 1) {
                    list = musicInfoList.subList(from, from + syncLimit);
                } else {
                    list = musicInfoList.subList(from, len);
                }
                List<MusicInfo> fail = this.uploadCloudV2(list, syncParam, cloudSuccess);
                failedList.addAll(fail);
                Utils.mySleep(2000);
            }
        }
        return failedList;
    }

    /**
     * @param musicInfos
     * @param syncParam
     */
    public void downloadAndUploadCloud(List<MusicInfo> musicInfos, SyncParam syncParam, UploadCloudSuccess cloudSuccess) {
        for (int i = 0; i < musicInfos.size(); i++) {
            MusicInfo musicInfo = getListenUrlIfExpireAgain(musicInfos, i, syncParam.getLevel());
            this.uploadCloud(musicInfo, syncParam, cloudSuccess);
            Utils.mySleep(1500);
        }
    }

    /**
     * 防止歌曲播放链接过期 重新获取
     *
     * @param musicInfos
     * @param from
     * @return
     */
    private MusicInfo getListenUrlIfExpireAgain(List<MusicInfo> musicInfos, int from, String qualityLevel) {
        long now = System.currentTimeMillis();
        MusicInfo musicInfo = musicInfos.get(from);
        if (now - musicInfo.getListenUrlCreateTime() >= 10 * 60 * 1000) {
            log.info("the song after [{}]  played time may be expired.song:[size:{}] will get again...", musicInfo.getTitle(), musicInfos.size() - from);
            List<MusicInfo> expireList = musicInfos.subList(from, musicInfos.size());
            List<MusicInfo> listenUrl = findListenUrl(expireList, qualityLevel);
            Map<String, MusicInfo> map = new HashMap<>();
            for (MusicInfo info : listenUrl) {
                map.put(info.getMid(), info);
            }
            for (int j = from; j < musicInfos.size(); j++) {
                MusicInfo info = musicInfos.get(j);
                MusicInfo newMusic = map.get(info.getMid());
                if (newMusic != null) {
                    String resourceUrl = newMusic.getResourceUrl();
                    if (StringUtils.isNotEmpty(resourceUrl)) {
                        info.setResourceUrl(resourceUrl);
                    }
                    info.setListenUrlCreateTime(newMusic.getListenUrlCreateTime());
                } else {
                    info.setListenUrlCreateTime(now);
                }
            }
            musicInfo = musicInfos.get(from);
        }
        return musicInfo;
    }

    private void prepareFileParam(MusicInfo musicInfo) {
        try {
            if (!new File(musicInfo.getLocalUrl()).exists()) {
                Map<String, Object> headers = new HashMap<>();
                if (MusicSource.WYY.equals(musicInfo.getSource())) {
                    headers.put("Origin", "https://music.163.com");
                    headers.put("Referer", "https://music.163.com/");
                    headers.put("Cookie", cookieCache.getVipCookie());
                } else {
                    headers.put("Origin", "https://y.qq.com");
                    headers.put("Referer", "https://y.qq.com/");
                }
                DownloadUtils.downloadNetFile(musicInfo.getResourceUrl(), musicInfo.getLocalUrl(), headers);
            }
            fileCleaner.record(musicInfo.getLocalUrl());
            File file = new File(musicInfo.getLocalUrl());
            musicInfo.setFileSize(file.length());
            if (StringUtils.isEmpty(musicInfo.getMd5())) {
                FileInputStream fis = new FileInputStream(musicInfo.getLocalUrl());
                String md5 = DigestUtils.md5Hex(fis);
                musicInfo.setMd5(md5);
            }
        } catch (Exception e) {
            log.error(e.toString());
        }
    }

    private boolean match(String uid, MusicInfo musicInfo, String userCookie) {
        String url = "https://music.163.com/weapi/cloud/user/song/match ";
        log.info("mid[{}] uploadId[{}]", musicInfo.getMid(), musicInfo.getUploadId());
        if (StringUtils.isNotEmpty(musicInfo.getMid()) && StringUtils.isNotEmpty(musicInfo.getUploadId())) {
            if (musicInfo.getMid().equals(musicInfo.getUploadId())) {
                log.info("id is same.don't need match.");
                return true;
            }
            JSONObject param = new JSONObject();
            param.put("userId", uid);
            param.put("songId", musicInfo.getUploadId());
            param.put("adjustSongId", musicInfo.getMid());
            JSONObject result = JSONObject.parseObject(RestTemplateUtil.postFormDataWithCookie(url, WyyMusicUtils.weapiEncrypt(param.toString()), get(), Collections.singletonList(userCookie)).toString());
            log.info("{} match[{},{}] result:[{}]", musicInfo.getTitle(), musicInfo.getUploadId(), musicInfo.getMid(), result);
            return !result.toString().contains("云盘文件不存在");
        }
        return false;
    }

    public void batchAddSheet(String sheetId, List<MusicInfo> musicInfos, String userCookie) {
        String url = "https://music.163.com/weapi/playlist/manipulate/tracks";
        JSONArray songIds = new JSONArray();
        for (MusicInfo m : musicInfos) {
            if (StringUtils.isNotEmpty(m.getUploadId())) {
                songIds.add(m.getUploadId());
            } else if (MusicSource.WYY.equals(m.getSource()) && StringUtils.isNotEmpty(m.getMid())) {
                songIds.add(m.getMid());
            }
        }
        if (!songIds.isEmpty()) {
            JSONObject param = new JSONObject();
            param.put("op", "add");
            param.put("pid", sheetId);
            param.put("imme", "true");
            param.put("trackIds", songIds.toString());
            JSONObject result = JSONObject.parseObject(RestTemplateUtil.postFormDataWithCookie(url, WyyMusicUtils.weapiEncrypt(param.toString()), get(), Collections.singletonList(userCookie)).toString());
            log.info("add sheet result[{}]", result);
        } else {
            log.warn("uploadIds size 0");
        }
    }

    public List<MusicInfo> findListenUrl(List<MusicInfo> musicInfoList, String qualityLevel) {
        if (musicInfoList == null || musicInfoList.isEmpty()) return new ArrayList<>();
        String vipCookie = cookieCache.getVipCookie();
        Map<String, MusicInfo> map = new HashMap<>();
        List<MusicInfo> fromWyy = new ArrayList<>();
        List<MusicInfo> fromQq = new ArrayList<>();
        for (MusicInfo musicInfo : musicInfoList) {
            if (MusicSource.WYY.equals(musicInfo.getSource())) {
                fromWyy.add(musicInfo);
                map.put(musicInfo.getMid(), musicInfo);
            } else {
                fromQq.add(musicInfo);
            }
        }
        String mids = fromWyy.stream().map(MusicInfo::getMid).collect(Collectors.joining(","));
        String url = "https://interface.music.163.com/eapi/song/enhance/player/url/v1";
        JSONObject param = new JSONObject();
        param.put("ids", "[" + mids + "]");
        param.put("level", qualityLevel);
        param.put("encodeType", "flac");
        Object resultObject = RestTemplateUtil.postFormDataWithCookie(url, WyyMusicUtils.eapiEncryptWithHeader("/api/song/enhance/player/url/v1", param), this.get(), Collections.singletonList(vipCookie));
        JSONObject info = JSONObject.parseObject(resultObject.toString());
        List<MusicInfo> list = new ArrayList<>();
        JSONArray jsonArray = (JSONArray) info.get("data");
        int wyyGet = 0, qqGet = 0;
        for (Object item : jsonArray) {
            JSONObject jo = (JSONObject) item;
            String id = jo.getString("id");
            MusicInfo musicInfo = map.get(id);
            if (musicInfo != null) {
                String listenUrl = jo.getString("url");
                if (StringUtils.isNotEmpty(listenUrl)) {
                    String type = jo.getString("type");
                    String ext = StringUtils.isNotEmpty(type) ? type.toLowerCase() : "";
                    musicInfo.setExt(ext);
                    musicInfo.setResourceUrl(listenUrl);
                    musicInfo.setLocalUrl(staticDir + "/" + UUID.randomUUID() + "." + ext);
                    musicInfo.setMd5(jo.getString("md5"));
                    musicInfo.setListenUrlCreateTime(System.currentTimeMillis());
                    wyyGet++;
                    list.add(musicInfo);
                } else {
                    MusicInfo qqMusic = qqService.bestMatch(musicInfo.getTitle(), musicInfo.getSingers(), musicInfo.getAlbumName(), qualityLevel);
                    qqGet = recordQq(qualityLevel, list, qqGet, musicInfo, qqMusic);
                }
            }
        }
        //qq
        for (MusicInfo musicInfo : fromQq) {
            ListenDetailParam listenDetailParam = new ListenDetailParam(musicInfo.getMid(), qualityLevel);
            MusicInfo listenDetail = qqService.getListenDetail(listenDetailParam);
            qqGet = recordQq(qualityLevel, list, qqGet, musicInfo, listenDetail);
            Utils.mySleep(500);
        }
        log.info("find listenUrl size[{}],detail[wyy:{},qq:{}]", list.size(), wyyGet, qqGet);
        return list;
    }

    private int recordQq(String qualityLevel, List<MusicInfo> list, int qqGet, MusicInfo musicInfo, MusicInfo listenDetail) {
        if (listenDetail != null && StringUtils.isNotEmpty(listenDetail.getResourceUrl())) {
            MusicQuality quality = MusicQuality.getQuality(qualityLevel);
            if (quality == null) {
                quality = MusicQuality.AP_128;
            }
            musicInfo.setResourceUrl(listenDetail.getResourceUrl());
            musicInfo.setLocalUrl(staticDir + "/" + UUID.randomUUID() + quality.getSuffix());
            musicInfo.setListenUrlCreateTime(System.currentTimeMillis());
            list.add(musicInfo);
            qqGet++;
        }
        return qqGet;
    }

    public List<MusicInfo> filterAvailableMusicAndNotToSheet(List<MusicInfo> musicList, List<MusicInfo> notAvailableList) {
        List<MusicInfo> list = new ArrayList<>();
        Set<String> dic = new HashSet<>();
        for (MusicInfo musicInfo : notAvailableList) {
            dic.add(musicInfo.getSource() + "_" + musicInfo.getMid());
        }
        for (MusicInfo musicInfo : musicList) {
            if (!musicInfo.isBelongWyySheet() && !dic.contains(musicInfo.getSource() + "_" + musicInfo.getMid())) {
                list.add(musicInfo);
            }
        }
        return list;
    }

    public List<MusicInfo> filterNotAvailableMusic(List<MusicInfo> musicInfo, String useCookie) {
        String url = "https://music.163.com/weapi/song/enhance/player/url";
        JSONObject param = new JSONObject();
        JSONArray ids = new JSONArray();
        Map<String, MusicInfo> map = new HashMap<>();
        List<MusicInfo> notAvailable = new ArrayList<>();
        for (MusicInfo m : musicInfo) {
            if (MusicSource.WYY.equals(m.getSource())) {
                ids.add(m.getMid());
                map.put(m.getMid(), m);
            } else {
                //不是wyy
                notAvailable.add(m);
            }
        }
        param.put("ids", ids.toString());
        param.put("br", 999000);
        Object o = RestTemplateUtil.postFormDataWithCookie(url, WyyMusicUtils.weapiEncrypt(param.toString()), get(), Collections.singletonList(useCookie));
        if (o != null) {
            JSONObject resultObject = JSONObject.parseObject(o.toString());
            JSONArray dataArr = ((JSONArray) resultObject.get("data"));
            for (Object value : dataArr) {
                JSONObject e = ((JSONObject) value);
                String mid = e.get("id").toString();
                int time = Integer.parseInt(e.get("time").toString());
                MusicInfo info = map.get(mid);
                info.setInterval(time);
                //mark
                if (time == 0) {
                    notAvailable.add(info);
                }
            }
        }
        return notAvailable;
    }

    public List<MusicInfo> uploadCloudV2(List<MusicInfo> musicInfos, SyncParam syncParam, UploadCloudSuccess cloudSuccess) {
        List<MusicInfo> failedList = new ArrayList<>();
        if (musicInfos == null || musicInfos.size() == 0) return failedList;
        String userCookie = syncParam.getUserCookie();
        String deviceId = "0643531b660ab584bc015d194a963e6b";
        HttpHeaders httpHeaders = get();
        httpHeaders.set("user-agent", "'Mozilla/5.0 (iPhone; CPU iPhone OS 13_5_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/7.0.15(0x17000f27) NetType/WIFI Language/zh'");
        String checkUrl = "https://interface3.music.163.com/eapi/cloud/upload/check/v2?_nmclfl=1";
        JSONObject param = new JSONObject();
        param.put("deviceId", deviceId);
        param.put("os", "iOS");
        JSONArray songs = new JSONArray();
        for (MusicInfo musicInfo : musicInfos) {
            if (StringUtils.isEmpty(musicInfo.getMd5()) && StringUtils.isNotEmpty(musicInfo.getResourceUrl())) {
                prepareFileParam(musicInfo);
            }
            String md5 = musicInfo.getMd5();
            if (StringUtils.isNotEmpty(md5)) {
                log.info("prepare albumName: {},song: {}", musicInfo.getAlbumName(), musicInfo.getTitle());
                JSONObject song = new JSONObject();
                song.put("md5", musicInfo.getMd5());
                song.put("fileSize", musicInfo.getFileSize());
                song.put("bitrate", 0);
                songs.add(song);
            }
            Utils.mySleep(1500);
        }
        log.info("prepare uploadV2 size songs [{}]", songs.size());
        if (songs.isEmpty()) return failedList;
        param.put("songs", songs.toString());
        param.put("uploadType", 0);
        param.put("verifyId", 1);
        param.put("header", "{}");
        JSONObject resultObject = JSONObject.parseObject(RestTemplateUtil.postFormDataWithCookie(checkUrl, WyyMusicUtils.eapiEncryptWithHeader("/api/cloud/upload/check/v2", param),
                httpHeaders, Collections.singletonList(userCookie)).toString());
        List<String> songIds = new ArrayList<>();
        JSONArray jsonArray = (JSONArray) resultObject.get("data");
        for (Object o : jsonArray) {
            JSONObject jo = (JSONObject) o;
            songIds.add(jo.getString("songId"));
        }
        param = new JSONObject();
        param.put("deviceId", deviceId);
        param.put("os", "iOS");
        songs = new JSONArray();
        int index = 0;
        Map<String, MusicInfo> map = new HashMap<>();
        for (MusicInfo musicInfo : musicInfos) {
            if (StringUtils.isNotEmpty(musicInfo.getMd5())) {
                String songId = songIds.get(index++);
                if (StringUtils.isNotEmpty(songId)) {
                    JSONObject song = new JSONObject();
                    song.put("artist", musicInfo.getSingers());
                    song.put("album", musicInfo.getAlbumName());
                    song.put("fileName", musicInfo.getTitle());
                    song.put("song", musicInfo.getTitle());
                    song.put("songId", songId);
                    song.put("bitrate", 999000);
                    songs.add(song);
                    map.put(songId, musicInfo);
                } else {
                    //该歌在wyy无源
                    failedList.add(musicInfo);
                }
            }
        }
        param.put("songs", songs.toString());
        param.put("header", "{}");
        param.put("uploadType", 0);
        param.put("verifyId", 1);
        String importUrl = "https://interface3.music.163.com/eapi/cloud/user/song/import?_nmclfl=1";
        Object result = RestTemplateUtil.postFormDataWithCookie(importUrl,
                WyyMusicUtils.eapiEncryptWithHeader("/api/cloud/user/song/import", param),
                httpHeaders, Collections.singletonList(userCookie));
        JSONObject res = JSONObject.parseObject(result.toString());
        JSONObject data = ((JSONObject) res.get("data"));
        if (data != null) {
            JSONArray successIds = (JSONArray) data.get("successIds");
            JSONArray failed = (JSONArray) data.get("failed");
            if (failed != null) {
                for (Object e : failed) {
                    JSONObject jo = (JSONObject) e;
                    String songId = jo.getString("songId");
                    MusicInfo musicInfo = map.get(songId);
                    String msg = jo.getString("msg");
                    //文件已在用户文件夹中存在
                    if (!msg.contains("存在")) {
                        failedList.add(musicInfo);
                    }
                    String name = musicInfo != null ? musicInfo.getTitle() : "";
                    log.error("{} failed reason: {}", name, msg);
                }
            }
            JSONArray successSongs = (JSONArray) data.get("successSongs");
            for (Object e : successSongs) {
                JSONObject jo = (JSONObject) e;
                String MD5Id = jo.getString("songId");
                MusicInfo musicInfo = map.get(MD5Id);
                if (musicInfo != null) {
                    JSONObject song = (JSONObject) jo.get("song");
                    String songId = song.getString("songId");
                    musicInfo.setUploadId(songId);
                    if (cloudSuccess != null) {
                        cloudSuccess.call(musicInfo, syncParam);
                    }
                    Utils.mySleep(3000);
                }
            }
            log.info("uploadCloudV2 result success:[{}]", successIds.size());
        } else {
            log.error("upload v2 data response null");
        }
        return failedList;
    }

    public void uploadCloud(MusicInfo musicInfo, SyncParam syncParam, UploadCloudSuccess cloudSuccess) {
        prepareFileParam(musicInfo);
        String userCookie = syncParam.getUserCookie();
        log.info("upload [{}]", musicInfo.getTitle());
        try {
            String filePath = musicInfo.getLocalUrl();
            String md5 = musicInfo.getMd5();
            File file = new File(filePath);
            if (!file.exists()) {
                log.error("uploadCloud file {} not exist", musicInfo.getTitle());
                return;
            }
            String ext = musicInfo.getExt();
            if (StringUtils.isEmpty(ext)) {
                int index = filePath.lastIndexOf(".");
                if (index > -1) {
                    ext = filePath.substring(index + 1);
                }
            }
            String fileName = StringUtils.isNotEmpty(musicInfo.getTitle()) ? musicInfo.getTitle() : file.getName().substring(0, file.getName().lastIndexOf("."));
            JSONObject params = new JSONObject();
            params.put("ext", ext);
            params.put("length", musicInfo.getFileSize());
            params.put("md5", md5);
            HttpHeaders headers = get();
            List<String> cookies = Collections.singletonList(userCookie);
            JSONObject checkResult = JSONObject.parseObject(uploadCheck(params, headers, cookies).toString());
            log.info("upload check [{}]", checkResult.toString());
            JSONObject tokenParam = new JSONObject();
            tokenParam.put("bucket", "jd-musicrep-privatecloud-audio-public");
            tokenParam.put("md5", md5);
            tokenParam.put("ext", ext);
            tokenParam.put("filename", fileName);
            boolean uploadSuccess = uploadIfNeed(checkResult, headers, cookies, file, String.valueOf(musicInfo.getFileSize()), md5, tokenParam);
            if (!uploadSuccess) {
                log.error("file size is not exist...");
                return;
            }
            JSONObject tokenAllocResult = JSONObject.parseObject(tokenAlloc(tokenParam, headers, cookies).toString());
            log.info("call tokenAlloc success.[{}]", tokenAllocResult.toString());
            String songId = checkResult.getString("songId");
            JSONObject songInfo = new JSONObject();
            songInfo.put("md5", md5);
            songInfo.put("songid", songId);
            songInfo.put("filename", fileName);
            songInfo.put("song", fileName);
            songInfo.put("bitrate", "999000");
            JSONObject tr = (JSONObject) tokenAllocResult.get("result");
            songInfo.put("resourceId", tr.getString("resourceId"));
            songInfo.put("album", StringUtils.isNotEmpty(musicInfo.getAlbumName()) ? musicInfo.getAlbumName() : "未知专辑");
            songInfo.put("artist", StringUtils.isNotEmpty(musicInfo.getSingers()) ? musicInfo.getSingers() : "未知歌手");
            JSONObject info = JSONObject.parseObject(info(songInfo, headers, cookies).toString());
            String songIdLong = info.getString("songIdLong");
            musicInfo.setUploadId(songIdLong);
            log.info("call info[{}] success...detail[{}]", songIdLong, info.toString());
            String lastSongId = info.getString("songId");
            if (StringUtils.isEmpty(lastSongId)) {
                log.error("this song don't have source...");
            } else {
                Object cloud = cloud(lastSongId, headers, cookies);
                JSONObject cloudResult = JSONObject.parseObject(cloud.toString());
                if ("200".equals(cloudResult.getString("code"))) {
                    log.info("call cloud public success...");
                    //成功回调
                    if (cloudSuccess != null) {
                        cloudSuccess.call(musicInfo, syncParam);
                    }
                } else {
                    log.info("call cloud public...[{}]", cloud.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Object uploadCheck(JSONObject params, HttpHeaders headers, List<String> cookies) {
        String url = "https://interface.music.163.com/weapi/cloud/upload/check";
        params.put("bitrate", "999000");
        params.put("ext", "");
        params.put("songId", "0");
        params.put("version", 1);
        return RestTemplateUtil.postFormDataWithCookie(url, WyyMusicUtils.weapiEncrypt(params.toString()), headers, cookies);
    }

    private Object tokenAlloc(JSONObject params, HttpHeaders headers, List<String> cookies) {
        String url = "https://music.163.com/weapi/nos/token/alloc";
        params.put("local", false);
        params.put("nos_product", 3);
        params.put("type", "audio");
        return RestTemplateUtil.postFormDataWithCookie(url, WyyMusicUtils.weapiEncrypt(params.toString()), headers, cookies);
    }

    private Object info(JSONObject songInfo, HttpHeaders headers, List<String> cookies) {
        String url = "https://music.163.com/weapi/upload/cloud/info/v2";
        return RestTemplateUtil.postFormDataWithCookie(url, WyyMusicUtils.weapiEncrypt(songInfo.toString()), headers, cookies);
    }

    private Object cloud(String songId, HttpHeaders headers, List<String> cookies) {
        String url = "https://interface.music.163.com/weapi/cloud/pub/v2";
        JSONObject params = new JSONObject();
        params.put("songid", Long.valueOf(songId));
        return RestTemplateUtil.postFormDataWithCookie(url, WyyMusicUtils.weapiEncrypt(params.toString()), headers, cookies);
    }

    private boolean uploadIfNeed(Map<String, Object> uploadCheckRes, HttpHeaders headers, List<String> cookies
            , File file, String size, String md5, JSONObject tokenParam) {
        Object needUpload = uploadCheckRes.get("needUpload");
        if (needUpload != null && (boolean) (needUpload)) {
            log.info("no file. need upload. current file size {}", file.length());
            if (file.length() == 0) return false;
            //upload file
            Object tokenRes = tokenAlloc(tokenParam, headers, cookies);
            JSONObject jsonObject = JSONObject.parseObject(tokenRes.toString());
            JSONObject result = ((JSONObject) jsonObject.get("result"));
            String key = result.getString("objectKey");
            int replaceIndex = key.indexOf("/");
            String objectKey = key.substring(0, replaceIndex) + "%2F" + key.substring(replaceIndex + 1);
            String url = "http://45.127.129.8/jd-musicrep-privatecloud-audio-public/" + objectKey + "?offset=0&complete=true&version=1.0";
            String token = result.getString("token");
            uploadFileDo(url, file, token, md5, size);
        }
        return true;
    }

    public HttpHeaders get() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36");
        headers.add("origin", "https://music.163.com");
        headers.add("Referer", "https://music.163.com/");
        return headers;
    }


    public void uploadFileDo(String actionUrl, File file, String token, String md5, String size) {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                //服务器性能而定
                .readTimeout(30 * 60, TimeUnit.SECONDS)
                .writeTimeout(30 * 60, TimeUnit.SECONDS)
                .build();
        MediaType mediaType = MediaType.Companion.parse("multipart/form-data");
        RequestBody body = RequestBody.Companion.create(file, mediaType);
        Request request = new Request.Builder()
                .url(actionUrl)
                .method("POST", body)
                .addHeader("x-nos-token", token)
                .addHeader("Content-MD5", md5)
                .addHeader("Content-Type", "audio/mpeg")
                .addHeader("Content-Length", size)
                .build();
        try {
            Response response = client.newCall(request).execute();
            String result = response.body().string();
            log.info("uploadFileDo result [{}]", result);
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
