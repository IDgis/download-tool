package nl.idgis.downloadtool.queue;

import nl.idgis.downloadtool.domain.DownloadRequest;

/**
 * The DownloadQueue describes sending and receiving downloadrequests<br>
 * <br>
 * 
 * @author Rob
 *
 */
public interface DownloadQueue {

	/**
	 * Put a download description in the request queue.
	 * @param downloadRequest
	 */
	void sendDownloadRequest(DownloadRequest downloadRequest);

	/**
	 * Get a download description from the request queue. 
	 * @return DownloadRequest
	 */
	DownloadRequest receiveDownloadRequest();

	/**
	 * The download has ended and can be removed from the request queue.<br>
	 * The parameter is the same object as received from the receiveDownloadRequest() method.
	 * @param downloadRequest
	 * @throws Exception when the downloadrequest was already deleted from the queue
	 * @see #receiveDownloadRequest()
	 */
	void deleteDownloadRequest(DownloadRequest downloadRequest) throws Exception;

}