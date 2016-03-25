/**
 * 
 */
package nl.idgis.downloadtool.dao;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.idgis.downloadtool.domain.DownloadRequestInfo;
import nl.idgis.downloadtool.domain.DownloadResultInfo;

import nl.idgis.commons.utils.DateTimeUtils;
/**
 * @author Rob
 *
 */
public class DownloadDao {
	private static final Logger log = LoggerFactory.getLogger(DownloadDao.class);
	
	// TODO remove this temporary store when db is available
	private static Map<String, DownloadRequestInfo> downloadRequestInfos = new HashMap<String, DownloadRequestInfo>();
	private static Map<String, DownloadResultInfo> downloadResultInfos = new HashMap<String, DownloadResultInfo>();
	
	public DownloadDao() {
		super();
		
	}
	
	public void createDownloadRequestInfo(DownloadRequestInfo downloadRequestInfo){
		downloadRequestInfo.setRequestTime(DateTimeUtils.now());
		downloadRequestInfos.put(downloadRequestInfo.getRequestId(), downloadRequestInfo);
	}
	
	public DownloadRequestInfo readDownloadRequestInfo(String requestId){
		return downloadRequestInfos.get(requestId);
	}
	
	public void createDownloadResultInfo(DownloadResultInfo downloadResultInfo){
		downloadResultInfo.setResponseTime(DateTimeUtils.now());
		downloadResultInfos.put(downloadResultInfo.getRequestId(), downloadResultInfo);
	}
	
	public DownloadResultInfo readDownloadResultInfo(String requestId){
		return downloadResultInfos.get(requestId);
	}
	
}
