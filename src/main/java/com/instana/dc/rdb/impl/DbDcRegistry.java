/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.rdb.impl;

import com.instana.dc.rdb.AbstractDbDc;
import com.instana.dc.DcException;

import java.util.HashMap;
import java.util.Map;

public class DbDcRegistry {
    /* Add all DataCollector implementations here:
    **/
    private final Map<String, Class<? extends AbstractDbDc>> map = new HashMap<String, Class<? extends AbstractDbDc>>() {{
        put("DAMENG", DamengDc.class);
    }};

    public Class<? extends AbstractDbDc> findDatabaseDc(String dbSystem) throws DcException {
        Class<? extends AbstractDbDc> cls = map.get(dbSystem);
        if (cls != null) {
            return cls;
        } else {
            throw new DcException("Unsupported DB system: " + dbSystem);
        }
    }
}
