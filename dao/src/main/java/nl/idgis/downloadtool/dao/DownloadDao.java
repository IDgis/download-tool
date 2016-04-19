/**
 * 
 */
package nl.idgis.downloadtool.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import nl.idgis.downloadtool.domain.Download;
import nl.idgis.downloadtool.domain.DownloadRequestInfo;
import nl.idgis.downloadtool.domain.DownloadResultInfo;
/**
 * @author Rob
 *
 */
public class DownloadDao {
	private static final Logger log = LoggerFactory.getLogger(DownloadDao.class);

	private final DataSource dataSource;
	
	private final Gson gson;
	
	public DownloadDao(DataSource dataSource) {
		this.dataSource =dataSource;
		this.gson = new Gson();
	}
	
	public void createDownloadRequestInfo(DownloadRequestInfo downloadRequestInfo) throws SQLException {
		String sql = "INSERT INTO request_info (request_id, request_time, job_id, download, user_name, user_emailaddress, user_format)  "+
				"VALUES(?, now(), ?, ?::jsonb, ?, ?, ?);";
		try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			log.debug("createDownloadRequestInfo sql: " + sql); 
			
			stmt.setString(1, downloadRequestInfo.getRequestId());
			stmt.setString(2, downloadRequestInfo.getJobId());
			String json = gson.toJson(downloadRequestInfo.getDownload());
			stmt.setString(3, json);
			stmt.setString(4, downloadRequestInfo.getUserName());
			stmt.setString(5, downloadRequestInfo.getUserEmailAddress());
			stmt.setString(6, downloadRequestInfo.getUserFormat());
			
			int count = stmt.executeUpdate();
			log.debug("createDownloadRequestInfo insert #records: " + count); 
		} catch (SQLException e) {
			log.error("Exception when executing sql=" + sql);
			throw e;
		}
	}

	public DownloadRequestInfo readDownloadRequestInfo(String requestId) throws SQLException {
		log.debug("retrieving download request info: {}", requestId);
		
		try(Connection conn = dataSource.getConnection(); 
			PreparedStatement stmt = conn.prepareStatement(
				"SELECT request_time, download, job_id, user_name, user_emailaddress, user_format " +
				"FROM request_info WHERE request_id=?")) {
			
			stmt.setString(1, requestId);
			
			try(ResultSet rs = stmt.executeQuery()) {
				if(rs.next()) {
					Timestamp requestTime = rs.getTimestamp(1);
					Download download = gson.fromJson(rs.getString(2), Download.class);
					String jobId = rs.getString(3);
					String userName = rs.getString(4);
					String userEmailaddress = rs.getString(5);
					String userFormat = rs.getString(6);
					
					DownloadRequestInfo downloadRequestInfo = new DownloadRequestInfo();
					downloadRequestInfo.setRequestId(requestId);
					downloadRequestInfo.setJobId(jobId);
					downloadRequestInfo.setRequestTime(requestTime);
					downloadRequestInfo.setDownload(download);
					downloadRequestInfo.setUserName(userName);
					downloadRequestInfo.setUserEmailAddress(userEmailaddress);
					downloadRequestInfo.setUserFormat(userFormat);
					
					if(rs.next()) {
						throw new IllegalStateException("multiple download request info records found");
					}
					
					log.debug("download request info object found");
					
					return downloadRequestInfo;
				} else {
					log.warn("download request info object not found");
					
					return null;
				}
			}
		} catch(SQLException e) {
			log.error("sql exception: {}", e);
			
			throw e;
		}
	}
	
	public void createDownloadResultInfo(DownloadResultInfo downloadResultInfo) throws SQLException {
		log.debug("creating download result info"); 
		
		try(Connection conn = dataSource.getConnection();
			PreparedStatement stmt = conn.prepareStatement(
				"INSERT INTO result_info(request_info_id, response_time, response_code) " + 
				"SELECT id, now(), ? FROM request_info WHERE request_id = ?")) {
			
			stmt.setString(1, downloadResultInfo.getResponseCode());
			stmt.setString(2, downloadResultInfo.getRequestId());
			
			int count = stmt.executeUpdate();
			if(count != 1) {
				throw new IllegalStateException("unexpected update count: " + count);
			}
			
			log.debug("download result info created"); 
		} catch (SQLException e) {
			log.error("sql exception: {}", e);
			
			throw e;
		}
	}
	
	public DownloadResultInfo readDownloadResultInfo(String requestId) throws SQLException {
		log.debug("retrieving download result info: {}", requestId);
		
		try(Connection conn = dataSource.getConnection(); 
			PreparedStatement stmt = conn.prepareStatement(
				"SELECT response_time, response_code FROM result_info " + 
				"WHERE request_info_id = (SELECT id FROM request_info WHERE request_id = ?)")) {
					
			stmt.setString(1, requestId);
			
			try(ResultSet rs = stmt.executeQuery()) {
				if(rs.next()) {
					Timestamp responseTime = rs.getTimestamp(1);
					String responseCode = rs.getString(2);
					
					DownloadResultInfo downloadResultInfo = new DownloadResultInfo();
					downloadResultInfo.setRequestId(requestId);
					downloadResultInfo.setResponseCode(responseCode);
					downloadResultInfo.setResponseTime(responseTime);
					
					log.debug("download result info object found");
					
					if(rs.next()) {
						throw new IllegalStateException("multiple download result info records found");
					}
					
					return downloadResultInfo;
				} else {
					log.warn("download result info object not found");
					
					return null;
				}
			}
		} catch (SQLException e) {
			log.error("sql exception: {}", e);
			
			throw e;
		}
	}
	
}
