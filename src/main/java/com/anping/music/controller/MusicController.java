package com.anping.music.controller;

import com.anping.music.config.SpringContextHolder;
import com.anping.music.entity.*;
import com.anping.music.service.CatchService;
import com.anping.music.service.impl.WyyServiceImpl;
import com.anping.music.utils.WyyApi;
import com.anping.music.utils.result.ResponseResult;
import com.anping.music.utils.result.ResultUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Anping Sec
 * @date 2023/03/07
 * description:
 */
@RestController
@RequestMapping("/music")
@Slf4j
public class MusicController {
    @Value("${sync.limit}")
    private Integer syncLimit;

    @Autowired
    private WyyApi wyyApi;

    @Autowired
    private WyyServiceImpl wyyService;
    private long preTime = System.currentTimeMillis();
    AtomicInteger cnt = new AtomicInteger(0);

    AtomicInteger syncCnt = new AtomicInteger(0);

    @GetMapping("/search")
    public ResponseResult<Object> search(Page<MusicInfo> page, @RequestParam String source) {
        if (StringUtils.isEmpty(page.getKey())) {
            return ResultUtil.success(new ArrayList<>());
        }
        log.info("search {} from {}", page.getKey(), source);
        CatchService catchService = ((CatchService) SpringContextHolder.getBean(source));
        Page<MusicInfo> pageList = catchService.findList(page);
        return ResultUtil.success(pageList);
    }

    @GetMapping("/getTotalDownload")
    public ResponseResult<Object> getTotalDownload() {
        return ResultUtil.success(DownloadInfo.totalDownload.get());
    }

    @GetMapping("/getListenDetail")
    public ResponseResult<MusicInfo> getListenUrl(@RequestParam String source, @RequestParam String mid, String quality, Long songID) {
        ResponseResult<MusicInfo> data = ResultUtil.success("ok!");
        CatchService catchService = ((CatchService) SpringContextHolder.getBean(source));
        data.setData(catchService.getListenDetail(new ListenDetailParam(mid, songID, quality)));
        return data;
    }

    @PostMapping("/findAllSheetByUid")
    public ResponseResult<Object> findAllSheetByUid(WyyUserParam wyyUserParam) {
        String userCookie = wyyUserParam.getUserCookie();
        if (StringUtils.isEmpty(userCookie)) {
            return ResultUtil.success(new ArrayList<>());
        }
        String uid = wyyApi.getUid(userCookie);
        return ResultUtil.success(wyyApi.findPlayList(uid, userCookie));
    }

    @PostMapping("/findSongsBySheetId")
    public ResponseResult<List<MusicInfo>> findSongsBySheetId(@RequestParam String sheetId, WyyUserParam wyyUserParam) {
        String userCookie = wyyUserParam.getUserCookie();
        if (StringUtils.isEmpty(userCookie) || StringUtils.isEmpty(wyyApi.getUid(userCookie))) {
            return ResultUtil.error("请先绑定用户!");
        }
        List<MusicInfo> musicInfos = wyyApi.songList(sheetId, wyyUserParam.getUserCookie());
        return ResultUtil.success(musicInfos);
    }

    @PostMapping("/checkUser")
    public ResponseResult<Object> checkUser(@RequestBody WyyUserParam wyyUserParam) {
        String userCookie = wyyUserParam.getUserCookie();
        if (StringUtils.isEmpty(userCookie)) {
            return ResultUtil.error("凭证不能为空!");
        }
        String uid = wyyApi.getUid(userCookie);
        log.info("uid {}", uid);
        if (StringUtils.isEmpty(uid)) {
            return ResultUtil.error("用户不存在或凭证已失效!");
        }
        return ResultUtil.success("ok!");
    }

    @PostMapping("/findDailyPush")
    public ResponseResult<List<MusicInfo>> findDailyPush(@RequestBody WyyUserParam wyyUserParam) {
        String userCookie = wyyUserParam.getUserCookie();
        if (StringUtils.isEmpty(userCookie)) {
            return ResultUtil.error("用户cookie不能为空!");
        }
        if (!userCookie.contains("NMTID") || !userCookie.contains("MUSIC_U")) {
            return ResultUtil.error("cookie非法!");
        }
        List<MusicInfo> dailyPushByUser = wyyApi.getDailyPushByUser(userCookie);
        return ResultUtil.success(dailyPushByUser);
    }

    @PostMapping("/sync")
    public ResponseResult<Object> sync(@RequestBody WyyUserParam wyyUserParam) {
        String userCookie = wyyUserParam.getUserCookie();
        if (StringUtils.isEmpty(userCookie)) {
            return ResultUtil.error("用户凭证不能为空!");
        }
        if (!userCookie.contains("NMTID") || !userCookie.contains("MUSIC_U")) {
            return ResultUtil.error("凭证非法!");
        }
        String uid = wyyApi.getUid(userCookie);
        wyyUserParam.setUid(uid);
        log.info("uid {}", uid);
        if (StringUtils.isEmpty(uid) || !wyyApi.existUser(wyyUserParam)) {
            return ResultUtil.error("用户不存在或凭证已失效!");
        }
        if (wyyService.isRunning(uid)) {
            return ResultUtil.error("您有任务正在处理中，请稍后再重试!");
        }
        //校验今天可用次数
        long now = System.currentTimeMillis();
        if (now - preTime >= 24 * 60 * 60 * 1000) {
            cnt = new AtomicInteger(0);
            preTime = now;
        }
        if (cnt.get() > syncLimit) {
            return ResultUtil.error("服务器今日同步次数已达到上限");
        }
        try {
            wyyService.syncUserSheet(wyyUserParam);
        } catch (Exception e) {
            log.error(e.toString());
            return ResultUtil.error("error!");
        } finally {
            cnt.incrementAndGet();
            syncCnt.incrementAndGet();
            log.info("today sync counter：{}", cnt.get());
        }
        return ResultUtil.success("同步任务发起成功,请等待!");
    }

    @GetMapping("/syncNum")
    public ResponseResult<Integer> syncNum() {
        return ResultUtil.success(syncCnt.get());
    }
}
