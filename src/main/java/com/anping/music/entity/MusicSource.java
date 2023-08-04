package com.anping.music.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Anping Sec
 * @date 2023/03/06
 * description:
 */
@Data
public class MusicSource implements Serializable {

    /**
     * https://y.qq.com/n/ryqq/mv/T0010Rcobut
     */
    private String pageUrl;

    /**
     * 00:00:00
     */
    private String ss;

    /**
     * 00:01:00
     */
    private String to;
}
