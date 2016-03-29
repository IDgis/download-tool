package nl.idgis.downloadtool.domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Bean containing feedback information on individual download requests.
 * 
 * @author Rob
 *
 */
public class Feedback implements Serializable{
    
    private static final long serialVersionUID = -3600596933670573586L;
 
    /**
     * requestId is uniek Id van dit download verzoek, wordt gebruikt bij terugmeldingen
     */
    String requestId;
    
    /**
     * Result code
     */
    String resultCode;


    /**
     * @param id unique identifier of this downloadrequest.<br>
     * Equal to DownloadRequest.requestId
     */
    public Feedback(String id) {
        super();
        this.requestId = id;
    }


    public String getRequestId() {
        return requestId;
    }


	public String getResultCode() {
		return resultCode;
	}


	public void setResultCode(String resultCode) {
		this.resultCode = resultCode;
	}


	@Override
	public String toString() {
		return "Feedback [requestId=" + requestId + ", resultCode=" + resultCode + "]";
	}

}
