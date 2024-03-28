package com.anping.music.entity;

import lombok.Data;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Anping Sec
 * @date 2023/03/07
 * description:
 */
@Data
public class Page<T> implements Serializable {

    private int pageNum = 1;

    private int PageSize = 10;

    private long total;

    private String key;

    List<T> data = new ArrayList<>();
}
