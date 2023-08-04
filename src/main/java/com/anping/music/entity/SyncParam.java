package com.anping.music.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author Anping Sec
 * @date 2023/07/26
 * description:
 */
@Data
public class SyncParam implements Serializable {

    private String uid;

    private String sheetId;

    private String userCookie;

    private List<MusicInfo> musicInfoList;

    public SyncParam(String uid, String sheetId, String userCookie, List<MusicInfo> musicInfoList) {
        this.uid = uid;
        this.sheetId = sheetId;
        this.userCookie = userCookie;
        this.musicInfoList = musicInfoList;
    }
}
