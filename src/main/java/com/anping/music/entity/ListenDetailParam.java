package com.anping.music.entity;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Anping Sec
 * @date 2024/3/28
 * description:
 */
@Data
public class ListenDetailParam {

    private String mid;

    private Long songID;

    private String level;

    public ListenDetailParam(String mid) {
        this.mid = mid;
        this.level = MusicQuality.AP_128.getLevel();
    }

    public ListenDetailParam(String mid, String level) {
        this.mid = mid;
        this.level = MusicQuality.getQuality(level) != null ? level : MusicQuality.AP_128.getLevel();
    }

    public ListenDetailParam(String mid, Long songID, String level) {
        this.mid = mid;
        this.songID = songID;
        this.level = MusicQuality.getQuality(level) != null ? level : MusicQuality.AP_128.getLevel();
    }
}
