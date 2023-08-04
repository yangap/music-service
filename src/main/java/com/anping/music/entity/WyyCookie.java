package com.anping.music.entity;

import lombok.Data;

/**
 * @author Anping Sec
 * @date 2023/07/07
 * description:
 */
@Data
public class WyyCookie {

    private String ntes_kaola_ad = "1";

    private String channel = "netease";

    private String WEVNSM = "1.0.0";

    private String os = "osx";

    private String appver = "2.3.14";

    private String MUSIC_U;

    private String MUSIC_R_U;

    private String deviceId;

    private String __csrf;

    private String osver;

    private String csrfToken;

    private String WNMCID;

    private String NMTID;

    public String getCookie() {
        return "ntes_kaola_ad=" + ntes_kaola_ad + "; channel=" + channel + "; WEVNSM=" + WEVNSM
                + "; MUSIC_U=" + MUSIC_U + "; MUSIC_R_U=" + MUSIC_R_U + "; deviceId="
                + deviceId + "; os=" + os + "; appver=" + appver + "; __csrf=" + __csrf
                + "; osver=" + osver + "; csrfToken=" + csrfToken + "; WNMCID=" + WNMCID + "; NMTID=" + NMTID;
    }

    public WyyCookie(String cookie) {
        setCookieEntry(cookie);
    }

    public void setCookieEntry(String cookie) {
        String[] split = cookie.replaceAll(" ", "").split(";");
        for (String c : split) {
            String[] strings = c.split("=");
            String key = strings[0];
            String value = strings[1];
            switch (key) {
                case "ntes_kaola_ad":
                    this.ntes_kaola_ad = value;
                    break;
                case "channel":
                    this.channel = value;
                    break;
                case "WEVNSM":
                    this.WEVNSM = value;
                    break;
                case "MUSIC_U":
                    this.MUSIC_U = value;
                    break;
                case "MUSIC_R_U":
                    this.MUSIC_R_U = value;
                    break;
                case "deviceId":
                    this.deviceId = value;
                    break;
                case "os":
                    this.os = value;
                    break;
                case "appver":
                    this.appver = value;
                    break;
                case "__csrf":
                    this.__csrf = value;
                    break;
                case "osver":
                    this.osver = value;
                    break;
                case "csrfToken":
                    this.csrfToken = value;
                    break;
                case "WNMCID":
                    this.WNMCID = value;
                    break;
                case "NMTID":
                    this.NMTID = value;
                    break;
            }
        }
    }
}
