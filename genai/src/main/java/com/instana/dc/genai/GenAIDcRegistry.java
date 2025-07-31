package com.instana.dc.genai;

import com.instana.dc.genai.base.AbstractGenAIDc;
import com.instana.dc.genai.llm.impl.LLMDc;

import com.instana.dc.genai.vectordb.milvus.impl.MilvusDc;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class GenAIDcRegistry {
    private static final Map<String, Class<? extends AbstractGenAIDc>> map;
    
    static {
        Map<String, Class<? extends AbstractGenAIDc>> tempMap = new HashMap<>();
        // Register LLM DCs
        tempMap.put("LLM", LLMDc.class);
        // Register Milvus DCs
        tempMap.put("MILVUS", MilvusDc.class);

        // More DCs can be registered here
        map = Collections.unmodifiableMap(tempMap);
    }

    public Class<? extends AbstractGenAIDc> findGenAIDc(String service) throws Exception {
        Class<? extends AbstractGenAIDc> cls = map.get(service.toUpperCase());
        if (cls != null) {
            return cls;
        } else {
            throw new Exception("Unsupported GenAI system: " + service);
        }
    }
}
