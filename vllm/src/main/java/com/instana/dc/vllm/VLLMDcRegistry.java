/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.vllm;

import java.util.HashMap;
import java.util.Map;

import com.instana.dc.vllm.impl.vllm.VLLMDc;
public class VLLMDcRegistry {
    /* Add all DataCollector implementations here:
    **/
    private final Map<String, Class<? extends AbstractVLLMDc>> map = new HashMap<>() {{
        put("VLLM", VLLMDc.class);
        // More DCs can be registred here
    }};

    public Class<? extends AbstractVLLMDc> findVLLMDc(String llmService) throws Exception {
        Class<? extends AbstractVLLMDc> cls = map.get(llmService.toUpperCase());
        if (cls != null) {
            return cls;
        } else {
            throw new Exception("Unsupported LLM system: " + llmService);
        }
    }
}
