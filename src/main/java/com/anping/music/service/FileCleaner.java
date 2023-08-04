package com.anping.music.service;

import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Anping Sec
 * @date 2023/07/24
 * description:
 */
@Component
public class FileCleaner {
    private final Set<String> paths = new HashSet<>();

    public void record(String filePath) {
        paths.add(filePath);
    }

    public void clean() {
        for (String path : paths) {
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
        }
        paths.clear();
    }
}
