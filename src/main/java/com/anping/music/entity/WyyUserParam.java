package com.anping.music.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author Anping Sec
 * @date 2023/06/27
 * description:
 */
@Data
public class WyyUserParam implements Serializable {

    private String userCookie;

    private String uid;

    private String nickname;

    private String level;

    /**
     * 是否同步歌单
     */
    private boolean syncSheet;

    private List<MusicInfo> addMusics;
}
