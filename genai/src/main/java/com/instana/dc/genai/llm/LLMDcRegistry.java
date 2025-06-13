package com.instana.dc.genai.llm;

import com.instana.dc.genai.base.AbstractGenAIDc;
import com.instana.dc.genai.llm.impl.LLMDc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class LLMDcRegistry {
    private static final Map<String, Class<? extends AbstractGenAIDc>> map;
    
    static {
        Map<String, Class<? extends AbstractGenAIDc>> tempMap = new HashMap<>();
        tempMap.put("LLM", LLMDc.class);
        // More DCs can be registered here
        map = Collections.unmodifiableMap(tempMap);
    }

    public Class<? extends AbstractGenAIDc> findLLMDc(String llmService) throws Exception {
        Class<? extends AbstractGenAIDc> cls = map.get(llmService.toUpperCase());
        if (cls != null) {
            return cls;
        } else {
            throw new Exception("Unsupported LLM system: " + llmService);
        }
    }
} 