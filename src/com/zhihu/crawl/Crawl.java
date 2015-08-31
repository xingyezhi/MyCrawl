package com.zhihu.crawl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;

import com.zhihu.database.DbOperation;

public abstract class Crawl implements Runnable{
	private static final String CHARSET = "UTF-8";
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	
	public abstract void doCrawl(String url);

	//保存网页并返回信息
	String SaveToHtml(HttpEntity entity,String fileName){       
        try{
        	 BufferedReader br=new BufferedReader(new InputStreamReader(entity.getContent(),CHARSET));
             BufferedWriter bw=new BufferedWriter(new FileWriter(fileName));
        	String t="";
        	StringBuffer sb=new StringBuffer();
        	while((t=br.readLine())!=null){
        		bw.write(t+"\n\r");
        		sb.append(t+"\n\r");
        	}
        	bw.close();
        	br.close();
        	return sb.toString();
        }catch (Exception ex){
        	ex.printStackTrace();
        	return null;
        }
        
	}
}
