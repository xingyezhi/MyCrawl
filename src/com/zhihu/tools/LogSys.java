package com.zhihu.tools;

import java.text.SimpleDateFormat;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class LogSys {
	static{
		PropertyConfigurator.configure ("config/log4j_Main.properties" );
	}

	public static Logger nodeLogger=Logger.getLogger("NODE");
	public static Logger errorLogger=Logger.getLogger("ERROR");

	public static void main(String []args){
		java.util.Date data=new java.util.Date();
		SimpleDateFormat dateformat1=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss E");
		nodeLogger.info("我是NODES"+dateformat1.format(data));
		errorLogger.info("我是debugS"+dateformat1.format(data));
	}

}
