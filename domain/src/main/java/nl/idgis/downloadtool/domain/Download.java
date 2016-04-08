package nl.idgis.downloadtool.domain;

import java.io.Serializable;
import java.util.List;

/**
 * Bean containing information concerning downloads for a specific dataset/<br>
 * A download exists of a WFS download and 0 or more additional downloads.<br>
 * 
 * 
 * @author Rob
 *
 */
public class Download implements Serializable {
	private static final long serialVersionUID = 1012331026914569758L;

	private String name; // name of the zip file where all individual downloads
							// are packaged
	private WfsFeatureType ft; // description of the featuretype to download
	private List<AdditionalData> additionalData; // list of non-conversion data

	/**
	 * @return name of the zip file where all individual downloads are packaged
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the featuretype to download
	 */
	public WfsFeatureType getFt() {
		return ft;
	}

	public void setFt(WfsFeatureType ft) {
		this.ft = ft;
	}

	/**
	 * @return list of additional non-wfs data to download
	 */
	public List<AdditionalData> getAdditionalData() {
		return additionalData;
	}

	public void setAdditionalData(List<AdditionalData> additionalData) {
		this.additionalData = additionalData;
	}

}
