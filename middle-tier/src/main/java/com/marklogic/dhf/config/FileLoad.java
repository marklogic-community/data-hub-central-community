package com.marklogic.dhf.config;

import java.util.List;

public class FileLoad {

    private String path;
    private String baseUri;
    private String collection;
    private ServerTransformConfig serverTransformConfig;
    private DataHubFlowConfig dataHubFlowConfig;
    private List<FixedFieldProp> fixedFieldProps;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getBaseUri() {
        return baseUri;
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public ServerTransformConfig getServerTransformConfig() {
        return serverTransformConfig;
    }

    public void setServerTransformConfig(ServerTransformConfig serverTransformConfig) {
        this.serverTransformConfig = serverTransformConfig;
    }

    public DataHubFlowConfig getDataHubFlowConfig() {
        return dataHubFlowConfig;
    }

    public void setDataHubFlowConfig(DataHubFlowConfig dataHubFlowConfig) {
        this.dataHubFlowConfig = dataHubFlowConfig;
    }

    public List<FixedFieldProp> getFixedFieldProps() {
        return fixedFieldProps;
    }

    public void setFixedFieldProps(List<FixedFieldProp> fixedFieldProps) {
        this.fixedFieldProps = fixedFieldProps;
    }
}
