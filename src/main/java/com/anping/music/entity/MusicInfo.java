package com.anping.music.entity;

import lombok.Data;

import java.io.Serializable;

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

    private String mvUrl;

    /**
     * 音乐地址
     */
    private String resourceUrl;

    private String localUrl;

    private String md5;

    private String uploadId;

    private String ext;

    private Long fileSize;

    /**
     * 获取到播放url的时间
     */
    private long listenUrlCreateTime;

    /**
     * 来源:wyy,qq
     */
    private String source;

    private boolean belongWyySheet;

}
