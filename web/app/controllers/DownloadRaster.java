package controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import play.Configuration;
import play.mvc.Controller;
import play.mvc.Result;

import views.html.datasetmissing;

public class DownloadRaster extends Controller {
	private static final Logger log = LoggerFactory.getLogger(DownloadRaster.class);
	
	private final WebJarAssets webJarAssets;
	
	private final Configuration config;
	
	private final DriverManagerDataSource dataSource;
	
	private final String rasterDirectory = "/var/lib/geo-publisher/raster/";
	
	@Inject
	public DownloadRaster(WebJarAssets webJarAssets, Configuration config) {
		this.webJarAssets = webJarAssets;
		this.config = config;
		
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("org.postgresql.Driver");
		dataSource.setUrl(System.getenv("PUB_DB_URL"));
		dataSource.setUsername(System.getenv("PUB_DB_USER"));
		dataSource.setPassword(System.getenv("PUB_DB_PW"));
		
		this.dataSource = dataSource;
	}
	
	public Result get(String fileIdentification) throws SQLException, IOException {
		
		String sql = "select identification, source_dataset_id, name from publisher.dataset where metadata_file_identification = ?;";
		
		try(Connection c = dataSource.getConnection(); PreparedStatement stmt = c.prepareStatement(sql)) {
			stmt.setString(1, fileIdentification);
			
			try(ResultSet rs = stmt.executeQuery()) {
				if(rs.next()) {
					String identification = rs.getString(1);
					Integer sourceDatasetId = rs.getInt(2);
					String name = rs.getString(3);
					
					if("extern".equals(config.getString("download.access")) && isDatasetConfidential(sourceDatasetId)) {
						return forbidden();
					}
					
					String filePath = rasterDirectory + identification + ".tif";
					
					Path path = Paths.get(filePath);
					
					try {
						byte[] bytes = Files.readAllBytes(path);
						
						response().setHeader("Content-Disposition", "attachment; filename=" + name + ".tif");
						
						return ok(bytes).as("image/tiff");
					} catch (IOException ioe) {
						log.debug(ioe.getMessage());
						return notFound(datasetmissing.render(webJarAssets, identification));
					}
				} else {
					return notFound(datasetmissing.render(webJarAssets, fileIdentification));
				}
			} catch(SQLException se) {
				log.debug(se.getMessage());
				return notFound(datasetmissing.render(webJarAssets, fileIdentification));
			}
			
		} catch (SQLException se) {
			log.debug(se.getMessage());
			return notFound(datasetmissing.render(webJarAssets, fileIdentification));
		}
	}
	
	public boolean isDatasetConfidential(Integer sourceDatasetId) {
		String sql = "select sdv.confidential from publisher.source_dataset sd "
				+ "join publisher.source_dataset_version sdv on sdv.source_dataset_id = sd.id "
				+ "where sdv.id = "
					+ "(select max(id) from publisher.source_dataset_version sdv2 "
						+ "where sdv2.source_dataset_id = sd.id) "
				+ "and sd.id = ?";
		
		try(Connection c = dataSource.getConnection(); PreparedStatement stmt = c.prepareStatement(sql)) {
			stmt.setInt(1, sourceDatasetId);
			
			try(ResultSet rs = stmt.executeQuery()) {
				if(rs.next()) {
					boolean confidential = rs.getBoolean(1);
					
					if(confidential) {
						log.info("dataset is confidential");
					} else {
						log.info("dataset is not confidential");
					}
					
					return confidential;
				} else {
					return true;
				}
			} catch(SQLException se) {
				log.debug(se.getMessage());
				return true;
			}
			
		} catch (SQLException se) {
			log.debug(se.getMessage());
			return true;
		}
	}
}