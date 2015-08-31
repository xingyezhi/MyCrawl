package com.zhihu.bean;

public class Answer {
private int id;
private String author;
private String content;
private int agree_number;
private int comment_number;
private String question_title;
private String url;
public int getId() {
	return id;
}
public void setId(int id) {
	this.id = id;
}
public String getAuthor() {
	return author;
}
public void setAuthor(String author) {
	this.author = author;
}
public String getContent() {
	return content;
}
public void setContent(String content) {
	this.content = content;
}
public int getAgree_number() {
	return agree_number;
}
public void setAgree_number(int agree_number) {
	this.agree_number = agree_number;
}
public int getComment_number() {
	return comment_number;
}
public void setComment_number(int comment_number) {
	this.comment_number = comment_number;
}
public String getQuestion_title() {
	return question_title;
}
public void setQuestion_title(String question_title) {
	this.question_title = question_title;
}
public String getUrl() {
	return url;
}
public void setUrl(String url) {
	this.url = url;
}


}
