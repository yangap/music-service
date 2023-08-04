package com.anping.music.utils.result;

import java.util.List;

/**
 * @author Anping Sec
 * date 2022/02/17
 * description: 分页
 */
public class PageData<T>{

    private int pageNum=1;

    private int pageSize=10;

    private long total=0;

    private long pages=0;

    private List<T> rows;

    public PageData(){}

    public PageData(List<T> list){
        this.rows = list;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPages() {
        return pages;
    }

    public void setPages(long pages) {
        this.pages = pages;
    }

    public List<T> getRows() {
        return rows;
    }

    public void setRows(List<T> rows) {
        this.rows = rows;
    }
}
