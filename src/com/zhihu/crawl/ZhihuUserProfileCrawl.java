package com.zhihu.crawl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.zhihu.bean.User;
import com.zhihu.database.DbOperation;
import com.zhihu.server.ZhihuServer;
import com.zhihu.tools.LogSys;

public class ZhihuUserProfileCrawl extends Crawl{
	private DefaultHttpClient httpclient;
	public DbOperation dboperation;

	private String follows;//那些人关注他
	private String follow;//关注了那些人
	private String answerhome;//那些他回答的问题链接
	private String username;
	
	public ZhihuUserProfileCrawl(DefaultHttpClient _httpclient){
		this.httpclient=_httpclient;
		dboperation=new DbOperation();
		
	}
	@Override
	public void doCrawl(String url) {
		try {
			// TODO Auto-generated method stub
			LogSys.nodeLogger.debug("准备进行下载网页");
			HttpGet httpget = new HttpGet(url);
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			String content=SaveToHtml(entity,"Output/Zhihu/UserProfile.html");
			LogSys.nodeLogger.debug("下载网页完成，正在提取信息");
			User user=new User();
			analysis(content,user);
			LogSys.nodeLogger.debug("提取信息完成，正在写入数据库");
			saveToDB(user);
			
			//获取Follow的用户列表
			httpget = new HttpGet(follow);
			response = httpclient.execute(httpget);
			entity = response.getEntity();
			content=SaveToHtml(entity,"Output/Zhihu/Follow.html");
			ArrayList<String>FollowList=ProduceUrl(content,false);
			for(String st:FollowList){
				if(!ZhihuServer.userset.contains(st))
					{ZhihuServer.userset.add(st);ZhihuServer.ulist.add(st);}
			}
			//获取Follows的用户列表
			httpget = new HttpGet(follows);
			response = httpclient.execute(httpget);
			entity = response.getEntity();
			content=SaveToHtml(entity,"Output/Zhihu/Follows.html");
			ArrayList<String>FollowsList=ProduceUrl(content,true);
			for(String st:FollowsList){
				if(!ZhihuServer.userset.contains(st))
					{ZhihuServer.userset.add(st);ZhihuServer.ulist.add(st);}
			}
			
			ArrayList<String> aslist=this.ProduceAnswer();
			for(String s:aslist)
				ZhihuServer.alist.add(s);
			
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			LogSys.errorLogger.error("下载网页失败:重新登录");
			LogSys.errorLogger.error(url);
			relog();
		} catch (IOException e) {
			LogSys.errorLogger.equals("保存网页失败");
			LogSys.errorLogger.error(url);
		}catch(Exception e){
			LogSys.errorLogger.error("其他未知错误");
			LogSys.errorLogger.error(url);
		}
		
	}
	//解析网页获取用户信息
	private void analysis(String content,User user){
		Document doc=Jsoup.parse(content, "/");
		Elements  e=doc.select("a[class=name]");
		String url=e.attr("href");
		answerhome=url;
		follow="http://www.zhihu.com"+url+"/followees";
		follows="http://www.zhihu.com"+url+"/followers";
		user.setUrl("http://www.zhihu.com"+url+"/about");
	//	System.out.println("用户名:"+e.text());
		user.setUsername(e.text());
		username=e.text();
		e=doc.select("span[class=location item]");
	//	System.out.println("地址："+e.text());
		user.setLocation(e.text());
		e=doc.select("span[class=business item]");
		//System.out.println("职业："+e.text());
		user.setBusiness(e.text());
		e=doc.select("span[class=employment item]");
		user.setEmployment(e.text());
		e=doc.select("span[class=position item]");
		user.setPosition_item(e.text());
		e=doc.select("span[class=education-extra item]");
		user.setEducation_extra_item(e.text());
		e=doc.select("span[class=item gender]");
		System.out.println("**"+e);
		if(e.toString().equals("")||e==null)
			user.setGender("male");
		else{
			Element gender=e.get(0).child(0);
		if(gender.attr("class").equals("icon icon-profile-female"))
			user.setGender("female");
		else
			user.setGender("male");
		}
		e=doc.select("span[class=education item]");
		//System.out.println("教育经历"+e.text());
		user.setEducation(e.text());
		e=doc.select("span[class=description unfold-item]");
		//System.out.println("简介"+e.text());
		user.setSummary(e.text());
		e=doc.select("a[href="+url+"/followees]");
		System.out.println("users:"+url);
		//System.out.println(url+"/followees");
		if(e.toString().equals("")||e==null){
			user.setFollow(0);
		}
		else{
		System.out.println("e"+e);
		Elements chirlds=e.get(0).children();
		for(Element l:chirlds){
			if (l.tagName().equals("strong"))
				user.setFollow(Integer.parseInt(l.text()));
				//System.out.println("关注了:"+l.text());
		}
		}
		e=doc.select("a[href="+url+"/followers]");
		if(e.toString().equals("")||e==null){
			user.setFollows(0);
		}else{
			Elements chirlds=e.get(0).children();
		for(Element l:chirlds){
			if (l.tagName().equals("strong"))
				user.setFollows(Integer.parseInt(l.text()));
		}
		}
		e=doc.select("i[class=zm-profile-icon zm-profile-icon-vote]");
		Element sib=e.get(0).nextElementSibling();
		//System.out.println(sib);
		//System.out.println("赞同数 :"+sib.child(0).text());
		user.setAgree_number(Integer.parseInt(sib.child(0).text()));
		e=doc.select("i[class=zm-profile-icon zm-profile-icon-thank]");
		sib=e.get(0).nextElementSibling();
		//System.out.println("感谢数:"+sib.childNode(0).childNode(0));
		user.setThanks_number(Integer.parseInt(sib.child(0).text()));
		e=doc.select("span[class=num]");
		//System.out.println("提问数"+e.get(0).text());
		user.setQuestion_number(Integer.parseInt(e.get(0).text()));
		//System.out.println("回答数"+e.get(1).text());
		user.setAnswer_number(Integer.parseInt(e.get(1).text()));
	}

	private boolean CheckValidation(String content){
		if(content==null||content.length()<=1||content.contains("Sorry, that page doesn’t exist")){
			LogSys.errorLogger.error("Profile采集_账户被冻结");
			return false;
		}
		return true;
	}

	//保持用户信息到数据库
	private void saveToDB(User u){
		Connection con=dboperation.GetConnection();
		try {
			PreparedStatement userprofile=con.prepareStatement("INSERT INTO user(username,location,business,employment,position_item,gender,education,education_extra_item,follows,follow,agree_number,thanks_number,question_number,answer_number,"
					+ "summary,url) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);
			userprofile.setString(1, u.getUsername());
			userprofile.setString(2, u.getLocation());
			userprofile.setString(3, u.getBusiness());
			userprofile.setString(4, u.getEmployment());
			userprofile.setString(5, u.getPosition_item());;
			userprofile.setString(6, u.getGender());
			userprofile.setString(7, u.getEducation());
			userprofile.setString(8, u.getEducation_extra_item());
			userprofile.setInt(9, u.getFollows());
			userprofile.setInt(10, u.getFollow());
			userprofile.setInt(11, u.getAgree_number());
			userprofile.setInt(12, u.getThanks_number());
			userprofile.setInt(13, u.getQuestion_number());;
			userprofile.setInt(14, u.getAnswer_number());
			userprofile.setString(15, u.getSummary());
			userprofile.setString(16, u.getUrl());
			System.out.println(userprofile);
			userprofile.executeUpdate();
			userprofile.close();
			} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	//保存用户关系
	private void saveToDB(String u,ArrayList<String>vlist,String type){
		Connection con=dboperation.GetConnection();
		try {
			PreparedStatement relationship=con.prepareStatement("INSERT INTO relationship(user1,user2,link_type) VALUES(?,?,?)",Statement.RETURN_GENERATED_KEYS);
			for(String v:vlist){
			relationship.setString(1, u);
			relationship.setString(2, v);
			relationship.setString(3, type);
			relationship.executeUpdate();
			}
			relationship.close();
			} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	//保存Follows和Follow信息
	private ArrayList<String> ProduceUrl(String content,boolean type){
		Document doc=Jsoup.parse(content, "/");
		ArrayList<String> result=new ArrayList<String>();
		Elements e=doc.select("h2[class=zm-list-content-title]");
		ArrayList<String>other=new ArrayList<String>();
		for(Element l:e){
			result.add(l.child(0).attr("href")+"/about");
			other.add(l.text());
		}
		
		if(!type)
			saveToDB(username,other,"true");
		else
			saveToDB(username,other,"false");
		return result;
	}
	
	private ArrayList<String> ProduceAnswer(){
		ArrayList<String>result=new ArrayList<String>();
		try {
			String url="http://www.zhihu.com"+answerhome+"/answers";
			HttpGet httpget = new HttpGet(url);
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			String content=SaveToHtml(entity,"Output/Zhihu/answer.html");
			Document doc=Jsoup.parse(content, "/");
			Elements e=doc.select("a[class=question_link]");
			for(Element l:e){
				result.add("http://www.zhihu.com"+l.attr("href"));
			}
		} catch (ClientProtocolException e) {
			LogSys.errorLogger.error("获取网页失败，重新登录");
			relog();
		} catch (IOException e) {
			LogSys.errorLogger.error("获取网页失败，重新登录");
			relog();
		}	
		return result;
	}
	public void run() {
		// TODO Auto-generated method stub
		while(true){
		if(ZhihuServer.ulist.size()!=0){
			String s=ZhihuServer.ulist.remove(0);
			System.out.println(s);
			doCrawl(s);						
		}
		try {
			Thread.sleep(700);
		} catch (InterruptedException e) {
			LogSys.errorLogger.error("线程出问题了");
			e.printStackTrace();
		}
		}
		
	}
	public void relog(){
		ZhihuLogManager log=new ZhihuLogManager(httpclient);
		while(!log.trylogin()){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	public static void main(String []args)throws Exception{
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog"); 
		System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true"); 
		System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "stdout"); 
		ZhihuClientManager cm=new ZhihuClientManager();
		DefaultHttpClient httpclient = cm.getClientNoProxy();
		httpclient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
		httpclient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 10000); 
		ZhihuUserProfileCrawl profile=new ZhihuUserProfileCrawl(httpclient);
		ZhihuLogManager log=new ZhihuLogManager(httpclient);
		if(log.trylogin()){
			profile.doCrawl("http://www.zhihu.com/people/gao-xing-37-50/about");
		}
		
		
//		StringBuilder bs=new StringBuilder();
//		BufferedReader re=new BufferedReader(new FileReader("Output/Zhihu/UserProfile.html"));
//		String line="";
//		while((line=re.readLine())!=null){
//			bs.append(line);
//		}
//		User u=new User();
//		profile.analysis(bs.toString(), u);
//		profile.saveToDB(u);
		

		
	}
}
