package com.instana.dc.genai.vectordb;

import com.instana.dc.genai.base.AbstractGenAIDc;
import com.instana.dc.genai.vectordb.impl.VectordbDc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class VectordbDcRegistry {
    private static final Map<String, Class<? extends AbstractGenAIDc>> map;
    
    static {
        Map<String, Class<? extends AbstractGenAIDc>> tempMap = new HashMap<>();
        tempMap.put("VECTORDB", VectordbDc.class);
        // More DCs can be registered here
        map = Collections.unmodifiableMap(tempMap);
    }

    public Class<? extends AbstractGenAIDc> findVecotrdbDc(String vectordbService) throws Exception {
        Class<? extends AbstractGenAIDc> cls = map.get(vectordbService.toUpperCase());
        if (cls != null) {
            return cls;
        } else {
            throw new Exception("Unsupported Vector DB system: " + vectordbService);
        }
    }
} 