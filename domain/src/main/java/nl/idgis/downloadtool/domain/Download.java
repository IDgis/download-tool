package nl.idgis.downloadtool.domain;

import java.io.Serializable;
import java.util.List;

/**
 * Bean containing information concerning downloads for a specific dataset/<br>
 * A download exists of a WFS download with conversion and 0 or more additional downloads without conversion step.<br>
 * 
 * 
 * @author Rob
 *
 */
public class Download implements Serializable {
    private static final long serialVersionUID = 1012331026914569758L;

    String name; // name of the zip file where all individual downloads are packaged
    WfsFeatureType ft; // description of the featuretype to download
    List<AdditionalData> additionalData; // list of non-conversion data

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public WfsFeatureType getFt() {
        return ft;
    }

    public void setFt(WfsFeatureType ft) {
        this.ft = ft;
    }

    public List<AdditionalData> getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(List<AdditionalData> additionalData) {
        this.additionalData = additionalData;
    }
    

}

