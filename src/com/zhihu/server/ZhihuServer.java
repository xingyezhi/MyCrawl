package com.zhihu.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;

import com.zhihu.crawl.ZhihuAnswerCrawl;
import com.zhihu.crawl.ZhihuClientManager;
import com.zhihu.crawl.ZhihuLogManager;
import com.zhihu.crawl.ZhihuUserProfileCrawl;

public class ZhihuServer {
//用户的队列
public static Set<String> userset=Collections.synchronizedSet(new HashSet<String>());
public static List<String> ulist=Collections.synchronizedList(new ArrayList<String>());
//问题的队列
public static Set<String> quesset=Collections.synchronizedSet(new HashSet<String>());

//答案的队列
public static List<String> alist=Collections.synchronizedList(new ArrayList<String>());
	
public static void main(String []args){
	System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog"); 
	System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true"); 
	System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "stdout"); 
	ZhihuClientManager cm=new ZhihuClientManager();
	userset.add("http://www.zhihu.com/people/kaiserwang730/about");
	userset.add("http://www.zhihu.com/people/yixiao-feng-yun-guo/about");
	userset.add("http://www.zhihu.com/people/kuang-zhi-xin/about");
	ulist.add("http://www.zhihu.com/people/kaiserwang730/about");
	ulist.add("http://www.zhihu.com/people/yixiao-feng-yun-guo/about");
	ulist.add("http://www.zhihu.com/people/kuang-zhi-xin/about");
	
	
	userset.add("http://www.zhihu.com/people/jixin/about");
	ulist.add("http://www.zhihu.com/people/jixin/about");
	
	userset.add("http://www.zhihu.com/people/zhang-jia-wei/about");
	ulist.add("http://www.zhihu.com/people/zhang-jia-wei/about");
	
	userset.add("http://www.zhihu.com/people/zord-vczh/about");
	ulist.add("http://www.zhihu.com/people/zord-vczh/about");
	
	userset.add("http://www.zhihu.com/people/chenran/about");
	ulist.add("http://www.zhihu.com/people/chenran/about");
	
	userset.add("http://www.zhihu.com/people/qin.chao/about");
	ulist.add("http://www.zhihu.com/people/qin.chao/about");
	for(int i=0;i<8;i++){
	DefaultHttpClient httpclient = cm.getClientNoProxy();
	httpclient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
	httpclient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 10000); 
	ZhihuUserProfileCrawl profile=new ZhihuUserProfileCrawl(httpclient);
	ZhihuLogManager log=new ZhihuLogManager(httpclient);
	if(log.trylogin()){
		Thread t1=new Thread(profile);
		t1.start();
	}
	}
	for(int i=0;i<8;i++){
		DefaultHttpClient httpclient = cm.getClientNoProxy();
		httpclient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
		httpclient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 10000); 
		ZhihuAnswerCrawl profile=new ZhihuAnswerCrawl(httpclient);
		ZhihuLogManager log=new ZhihuLogManager(httpclient);
		if(log.trylogin()){
			Thread t1=new Thread(profile);
			t1.start();
		}
		
		}
}
}
