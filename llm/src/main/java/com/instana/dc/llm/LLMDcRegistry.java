/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.llm;

import java.util.HashMap;
import java.util.Map;

import com.instana.dc.llm.impl.llm.LLMDc;
public class LLMDcRegistry {
    /* Add all DataCollector implementations here:
    **/
    private final Map<String, Class<? extends AbstractLLMDc>> map = new HashMap<String, Class<? extends AbstractLLMDc>>() {{
        put("LLM", LLMDc.class);
        // More DCs can be registred here
    }};

    public Class<? extends AbstractLLMDc> findLLMDc(String llmService) throws Exception {
        Class<? extends AbstractLLMDc> cls = map.get(llmService.toUpperCase());
        if (cls != null) {
            return cls;
        } else {
            throw new Exception("Unsupported LLM system: " + llmService);
        }
    }
}
