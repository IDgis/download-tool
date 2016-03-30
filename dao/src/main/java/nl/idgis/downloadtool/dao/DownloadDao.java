/**
 * 
 */
package nl.idgis.downloadtool.dao;

import java.sql.Connection;
import java.sql.DriverManager;
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
		
		gson = new Gson();

	}
	
	private Connection getConnection() throws SQLException{
		return DriverManager.getConnection(dbUrl,dbUser,dbPw);
	}
	
	public void createDownloadRequestInfo(DownloadRequestInfo downloadRequestInfo) throws SQLException {
		Connection conn = null;
		Statement stmt = null;
		downloadRequestInfo.setRequestTime(DateTimeUtils.now());
		try {
			conn = getConnection();
			
			
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
			ResultSet rs = stmt.executeQuery(sql);
			int rowcount = 0;
			if (rs.last()) {
			  rowcount = rs.getRow();
			  rs.beforeFirst(); // not rs.first() because the rs.next() below will move on, missing the first element
			}
			if (rowcount == 1) {
				Timestamp requestTime = null;
				String json = null;
				String uuid = null;
				String userName = null;
				String userEmailaddress = null;
				String userFormat = null;

				while (rs.next()) {
					requestTime = rs.getTimestamp("request_time");
					json = rs.getString("download");
					uuid = rs.getString("uuid");
					userName = rs.getString("user_name");
					userEmailaddress = rs.getString("user_emailaddress");
					userFormat = rs.getString("user_format");
				}
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
		Statement stmt = null;
		downloadResultInfo.setResponseTime(DateTimeUtils.now());
		try {
			conn = getConnection();
			
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
			sql = "SELECT * FROM request_info WHERE request_id='" + requestId + "'";
			ResultSet rs = stmt.executeQuery(sql);
			int rowcount = 0;
			if (rs.last()) {
			  rowcount = rs.getRow();
			  rs.beforeFirst(); // not rs.first() because the rs.next() below will move on, missing the first element
			}
			if (rowcount == 1) {
				Timestamp responseTime = null;
				String responseCode = null;
				while (rs.next()) {
					responseTime = rs.getTimestamp("response_time");
					responseCode = rs.getString("response_code");
				}
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
