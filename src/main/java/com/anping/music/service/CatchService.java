package com.anping.music.service;

import com.anping.music.entity.ListenDetailParam;
import com.anping.music.entity.MusicInfo;
import com.anping.music.entity.Page;
import org.springframework.http.HttpHeaders;

/**
 * @author Anping Sec
 * @date 2023/03/07
 * description:
 */
public interface CatchService {

    HttpHeaders get();

    Page<MusicInfo> findList(Page<MusicInfo> page);

    MusicInfo getListenDetail(ListenDetailParam listenDetailParam);

}
