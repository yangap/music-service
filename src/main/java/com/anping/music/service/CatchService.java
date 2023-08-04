package com.anping.music.service;

import com.anping.music.entity.MusicInfo;
import com.anping.music.entity.MusicSource;
import com.anping.music.entity.Page;
import org.springframework.http.HttpHeaders;

import java.util.List;

/**
 * @author Anping Sec
 * @date 2023/03/07
 * description:
 */
public interface CatchService {

    HttpHeaders get();

    List<MusicInfo> findList(Page page);

    MusicInfo getListenDetail(String mid,Long songID);

    String getByMV(MusicSource musicSource);

}
