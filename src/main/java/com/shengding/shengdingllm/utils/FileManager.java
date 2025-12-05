package com.shengding.shengdingllm.utils;



import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;


/**
 * 文件处理
 *
 */
public class FileManager {
	private static final String CacheName = "default";
	private static final String Cache= System.getProperty("user.home") + "/fileCache";
	private static final File DIRECTORY = new File(Cache);
	/**
	 * 初始化
	 */
	public void init(){	
		if(!DIRECTORY.isDirectory()) {
			DIRECTORY.mkdir();
		}
	}
	/**
	 * 关闭
	 * @throws IOException 
	 */
	public void close() throws IOException
	{	
		if(DIRECTORY.isDirectory()) {
			FileUtil.deleteFiles(Cache,1);
		}
	}
	
	/**
	 * 添加缓存
	 * @param cacheName
	 * @param key
	 * @param data
	 * @throws IOException
	 */
	public void add(String cacheName, Serializable key, byte[] data) throws IOException {
		File file=new File(DIRECTORY, cacheName+File.separator+key.toString());
		FileUtil.writeByteArrayToFile(file, data);
	}
	public void add(Serializable key, byte[] data) throws IOException {
		add(CacheName, key, data);
	}
	
	/**
	 * 取得缓存
	 * @param cacheName 缓存名称
	 * @param key
	 * @return
	 * @throws IOException
	 */
	public byte[]  get(String cacheName, Serializable key) throws IOException {
		File file=new File(DIRECTORY, cacheName+File.separator+key.toString());
		if(!file.isFile()) {
			return null;
		}
		return FileUtil.toByteArray(new FileInputStream(file));
	}
	public byte[]  get( Serializable key) throws IOException {	
		return get(CacheName, key);
	}
	
	/**
	 * 移除缓存
	 * @param cacheName
	 * @param key
	 */
	public void remove(String cacheName, Serializable key) {
		File file=new File(DIRECTORY, cacheName+File.separator+key.toString());
		if(file.isFile()) {
			final boolean delete = file.delete();
		}
	}
	public void remove( Serializable key) {
		remove(CacheName, key);
	}
	
	/**
	 * 清空缓存
	 * @param cacheName
	 * @throws IOException
	 */
	public void clear(String cacheName) throws IOException
	{
		File file=new File(DIRECTORY, cacheName);
		if(file.isDirectory()) {
			FileUtil.deleteFiles(Cache,1);
		}
	}
	public void clear() throws IOException
	{
		clear(CacheName);
	}
	public File[] getList(String cacheName){
		File file=new File(DIRECTORY, cacheName);
		return file.listFiles();
	}
	public File[] getList(){
		return DIRECTORY.listFiles();
	}
	public static void main(String[] args) {
		final FileCacheEngine fileCacheEngine = new FileCacheEngine();
		fileCacheEngine.add("12","213");
		System.out.println(fileCacheEngine.get("12"));
		fileCacheEngine.remove("12");
		System.out.println(fileCacheEngine.get("12"));
		System.out.println(Arrays.toString(fileCacheEngine.getList()));

	}
}
