package com.anping.music.entity;

import lombok.Data;

/**
 * @author Anping Sec
 * @date 2023/07/30
 * description:
 */
@Data
public class QqCookie {

    private String uin;

    private String qm_keyst;

    private String qqmusic_key;

    private String psrf_qqrefresh_token;

    private String psrf_qqaccess_token;

    private String time = "";

    private Integer musickeyCreateTime;

    private Integer keyExpiresIn;

    public String getUin() {
        return uin;
    }

    public void setUin(String uin) {
        this.uin = uin;
    }

    public String getQm_keyst() {
        return qm_keyst;
    }

    public void setQm_keyst(String qm_keyst) {
        this.qm_keyst = qm_keyst;
        this.qqmusic_key = qm_keyst;
    }

    public String getQqmusic_key() {
        return qqmusic_key;
    }

    public void setQqmusic_key(String qqmusic_key) {
        this.qm_keyst = qqmusic_key;
        this.qqmusic_key = qqmusic_key;
    }

    public String getPsrf_qqrefresh_token() {
        return psrf_qqrefresh_token;
    }

    public void setPsrf_qqrefresh_token(String psrf_qqrefresh_token) {
        this.psrf_qqrefresh_token = psrf_qqrefresh_token;
    }

    public String getPsrf_qqaccess_token() {
        return psrf_qqaccess_token;
    }

    public void setPsrf_qqaccess_token(String psrf_qqaccess_token) {
        this.psrf_qqaccess_token = psrf_qqaccess_token;
    }

    public QqCookie(String cookie){
        setCookieEntry(cookie);
    }

    public void refreshTime(Integer createTime,Integer interval){
        this.musickeyCreateTime = createTime;
        this.keyExpiresIn = interval;
        long end = createTime+interval;
        time = "psrf_musickey_createtime="+createTime+";psrf_access_token_expiresAt="+end+";";
    }

    public String getCookie() {
        return time+"uin=" + uin + "; qm_keyst=" + qm_keyst + "; qqmusic_key=" + qqmusic_key
                + "; psrf_qqrefresh_token=" + psrf_qqrefresh_token + "; psrf_qqaccess_token=" + psrf_qqaccess_token;
    }

    public void setCookieEntry(String cookie) {
        String[] split = cookie.replaceAll(" ", "").split(";");
        for (String c : split) {
            String[] strings = c.split("=");
            String key = strings[0];
            String value = strings[1];
            switch (key) {
                case "uin":
                    this.uin = value;
                    break;
                case "qm_keyst":
                    this.qm_keyst = value;
                    break;
                case "qqmusic_key":
                    this.qqmusic_key = value;
                    break;
                case "psrf_qqrefresh_token":
                    this.psrf_qqrefresh_token = value;
                    break;
                case "psrf_qqaccess_token":
                    this.psrf_qqaccess_token = value;
                    break;
            }
        }
    }
}
