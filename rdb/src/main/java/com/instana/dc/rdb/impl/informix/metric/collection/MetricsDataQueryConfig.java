/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */

package com.instana.dc.rdb.impl.informix.metric.collection;
import static com.instana.dc.rdb.DbDcUtil.getMetricWithSql;
import static com.instana.dc.rdb.DbDcUtil.getSimpleMetricWithSql;
import com.instana.dc.SimpleQueryResult;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.dbcp2.BasicDataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * This class which holds the meta data of a given metrics
 */
public class MetricsDataQueryConfig {

    private static final Logger logger = Logger.getLogger(MetricsDataQueryConfig.class.getName());
    private String query;
    private final Class<?> returnType;
    private String metricKey;
    private String scriptName;
    private final BasicDataSource dataSource;
    private  ResultSet rs;
    private String[] attr;

    private List<List<SimpleQueryResult>> results;


    public MetricsDataQueryConfig(String query, Class<?> returnType, BasicDataSource dataSource,String... attr) {
        this.query = query;
        this.returnType = returnType;
        this.attr = attr;
        this.dataSource = dataSource;
        this.results = new ArrayList<>();
        for (int attrIndex = 0;attrIndex<attr.length;attrIndex++) {
            this.results.add(new ArrayList<>());
        }
    }

    public void fetchQueryResults() {
        try (Connection connection = this.dataSource.getConnection()) {

            ResultSet rs = executeQuery(connection, this.query);
                if (rs.isClosed()) {
                    logger.severe("getMetricWithSql: ResultSet is closed");
                }
                while (rs.next()) {
            Object obj = rs.getObject(2);
            if (obj == null) {
                obj = "null";
            }
            if (obj instanceof String) {
                obj = ((String) obj).trim();
            }
            for(int attrIndex = 0;attrIndex<this.attr.length;attrIndex++) {
                if(attrIndex == 1) {
                    continue;
                }
                SimpleQueryResult result = new SimpleQueryResult((Number) rs.getObject(attrIndex+1));
                result.setAttribute(this.attr[1], obj);
                result.setKey(obj.toString());
                this.results.get(attrIndex).add(result);
            }
        }
        }
        catch (SQLException exp) {
            logger.log(Level.SEVERE, "Unable to execute the sql command, Exception: " + exp);
        }
    }

    public static ResultSet executeQuery(Connection connection, String query) throws SQLException {
        Statement statement = connection.createStatement();
        return statement.executeQuery(query);
    }


    public String getMetricKey() {
        return metricKey;
    }

    public String[] getAttr() {
        return attr;
    }

    public String getQuery() {
        return query;
    }

    public Class<?> getReturnType() {
        return returnType;
    }


    public List<SimpleQueryResult> getResults(int n) {
        return this.results.get(n);
    }
}
