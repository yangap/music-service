package com.anping.music.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Anping Sec
 * @date 2023/03/06
 * description:
 */
@Data
public class MusicInfo implements Serializable {

    private String mid;

    private Long songID;

    private String title;

    private String singers;

    /**
     * 音质
     */
    private String level;

    /**
     * 总时长
     */
    private Integer interval;

    /**
     * 专辑名称
     */
    private String albumName;
    /**
     * 封面地址
     */
    private String pic;

    private String lyric;

    /**
     * 音乐地址
     */
    private String resourceUrl;

    private String localUrl;

    @JsonIgnore
    private String md5;

    @JsonIgnore
    private String uploadId;

    private String ext;

    private Long fileSize;

    /**
     * 支持的音质
     */
    private List<MusicQuality> qualityList = new ArrayList<>();

    /**
     * 获取到播放url的时间
     */
    private long listenUrlCreateTime;

    /**
     * 来源:wy,pp
     */
    private String source;

    @JsonIgnore
    private boolean belongWyySheet;

}
