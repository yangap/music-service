package com.anping.music.entity;

/**
 * @author Anping Sec
 * @date 2024/3/27
 * description:
 */
public enum MusicQuality {
    AP_128("128k", "C400", ".m4a","标准音质","standard"),
    AP_320("320k", "M800", ".mp3","高音质","exhigh"),
    AP_FLAC("flac", "F000", ".flac","无损音质","lossless");
    private String level;

    private String s;

    private String suffix;

    private String name;

    private String label;

    MusicQuality(String level, String s, String suffix, String name,String label) {
        this.level = level;
        this.s = s;
        this.suffix = suffix;
        this.name = name;
        this.label = label;
    }

    public static MusicQuality getQuality(String level) {
        MusicQuality[] values = MusicQuality.values();
        for (MusicQuality quality : values) {
            if (quality.getLevel().equals(level)) {
                return quality;
            }
        }
        return null;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getS() {
        return s;
    }

    public void setS(String s) {
        this.s = s;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
