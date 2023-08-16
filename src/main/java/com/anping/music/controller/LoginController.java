package com.anping.music.controller;

import com.anping.music.utils.WyyApi;
import com.anping.music.utils.result.ResponseResult;
import com.anping.music.utils.result.ResultUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Anping Sec
 * @date 2023/08/15
 * description:
 */
@RestController
@RequestMapping("/login")
public class LoginController {

    public static final String WX = "Anping_Sec";

    @Autowired
    private WyyApi wyyApi;

    @PostMapping("/getQrKey")
    public ResponseResult<Object> getQrKey(String checkToken) {
        if (WX.equals(checkToken)) {
            return ResultUtil.success("ok!",wyyApi.getKey());
        }
        return ResultUtil.error("error!");
    }

    @PostMapping("/getCookie")
    public ResponseResult<Object> getCookie(String key) {
        return wyyApi.getCookie(key);
    }
}
