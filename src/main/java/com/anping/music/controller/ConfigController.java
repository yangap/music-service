package com.anping.music.controller;

import com.anping.music.entity.CookieParam;
import com.anping.music.service.TokenRefresh;
import com.anping.music.utils.CookieCache;
import com.anping.music.utils.result.ResponseResult;
import com.anping.music.utils.result.ResultUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author Anping Sec
 * @date 2023/03/07
 * description:
 */
@RestController
@RequestMapping("/config")
@Slf4j
public class ConfigController {

    @Autowired
    private CookieCache cookieCache;

    @PostMapping("/setCookie")
    public Object configTicket(@RequestBody CookieParam cookieParam){
        ResponseResult<Object> result = null;
        try{
            int update = 0;
            if(StringUtils.isNotEmpty(cookieParam.getWyyCookie())){
                update++;
                cookieCache.setVipCookie(cookieParam.getWyyCookie());
                log.info("set wyy cookie success.");
            }
            if(StringUtils.isNotEmpty(cookieParam.getQqCookie())){
                update++;
                cookieCache.setQqVipCookie(cookieParam.getQqCookie());
                log.info("set qq cookie success.");
            }
            result = ResultUtil.success(update);
        }catch (Exception e){
            e.printStackTrace();
            result = ResultUtil.error("error!");
        }
        return result;
    }
}
