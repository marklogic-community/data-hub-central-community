package com.marklogic.dhf.config;

import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "marklogic")
public class MarkLogicConfiguration {

    private String host;
    private int port;
    private String database;
    private String user;
    private String password;
    private String auth;
    private boolean ssl;
    private int batch;
    private int threads;
    private Set<String> collections;

    private List<FileLoad> fileLoadList;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public int getBatch() {
        return batch;
    }

    public void setBatch(int batch) {
        this.batch = batch;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public Set<String> getCollections() {
        return collections;
    }

    public void setCollections(Set<String> collections) {
        this.collections = collections;
    }

    public List<FileLoad> getFileLoadList() {
        return fileLoadList;
    }

    public void setFileLoadList(List<FileLoad> fileLoadList) {
        this.fileLoadList = fileLoadList;
    }
}