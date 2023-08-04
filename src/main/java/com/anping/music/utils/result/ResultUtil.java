package com.anping.music.utils.result;


public class ResultUtil {
	
	public final static Integer SUCCESS_CODE = 0;//成功编码
	public final static String SUCCESS_MSG = "成功";//成功
	public final static Integer ERROR_CODE = 1;//异常编码
	public final static String ERROR_MSG = "失败";//失败
	public final static Integer OTHER_CODE = 2;//其它
	
	/**
	 * 成功
	 * @param msg
	 * @param data
	 * @return
	 */
	public static <T> ResponseResult<T> success(String msg, T data){
		return build(SUCCESS_CODE, msg, data);
	}
	
	public static <T> ResponseResult<T> build(Integer code, String msg, T data){
		return new ResponseResult<>(code, msg, data);
	}
	
	public static <T> ResponseResult<T> success(String msg){
		return success(msg, null);
	}
	
	public static <T> ResponseResult<T> success(){
		return success(SUCCESS_MSG);
	}
	
	public static <T> ResponseResult<T> success(T data){
		return success(SUCCESS_MSG, data);
	}
	
	/**
	 * 失败
	 * @param msg
	 * @param data
	 * @return
	 */
	public static <T> ResponseResult<T> error(String msg, T data){
		return new ResponseResult<>(ERROR_CODE, msg, data);
	}
	
	public static <T> ResponseResult<T> error(String msg){
		return error(msg, null);
	}
	
	public static <T> ResponseResult<T> error(){
		return error(ERROR_MSG);
	}
	
	/**
	 * 其它错误
	 * @param msg
	 * @return
	 */
	public static <T> ResponseResult<T> other(String msg){
		return new ResponseResult<>(OTHER_CODE, msg, null);
	}
	
	public static <T> ResponseResult<T> other(Integer code, String msg){
		return new ResponseResult<>(code, msg, null);
	}
	

}
