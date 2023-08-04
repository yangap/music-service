package com.anping.music.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Anping Sec
 * @date 2023/07/31
 * description:
 */
@Data
public class CookieParam implements Serializable {

    private String wyyCookie;

    private String qqCookie;
}
