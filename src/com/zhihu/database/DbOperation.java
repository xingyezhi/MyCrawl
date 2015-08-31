package com.zhihu.database;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import com.zhihu.tools.LogSys;
import com.zhihu.tools.ReadTxtFile;
import com.zhihu.tools.UTF8Filter;



public class DbOperation {
	private static String ip = "127.0.0.1";
	
	private static String driver = "com.mysql.jdbc.Driver";
	
	private static String user = "";
	
	private static String password = "";
	
	private static String databaseName = "zhihu";
	
	private static String encode = "utf-8";
	
	private static int patchCount=0;	
	private Connection connect = null;	
	Statement stmt = null;
	public static int connectionCount;

	public static String getBase(){
		String dir=System.getProperty("user.dir");
		return dir;
	}
	
	public DbOperation() {
		String base = getBase();
		ReadTxtFile rxf = new ReadTxtFile(base + "/config/properties.ini");
		ArrayList<String> vector = rxf.read();
		for (String t : vector) {
			if(t.startsWith("http.dbaddressIP")){
				String res = t.substring(t.indexOf('=') + 1);
				DbOperation.ip = res;
			}
			
			if (t.startsWith("http.dbusername")) {
				String res = t.substring(t.indexOf('=') + 1);
				DbOperation.user = res;
			} else if (t.startsWith("http.dbpassword")) {
				String res = t.substring(t.indexOf('=') + 1);
				DbOperation.password = res;
			} else if (t.startsWith("http.databasename")) {
				String res = t.substring(t.indexOf('=') + 1);
				DbOperation.databaseName = res;
			}
		}
		this.reginster();
		this.conDB();
	}
	/**
	 * 加载mysql驱动
	 */
	public void reginster() {
		try {
			Class.forName(driver); // 加载MYSQL JDBC驱动程序

		} catch (Exception e) {
			System.out.print("Error loading Mysql Driver!");
			e.printStackTrace();
		}
	}
	/**
	 * 连接数据库
	 * 
	 * @return 连接对象
	 */

	public Connection conDB(){
		Connection connect = null;
		try {
			connect = DriverManager.getConnection("jdbc:mysql://" + ip
					+ ":3306/" + databaseName
					+ "?useUnicode=true&continueBatchOnError=true&autoReconnect=true&characterEncoding=" + encode, user,
					password);
			// 连接URL为 jdbc:mysql//服务器地址/数据库名
			// 后面的2个参数分别是登陆用户名和密码
		} catch (Exception e) {
			System.out.print("Fail to access DataBase!");
			e.printStackTrace();
			System.exit(-1);
			
		}
		this.connect = connect;
		return connect;
	}
	public void createStmt() {
		try {
			stmt = this.connect.createStatement();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public Connection conDB(String ip, String databaseName, String user,String password){
		connectionCount++;
		LogSys.nodeLogger.info("创建了一个连接，总共的连接数是？？"+connectionCount);
		Connection connect = null;
		try {
			connect = DriverManager.getConnection("jdbc:mysql://" + ip
					+ ":3306/" + databaseName
					+ "?rewriteBatchedStatements=true&continueBatchOnError=true&useUnicode=true&characterEncoding=" + encode, user,
					password);
			// 连接URL为 jdbc:mysql//服务器地址/数据库名
			// 后面的2个参数分别是登陆用户名和密码
			System.out.println("Success connect Mysql server!");
			stmt = this.connect.createStatement();
		} catch (Exception e) {
			System.out.print("get data error!");
			e.printStackTrace();
		}
		this.connect = connect;
		return connect;
	}
	public boolean insertWithoutBatch(String insertSql){
		try {
			if(this.connect==null||this.connect.isClosed()){
				this.connect=conDB();
			}
			if(stmt==null||stmt.isClosed()){
				stmt=this.connect.createStatement();
			}
			//将语句插入
			if(UTF8Filter.IsAllUTF8(insertSql)){
				insertSql=UTF8Filter.CleanLeaveUTF8(insertSql);
			}
			stmt.executeUpdate(insertSql);
		}
		catch (MySQLIntegrityConstraintViolationException ex) {
			//LogSys.nodeLogger.debug("数据重复：SQL语句为："+insertSql);
			return false;
		} catch(com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException ex){
			LogSys.nodeLogger.error("SQL中含有错误标示符："+insertSql);
		}catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			LogSys.nodeLogger.error("错误：Sql语句是："+insertSql);
			System.out.println(insertSql);
			return false;
		}catch(Exception ex){
			return false;
			
		}
		return true;
	}
	
	public boolean insertRightNow(){
		try {
			if(this.connect==null||this.connect.isClosed()){
				this.connect=conDB();
			}
			if(stmt==null||stmt.isClosed()){
				stmt=this.connect.createStatement();
			}
			stmt.executeBatch();			
			return true;
		}catch(SQLException e){
			return false;
		}
	}
	
	public boolean insert(String insertSql) {
		try {
			if(this.connect==null||this.connect.isClosed()){
				this.connect=conDB();
			}
			if(stmt==null||stmt.isClosed()){
				stmt=this.connect.createStatement();
			}
			//将语句插入
			if(UTF8Filter.IsAllUTF8(insertSql)){
				insertSql=UTF8Filter.CleanLeaveUTF8(insertSql);
			}
			stmt.addBatch(insertSql);
			if(patchCount>=20){
				//connect.setAutoCommit(true);
				patchCount=0;
				stmt.executeBatch();
				stmt.clearBatch();
				//connect.commit();
				//connect.setAutoCommit(true);
			}else{
				patchCount++;
				
			}
			
			return true;
		}catch (BatchUpdateException e){
			System.err.println("正常执行的Batch/100");
			System.err.println("Error Code "+e.getErrorCode());
			if(e.getErrorCode()==1062){
				LogSys.nodeLogger.debug("数据重复with SQL("+insertSql+")");
				return false;
			}
			System.err.println("SqlState "+e.getSQLState());
			System.err.println("Message "+e.getMessage());
			e.printStackTrace();
			return false;
		} 
		catch (MySQLIntegrityConstraintViolationException ex) {
			LogSys.nodeLogger.debug("数据重复：SQL语句为："+insertSql);
			return false;
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			LogSys.nodeLogger.error("错误：Sql语句是："+insertSql);
			System.out.println(insertSql);
			return false;
		}		
	}
	public void close() {
		if (this.stmt != null) {
			try {
				this.stmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			this.stmt = null;
		}
		if (this.connect != null) {
			try {
				connect.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			connect = null;
		}

	}
	public void closeConn() {
		// TODO Auto-generated method stub
		this.close();
	}

	public void closeStmt() {
		// TODO Auto-generated method stub
		this.close();
	}
	public Connection GetConnection(){
		try {
			if(this.connect!=null&&this.connect.isValid(20)){
				return this.connect;
			}else{
				return this.conDB();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return this.conDB();
		}
	}
	
	public static void main(String []args){
		DbOperation db=new DbOperation();
		for(int i=0;i<7;i++){
			db.insert("insert into answer(author,content,agree_number,comment_number,question_id,question_title) values('旷志鑫','心诚',4,5,1,'如何做一个好人')");
		}
		db.insertRightNow();
		System.out.println("done!");
	}
}
