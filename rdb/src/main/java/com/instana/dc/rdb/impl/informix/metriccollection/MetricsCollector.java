package com.instana.dc.rdb.impl.informix.metriccollection;

import com.instana.dc.rdb.impl.informix.OnstatCommandExecutor;

import java.sql.Connection;
import java.util.List;
import java.util.logging.Logger;

import static com.instana.dc.rdb.DbDcUtil.getMetricWithSql;
import static com.instana.dc.rdb.DbDcUtil.getSimpleMetricWithSql;


public class MetricsCollector {

    private MetricCollectionMode modeSelected;

    public MetricsCollector(MetricCollectionMode mode){
        this.modeSelected = mode;
    }
    private static final Logger LOGGER = Logger.getLogger(MetricsCollector.class.getName());

    public <T> T collectMetrics(String metricName, Connection connection, OnstatCommandExecutor onstatCommandExecutor){
        MetricCMD metricCMDs = MetricCommandMapping.getMetricCMDMap(metricName);
        T metricValue = null;
        if(metricCMDs!=null){
            switch(modeSelected){
                case SQL:
                    metricValue = collectMetricsUsingSQL(metricCMDs,connection);
                    break;
                case CMD:
                    metricValue = collectMetricsUsingCMD(metricCMDs,onstatCommandExecutor);
                    break;
                case DEFAULT:
                    this.modeSelected = metricCMDs.getDefaultMode();
                    collectMetrics(metricName,connection,onstatCommandExecutor);
                    break;
                default:
                    throw new IllegalStateException("Invalid Mode:"+modeSelected);
            }
            // Type checking to avoid mismatching of data type parsing.
            try{
                metricCMDs.getReturnType().cast(metricValue);
            }catch (ClassCastException e){
                throw new IllegalStateException("Error casting the metric",e);
            }
        }
        else{
            throw new IllegalArgumentException("Metric not found:"+metricName);
        }
        return metricValue;
    }

    private static class TypeChecker{
        public static boolean isNumber(Class<?> type){
            return Number.class.isAssignableFrom(type);
        }
        public static boolean isString(Class<?> type){
            return String.class.isAssignableFrom(type);
        }
        public static boolean isList(Class<?> type){
            return List.class.isAssignableFrom(type);
        }
    }
    private static <T> T collectMetricsUsingSQL(MetricCMD metricCMD, Connection connection){
        if(TypeChecker.isNumber(metricCMD.getReturnType())){
            return (T) getSimpleMetricWithSql(connection,metricCMD.getSqlCMD());
        }
        else if(TypeChecker.isList(metricCMD.getReturnType())){
            return (T) getMetricWithSql(connection,metricCMD.getSqlCMD());
        }
        return  null;
    }

    private <T> T collectMetricsUsingCMD(MetricCMD metricCMD,OnstatCommandExecutor onstatCommandExecutor){
        return (T) onstatCommandExecutor.executeCommand(metricCMD.getCmdLine())[0]; // Need to verify this
    }
}
