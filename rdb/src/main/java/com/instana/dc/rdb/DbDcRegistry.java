/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.rdb;

import com.instana.dc.DcException;
import com.instana.dc.rdb.impl.DamengDc;
import com.instana.dc.rdb.impl.InformixDc;
import com.instana.dc.rdb.impl.Oceanbase4Dc;

import java.util.HashMap;
import java.util.Map;

public class DbDcRegistry {
    /**
     * Add all DataCollector implementations should Register Here:
     */
    private final Map<String, Class<? extends AbstractDbDc>> map = new HashMap<String, Class<? extends AbstractDbDc>>() {{
        put("DAMENG", DamengDc.class);
        put("INFORMIX", InformixDc.class);
        put("OCEANBASE4", Oceanbase4Dc.class);
    }};

    public Class<? extends AbstractDbDc> findDatabaseDc(String dbSystem) throws DcException {
        Class<? extends AbstractDbDc> cls = map.get(dbSystem.toUpperCase());
        if (cls != null) {
            return cls;
        } else {
            throw new DcException("Unsupported DB system: " + dbSystem);
        }
    }
}
