package nl.idgis.downloadtool.domain;

import java.io.Serializable;

/**
 * Description of the wfs featuretype to download, with hints for conversion<br>
 * Will be retrieved by a http POST request.<br>
 * No means of authentication are used, so no username, password.
 * 
 * @author Rob
 *
 */
public class WfsFeatureType implements Serializable {
    private static final long serialVersionUID = 2076921005433138680L;
    
    private String uuid; // identifies the dataset 
    private String name; // name as in WFS capabilities, will be used as filename
    private String extension; // extension of the filename, can be retrieved via converter
    private String namespacePrefix; // e.g. "ps"
    private String namespaceUri; // e.g. "urn:x-inspire:specification:gmlas:ProtectedSites:3.0"
    private String filterExpression; // expression to filter out sub type from featuretype e.g. "EHS"
    private String serviceUrl; // url for post request to the wfs
    private String serviceVersion; // WFS version (needed for proper post request)
    private String crs; // Crs code e.g. "urn:ogc:def:crs:EPSG::4258"
    private String wfsMimetype; // Mimetype to retrieve from the WFS e.g. "application/gml+xml; version=3.1"
    
    /**
     * @return uuid identifies the dataset
     */
    public String getUuid() {
        return uuid;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    /**
     * @return name as in WFS capabilities, will be used as filename
     */
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * @return extension of the filename, can be retrieved via converte
     */
    public String getExtension() {
        return extension;
    }
    
    public void setExtension(String extension) {
        this.extension = extension;
    }
    
    public String getNamespacePrefix() {
        return namespacePrefix;
    }
    
    public void setNamespacePrefix(String namespacePrefix) {
        this.namespacePrefix = namespacePrefix;
    }
    
    public String getNamespaceUri() {
        return namespaceUri;
    }
    
    public void setNamespaceUri(String namespaceUri) {
        this.namespaceUri = namespaceUri;
    }

    public String getFilterExpression() {
        return filterExpression;
    }
    
    public void setFilterExpression(String filterExpression) {
        this.filterExpression = filterExpression;
    }
    
    /**
     * @return url of the wfs service 
     */
    public String getServiceUrl() {
        return serviceUrl;
    }
    
    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }
    
    /**
     * @return WFS version
     */
    public String getServiceVersion() {
        return serviceVersion;
    }
    
    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }
    
    /**
     * @return Crs code e.g. "urn:ogc:def:crs:EPSG::4258"
     */
    public String getCrs() {
        return crs;
    }
    
    public void setCrs(String crs) {
        this.crs = crs;
    }
    
    /**
     * @return Mimetype to retrieve from the WFS e.g. "application/gml+xml; version=3.1"
     */
    public String getWfsMimetype() {
        return wfsMimetype;
    }

    public void setWfsMimetype(String wfsMimetype) {
        this.wfsMimetype = wfsMimetype;
    }
    
    
}

