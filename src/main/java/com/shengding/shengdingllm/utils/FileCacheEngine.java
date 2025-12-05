package com.shengding.shengdingllm.utils;



import com.shengding.shengdingllm.common.SerializationUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * 文件存储换成
 *
 */
public class FileCacheEngine implements CacheEngine {

	private  static final FileManager FILE_MANAGER;
	static
	{
		FILE_MANAGER = new FileManager();
		FILE_MANAGER.init();
	}
	@Override
	public void add(String cacheName, Serializable key, Object value) {
		if(cacheName!=null&&key!=null&&value!=null){		
			try {
				byte[] data = SerializationUtils.serialize(value);
				FILE_MANAGER.add(cacheName, key, data);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	@Override
	public void add( Serializable key, Object value) {
		add(FOLDER_NAME, key, value);
	}
	@Override
	public Object get(String folderName, Serializable key) {
		if(folderName!=null&&key!=null)
		{
			try {
				byte[] data = FILE_MANAGER.get(folderName, key);
				return SerializationUtils.deserialize(data);
			} catch (IOException e) {			
				e.printStackTrace();
			}
		}
		return null;
	}
	@Override
	public Object get( Serializable key) {		
		return get(FOLDER_NAME, key);
	}

	@Override
	public <T> T get(String folderName, Serializable key,Class<?> s) {
		final Object o = get(folderName,key);
		if(null == o){
			return null;
		}
		if(s.isInstance(o)){
			return (T) s.cast(o);
		}
		return null;
	}

	@Override
	public <T> T get(Serializable key, Class<?> s) {
		final Object o = get(key);
		if(null == o){
			return null;
		}
		if(s.isInstance(o)){
			return (T) s.cast(o);
		}
		return null;
	}

	@Override
	public void remove(String folderName, Serializable key) {
		if(folderName!=null&&key!=null)
		{
			FILE_MANAGER.remove(folderName, key);
		}
		
	}
	@Override
	public void remove(Serializable key) {
		remove(FOLDER_NAME, key);
		
	}
	@Override
	public void clear(String folderName) {
		if(folderName!=null)
		{
			try {
				FILE_MANAGER.clear(folderName);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	@Override
	public void clear() {
		clear(FOLDER_NAME);
		
	}
	@Override
	public void stop() {
		try {
			FILE_MANAGER.close();
		} catch (IOException e) {		
			e.printStackTrace();
		}
		
	}
	public File[] getList(){
		return getList(FOLDER_NAME);
	}
	public File[] getAll(){
		return getList();
	}
	public File[] getList(String cacheName){
		return FILE_MANAGER.getList(cacheName);
	}

}
