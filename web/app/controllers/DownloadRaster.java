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

import play.mvc.Controller;
import play.mvc.Result;

import views.html.datasetmissing;

public class DownloadRaster extends Controller {
	private static final Logger log = LoggerFactory.getLogger(DownloadRaster.class);
	
	private final WebJarAssets webJarAssets;
	
	private final String rasterDirectory = "/var/lib/geo-publisher/raster/";
	
	@Inject
	public DownloadRaster(WebJarAssets webJarAssets) {
		this.webJarAssets = webJarAssets;
	}
	
	public Result get(String id) throws SQLException, IOException {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("org.postgresql.Driver");
		dataSource.setUrl(System.getenv("PUB_DB_URL"));
		dataSource.setUsername(System.getenv("PUB_DB_USER"));
		dataSource.setPassword(System.getenv("PUB_DB_PW"));
		
		String sql = "select identification, name from publisher.dataset where metadata_file_identification = ?;";
		
		try(Connection c = dataSource.getConnection(); PreparedStatement stmt = c.prepareStatement(sql)) {
			stmt.setString(1, id);
			
			try(ResultSet rs = stmt.executeQuery()) {
				if(rs.next()) {
					String identification = rs.getString(1);
					String name = rs.getString(2);
					
					String filePath = rasterDirectory + identification + ".tif";
					
					Path path = Paths.get(filePath);
					
					try {
						byte[] bytes = Files.readAllBytes(path);
						
						response().setHeader("Content-Disposition", "attachment; filename=" + name + ".tif");
						
						return ok(bytes).as("image/tiff");
					} catch (IOException ioe) {
						log.debug(ioe.getMessage());
						return notFound(datasetmissing.render(webJarAssets, id));
					}
				} else {
					return notFound(datasetmissing.render(webJarAssets, id));
				}
			} catch(SQLException se) {
				log.debug(se.getMessage());
				return notFound(datasetmissing.render(webJarAssets, id));
			}
			
		} catch (SQLException se) {
			log.debug(se.getMessage());
			return notFound(datasetmissing.render(webJarAssets, id));
		}
	}
}