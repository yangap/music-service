package com.anping.music.service;

import com.anping.music.entity.MusicInfo;
import com.anping.music.entity.SyncParam;

/**
 * @author Anping Sec
 * @date 2023/07/26
 * description: 歌曲上传成功回调
 */
public interface UploadCloudSuccess {

    public void call(MusicInfo musicInfo, SyncParam syncParam);
}
