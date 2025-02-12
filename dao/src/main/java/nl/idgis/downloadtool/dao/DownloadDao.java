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
}
