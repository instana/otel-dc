package com.instana.dc.rdb.impl.informix.metriccollection;

import java.util.function.Consumer;

public class MetricCMD implements Consumer<MetricCollectionMode> {
    private final String sqlCMD;
    private final String cmdLine;

    private MetricCollectionMode defaultMode;
    private final Class<?> returnType;
    MetricCMD(String sqlCmd, String cmdLine, MetricCollectionMode mode, Class<?> returnType){
        this.sqlCMD = sqlCmd;
        this.cmdLine = cmdLine;
        this.defaultMode = mode;
        this.returnType = returnType;
        accept(defaultMode);
    }
    public String getSqlCMD(){
        return sqlCMD;
    }
    public String getCmdLine(){
        return cmdLine;
    }
    public void setDefaultMode(MetricCollectionMode mode){
        this.defaultMode = mode;
    }
    public MetricCollectionMode getDefaultMode() {
        return defaultMode;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    @Override
    public void accept(MetricCollectionMode mode) {
        if(mode!=MetricCollectionMode.CMD&&mode!=MetricCollectionMode.SQL){
            throw new IllegalArgumentException("Invalid Mode");
        }
        return;
    }
}
