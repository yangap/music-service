package com.anping.music.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Anping Sec
 * @date 2023/08/15
 * description: 歌单
 */
@Data
public class Sheet implements Serializable {

    private Long id;

    private String name;

    private String coverImg;

    private Integer status;
}
