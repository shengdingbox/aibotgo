package com.shengding.shengdingllm.utils;

import java.io.Serializable;

/**
 * 缓存
 * @author tngou@tngou.net
 *
 */
public interface CacheEngine {

    String FOLDER_NAME ="default";
    /**
     * 添加缓存
     * @param folderName
     * @param key 
     * @param value
     */
	public  void add(String folderName, Serializable key, Object value);
	public  void add(Serializable key, Object value);
	/**
	 * 取得缓存
	 * @param folderName
	 * @param key
	 * @return
	 */
	public  Object get(String folderName, Serializable key);
	public  Object get(Serializable key);
	public  <T> T get(String folderName, Serializable key,Class<?> s);
	public  <T> T get(Serializable key,Class<?> s);
	/**
	 * 删除缓存
	 * @param folderName
	 * @param key
	 */
	public  void remove(String folderName, Serializable key);
	public  void remove(Serializable key);
	/**
	 * 清空
	 * @param folderName
	 */
	public  void clear(String folderName) ;
	public  void clear() ;
	/**
	 * 关闭
	 */
	public  void stop() ;
}
