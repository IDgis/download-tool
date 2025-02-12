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
		String sql = "INSERT INTO request_info (request_id, request_time, job_id, download, user_format)  "+
				"VALUES(?, now(), ?, ?::jsonb, ?);";
		try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			log.debug("createDownloadRequestInfo sql: " + sql); 
			
			stmt.setString(1, downloadRequestInfo.getRequestId());
			stmt.setString(2, downloadRequestInfo.getJobId());
			String json = gson.toJson(downloadRequestInfo.getDownload());
			stmt.setString(3, json);
			stmt.setString(4, downloadRequestInfo.getUserFormat());
			
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
				"SELECT download, user_format " +
				"FROM request_info WHERE request_id = ?")) {
			
			stmt.setString(1, requestId);
			
			try(ResultSet rs = stmt.executeQuery()) {
				if(rs.next()) {
					Download download = gson.fromJson(rs.getString(1), Download.class);
					String userFormat = rs.getString(2);
					
					DownloadRequestInfo downloadRequestInfo = new DownloadRequestInfo();
					downloadRequestInfo.setRequestId(requestId);
					downloadRequestInfo.setDownload(download);
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
			
			int count, retriesLeft = 3;
			long sleepTime = 500;
			for (;;) {
				stmt.setString(1, downloadResultInfo.getResponseCode());
				stmt.setString(2, downloadResultInfo.getRequestId());
				count = stmt.executeUpdate();
				if (count != 1) {
					if (retriesLeft > 0) {
						log.debug("request info not found, retrying");
						Thread.sleep(sleepTime);
						
						sleepTime *= 2;
						retriesLeft--;
					} else {
						throw new IllegalStateException("request info not found");
					}
				} else {
					log.debug("download result info created");
					return;
				}
			}
		} catch (SQLException e) {
			log.error("sql exception: {}", e);
			
			throw e;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
