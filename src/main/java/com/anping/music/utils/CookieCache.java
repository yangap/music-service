package com.anping.music.utils;

import com.anping.music.entity.QqCookie;
import com.anping.music.entity.WyyCookie;
import org.springframework.stereotype.Component;

/**
 * @author Anping Sec
 * @date 2023/06/26
 * description:
 */
@Component
public class CookieCache {

    private WyyCookie wyyCookie;

    private QqCookie qqCookie;

    public void setQqVipCookie(String qqVipCookie) {
        qqCookie = new QqCookie(qqVipCookie);
    }

    public void setVipCookie(String vipCookie) {
        wyyCookie = new WyyCookie(vipCookie);
    }

    public WyyCookie getWyyCookie() {
        if (this.wyyCookie == null) {
            throw new RuntimeException("wyy cookie not config...");
        }
        return wyyCookie;
    }

    public QqCookie getQqCookie() {
        if (this.qqCookie == null) {
            throw new RuntimeException("qq cookie not config...");
        }
        return qqCookie;
    }

    public String getVipCookie() {
        return getWyyCookie().getCookie();
    }

    public String getQqVipCookie() {
        return getQqCookie().getCookie();
    }

    public boolean qqAccess() {
        return this.qqCookie != null;
    }

    public boolean wyyAccess() {
        return this.wyyCookie != null;
    }
}
