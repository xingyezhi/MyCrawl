package com.zhihu.crawl;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

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

import com.zhihu.bean.Answer;
import com.zhihu.bean.Question;
import com.zhihu.bean.User;
import com.zhihu.database.DbOperation;
import com.zhihu.server.ZhihuServer;
import com.zhihu.tools.LogSys;

public class ZhihuAnswerCrawl extends Crawl{
	private DefaultHttpClient httpclient;
	public DbOperation dboperation;
	public ZhihuAnswerCrawl(DefaultHttpClient _httpclient){
		this.httpclient=_httpclient;
		dboperation=new DbOperation();
		
	}
	@Override
	public void doCrawl(String url) {
		try {
			LogSys.nodeLogger.debug("准备进行下载网页");
			HttpGet httpget = new HttpGet(url);
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			String content=SaveToHtml(entity,"Output/Zhihu/UserProfile.html");
			LogSys.nodeLogger.debug("下载网页完成，正在提取信息");
			int end=url.lastIndexOf("answer");
			String qurl=url.substring(20,end);
			String question_url="http://www.zhihu.com"+qurl;
			boolean question_complete=true;
			if(!ZhihuServer.quesset.contains(question_url)){
				question_complete=false;
				ZhihuServer.quesset.add(question_url);
			}

			analysis(content,url,qurl,question_complete);
		} catch (ClientProtocolException e) {
			LogSys.errorLogger.error("出现错误");
			LogSys.errorLogger.error(url);
			relog();
		} catch (IOException e) {
			LogSys.errorLogger.error("出现错误");
			LogSys.errorLogger.error(url);
		}catch(Exception e){
			LogSys.errorLogger.error("其他未知错误");
			LogSys.errorLogger.error(url);
		}
	}
	
	public void analysis(String content,String url,String qurl,boolean question_complete){
		if(content.contains("你似乎来到了没有知识存在的荒原..."))
			return;
		Document doc=Jsoup.parse(content,"/");
		Elements e;
			if(!question_complete){
			Question q=new Question();
			e=doc.select("h2[class=zm-item-title zm-editable-content]");
			q.setTitle(e.text());
			e=doc.select("#zh-question-detail");
			Element sib=e.get(0);
			q.setSummary(sib.text());
			e=doc.select("a[class=zm-item-tag]");
			StringBuilder tag=new StringBuilder();
			tag.append("|");
			for(Element l:e){
				tag.append(l.text()+"|");
			}
			q.setTag(tag.toString());
			q.setUrl("http://www.zhihu.com"+qurl);
			e=doc.select("a[href="+qurl+"followers]");
			if(e==null||e.toString().equals(""))
				q.setFocus_number(0);
			else
				q.setFocus_number(Integer.parseInt(e.text()));
			QuestionSaveToDB(q);
		}
			Answer ans=new Answer();
			e=doc.select("h2[class=zm-list-content-title]");
			System.out.println("ans： "+url);
			ans.setAuthor(e.text());
			System.out.println(ans.getAuthor());
			e=doc.select(".zm-item-vote-info");
			int agree=Integer.parseInt(e.get(0).attr("data-votecount"));
			System.out.println(agree);
			ans.setAgree_number(agree);
			e=doc.select(".zm-editable-content").select(".clearfix");
			System.out.println(e.text());
			ans.setContent(e.text());
			e=doc.select("h2[class=zm-item-title zm-editable-content]");
			ans.setQuestion_title(e.text());
			e=doc.select(".toggle-comment").select(".meta-item");
			
			int commet;
			try {
				commet = Integer.parseInt(e.get(1).text().split(" ")[0]);
			} catch (Exception e1) {
				commet=0;
			}
			ans.setComment_number(commet);
			ans.setUrl(url);
			AnswerSaveToDB(ans);
	}
	//重新登录
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
//保存问题
	private void QuestionSaveToDB(Question q){
		Connection con=dboperation.GetConnection();
		try {
			PreparedStatement question=con.prepareStatement("INSERT INTO question(title,tag,summary,focus_number,url) "
					+ "VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);
			question.setString(1, q.getTitle());
			question.setString(2, q.getTag());
			question.setString(3, q.getSummary());
			question.setInt(4, q.getFocus_number());
			question.setString(5, q.getUrl());
			question.executeUpdate();
			question.close();
			} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	//保存答案
		private void AnswerSaveToDB(Answer a){
			Connection con=dboperation.GetConnection();
			try {
				PreparedStatement answer=con.prepareStatement("INSERT INTO answer(author,content,agree_number,comment_number,question_title,url) "
						+ "VALUES(?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);
				answer.setString(1, a.getAuthor());
				answer.setString(2, a.getContent());
				answer.setInt(3, a.getAgree_number());
				answer.setInt(4, a.getComment_number());
				answer.setString(5, a.getQuestion_title());
				answer.setString(6, a.getUrl());
				answer.executeUpdate();
				answer.close();
				} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
	public void run() {
		// TODO Auto-generated method stub
		while(true){
		if(ZhihuServer.alist.size()!=0){
			String s=ZhihuServer.alist.remove(0);
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
public static void main(String []args){
	System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog"); 
	System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true"); 
	System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "stdout"); 
	ZhihuClientManager cm=new ZhihuClientManager();
	DefaultHttpClient httpclient = cm.getClientNoProxy();
	httpclient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
	httpclient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 10000); 
	ZhihuAnswerCrawl profile=new ZhihuAnswerCrawl(httpclient);
	ZhihuLogManager log=new ZhihuLogManager(httpclient);
	if(log.trylogin()){
		profile.doCrawl("http://www.zhihu.com/question/34836703/answer/60118983");
	}
}
}
