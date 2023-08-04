package com.anping.music.utils.result;

import java.io.Serializable;

/**
 * 数据传输对象（restful格式）
 */
public class ResponseResult<T> implements Serializable {
	private static final long serialVersionUID = 4783587157568739265L;

	private Integer code;/*返回状态的编码*/
	
	private String message;/*返回消息*/
	
	private T data;/*返回数据*/
	
	public ResponseResult() {
		super();
	}

	public ResponseResult(Integer code, String msg, T data) {
		super();
		this.code = code;
		this.message = msg;
		this.data = data;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}
	
}
