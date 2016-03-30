/**
 * 
 */
package nl.idgis.downloadtool.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import nl.idgis.downloadtool.domain.Download;
import nl.idgis.downloadtool.domain.DownloadRequest;
import nl.idgis.downloadtool.domain.DownloadRequestInfo;
import nl.idgis.downloadtool.domain.DownloadResultInfo;

import nl.idgis.commons.utils.DateTimeUtils;
/**
 * @author Rob
 *
 */
public class DownloadDao {
	private static final Logger log = LoggerFactory.getLogger(DownloadDao.class);

	private String dbUrl;
	private String dbUser;
	private String dbPw;
	
	private Gson gson = null;
	
	public DownloadDao(String connectionStr, String user, String pw) {
		super();
		try {
			// Register JDBC driver
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		}
		this.dbUrl = connectionStr; 
		this.dbUser = user;          
		this.dbPw = pw;  
		log.debug("dbUrl" +dbUrl);
		
		gson = new Gson();

	}
	
	private Connection getConnection() throws SQLException{
		return DriverManager.getConnection(dbUrl,dbUser,dbPw);
	}
	
	public void createDownloadRequestInfo(DownloadRequestInfo downloadRequestInfo) throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = getConnection();
			String sql;
			sql = "INSERT INTO request_info (request_id, request_time, download, user_name, user_emailaddress, user_format)  "+
				"VALUES(?, now(), ?, ?, ?, ?);";
			log.debug("createDownloadRequestInfo sql: " + sql); 
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, downloadRequestInfo.getRequestId());
			String json = gson.toJson(downloadRequestInfo.getDownload());
			stmt.setString(2, json);
			stmt.setString(3, downloadRequestInfo.getUserName());
			stmt.setString(4, downloadRequestInfo.getUserEmailAddress());
			stmt.setString(5, downloadRequestInfo.getUserFormat());
			
			int count = stmt.executeUpdate();
			log.debug("createDownloadRequestInfo insert #records: " + count); 
		} catch (SQLException e) {
			throw e;
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException se2) {
			} // nothing we can do
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				se.printStackTrace();
			} // end finally try

		}
	}

	public DownloadRequestInfo readDownloadRequestInfo(String requestId) throws SQLException{
		DownloadRequestInfo downloadRequestInfo = null;
		Connection conn = null;
		Statement stmt = null;
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			String sql;
			sql = "SELECT * FROM request_info WHERE request_id='" + requestId + "'";
			log.debug("readDownloadRequestInfo sql: " + sql); 
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				Timestamp requestTime = rs.getTimestamp("request_time");
				log.debug("readDownloadRequestInfo requestTime: " + requestTime); 
				String json = rs.getString("download");
				String uuid = rs.getString("uuid");
				String userName = rs.getString("user_name");
				String userEmailaddress = rs.getString("user_emailaddress");
				String userFormat = rs.getString("user_format");
				downloadRequestInfo = new DownloadRequestInfo();
				downloadRequestInfo.setRequestId(requestId);
				downloadRequestInfo.setRequestTime(requestTime);
				Download download = gson.fromJson(json, Download.class); 
				downloadRequestInfo.setDownload(download);
				downloadRequestInfo.setUserName(userName);
				downloadRequestInfo.setUserEmailAddress(userEmailaddress);
				downloadRequestInfo.setUserFormat(userFormat);
			}
			rs.close();
		} catch (SQLException e) {
			throw e;
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException se2) {
			} // nothing we can do
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				se.printStackTrace();
			} // end finally try

		}
		return downloadRequestInfo;
	}
	
	public void createDownloadResultInfo(DownloadResultInfo downloadResultInfo) throws SQLException{
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = getConnection();
			String sql;
			sql = "INSERT INTO result_info (request_id, response_time, response_code) VALUES(?, now(), ?);";
			log.debug("createDownloadResultInfo sql: " + sql); 
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, downloadResultInfo.getRequestId());
			stmt.setString(2, downloadResultInfo.getResponseCode());
			int count = stmt.executeUpdate();
			log.debug("createDownloadResultInfo insert #records: " + count); 
		} catch (SQLException e) {
			throw e;
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException se2) {
			} // nothing we can do
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				se.printStackTrace();
			} // end finally try

		}
	}
	
	public DownloadResultInfo readDownloadResultInfo(String requestId) throws SQLException{
		DownloadResultInfo downloadResultInfo = null;
		Connection conn = null;
		Statement stmt = null;
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			String sql;
			sql = "SELECT * FROM result_info WHERE request_id='" + requestId + "'";
			log.debug("readDownloadResultInfo sql: " + sql); 
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				Timestamp responseTime = rs.getTimestamp("response_time");
				log.debug("readDownloadResultInfo responseTime: " + responseTime); 
				String responseCode = rs.getString("response_code");
				downloadResultInfo = new DownloadResultInfo();
				downloadResultInfo.setRequestId(requestId);
				downloadResultInfo.setResponseCode(responseCode);
				downloadResultInfo.setResponseTime(responseTime);
			}
			rs.close();
		} catch (SQLException e) {
			throw e;
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException se2) {
			} // nothing we can do
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				se.printStackTrace();
			} // end finally try

		}
		return downloadResultInfo;
	}
	
}
