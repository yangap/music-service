package com.anping.music.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Anping Sec
 * @date 2023/03/07
 * description:
 */
@Data
public class Page implements Serializable {

    private int pageNum=1;

    private int PageSize=10;

    private String key;
}
