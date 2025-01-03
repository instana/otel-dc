/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc.llm;

//import static com.instana.agent.sensorsdk.semconv.SemanticAttributes.*;

public class LLMDcUtil {
    //private static final Logger logger = Logger.getLogger(LLMDcUtil.class.getName());

    /* Configurations for the Data Collector:
     */
    public static final String DEFAULT_INSTRUMENTATION_SCOPE = "instana.sensor-sdk.dc.llm";
    public static final String DEFAULT_INSTRUMENTATION_SCOPE_VER = "1.0.0";
    public final static String LLM_PRICES_PROPERTIES = "config/prices.properties";
    public final static String SERVICE_LISTEN_PORT = "otel.service.port";
    public final static String OTEL_AGENTLESS_MODE = "otel.agentless.mode";

    /* Configurations for Metrics:
     */
    public static final String UNIT_S = "s";
    public static final String UNIT_BY = "By";
    public static final String UNIT_1 = "1";

    public static final String LLM_USER_NAME = "llm.request.user";
    public static final String LLM_USER_DESC = "The user for watson api call";
    public static final String LLM_USER_UNIT = "{user}";

    public static final String LLM_MODEL_ID_NAME = "llm.request.modelId";
    public static final String LLM_MODEL_ID_DESC = "The model id of the watsonx calls";
    public static final String LLM_MODEL_ID_UNIT = "{modelId}";

    public static final String LLM_STATUS_NAME = "llm.status";
    public static final String LLM_STATUS_DESC = "The status of the watsonx dc";
    public static final String LLM_STATUS_UNIT = "{status}";

    public static final String LLM_DURATION_NAME = "llm.response.duration";
    public static final String LLM_DURATION_DESC = "The average duration of watsonx calls by interval";
    public static final String LLM_DURATION_UNIT = "ms";

    public static final String LLM_DURATION_MAX_NAME = "llm.response.duration.max";
    public static final String LLM_DURATION_MAX_DESC = "The maximum duration of watsonx calls by interval";
    public static final String LLM_DURATION_MAX_UNIT = "ms";

    public static final String LLM_COST_NAME = "llm.usage.cost";
    public static final String LLM_COST_DESC = "The total cost of watsonx calls by interval";
    public static final String LLM_COST_UNIT = "{cost}";

    public static final String LLM_INPUT_COST_NAME = "llm.usage.input_cost";
    public static final String LLM_INPUT_COST_DESC = "The input cost of watsonx calls by interval";
    public static final String LLM_INPUT_COST_UNIT = "{cost}";

    public static final String LLM_OUTPUT_COST_NAME = "llm.usage.output_cost";
    public static final String LLM_OUTPUT_COST_DESC = "The output cost of watsonx calls by interval";
    public static final String LLM_OUTPUT_COST_UNIT = "{cost}";
 
    public static final String LLM_TOKEN_NAME = "llm.usage.total_tokens";
    public static final String LLM_TOKEN_DESC = "The total tokens of watsonx calls by interval";
    public static final String LLM_TOKEN_UNIT = "{token}";

    public static final String LLM_INPUT_TOKEN_NAME = "llm.usage.input_tokens";
    public static final String LLM_INPUT_TOKEN_DESC = "The input tokens of watsonx calls by interval";
    public static final String LLM_INPUT_TOKEN_UNIT = "{token}";

    public static final String LLM_OUTPUT_TOKEN_NAME = "llm.usage.output_tokens";
    public static final String LLM_OUTPUT_TOKEN_DESC = "The output tokens of watsonx calls by interval";
    public static final String LLM_OUTPUT_TOKEN_UNIT = "{token}";

    public static final String LLM_REQ_COUNT_NAME = "llm.request.count";
    public static final String LLM_REQ_COUNT_DESC = "The total count of watsonx calls by interval";
    public static final String LLM_REQ_COUNT_UNIT = "{count}";

    public static final String LLM_SERVICE_DURATION_NAME = "llm.service.response.duration";
    public static final String LLM_SERVICE_DURATION_DESC = "The average duration of watsonx calls by interval";
    public static final String LLM_SERVICE_DURATION_UNIT = "ms";

    public static final String LLM_SERVICE_DURATION_MAX_NAME = "llm.service.response.duration.max";
    public static final String LLM_SERVICE_DURATION_MAX_DESC = "The maximum duration of watsonx calls by interval";
    public static final String LLM_SERVICE_DURATION_MAX_UNIT = "ms";

    public static final String LLM_SERVICE_COST_NAME = "llm.service.usage.cost";
    public static final String LLM_SERVICE_COST_DESC = "The total cost of watsonx calls by interval";
    public static final String LLM_SERVICE_COST_UNIT = "{cost}";

    public static final String LLM_SERVICE_INPUT_COST_NAME = "llm.service.usage.input_cost";
    public static final String LLM_SERVICE_INPUT_COST_DESC = "The input cost of watsonx calls by interval";
    public static final String LLM_SERVICE_INPUT_COST_UNIT = "{cost}";

    public static final String LLM_SERVICE_OUTPUT_COST_NAME = "llm.service.usage.output_cost";
    public static final String LLM_SERVICE_OUTPUT_COST_DESC = "The output cost of watsonx calls by interval";
    public static final String LLM_SERVICE_OUTPUT_COST_UNIT = "{cost}";

    public static final String LLM_SERVICE_TOKEN_NAME = "llm.service.usage.total_tokens";
    public static final String LLM_SERVICE_TOKEN_DESC = "The total tokens of watsonx calls by interval";
    public static final String LLM_SERVICE_TOKEN_UNIT = "{token}";

    public static final String LLM_SERVICE_INPUT_TOKEN_NAME = "llm.service.usage.input_tokens";
    public static final String LLM_SERVICE_INPUT_TOKEN_DESC = "The input tokens of watsonx calls by interval";
    public static final String LLM_SERVICE_INPUT_TOKEN_UNIT = "{token}";

    public static final String LLM_SERVICE_OUTPUT_TOKEN_NAME = "llm.service.usage.output_tokens";
    public static final String LLM_SERVICE_OUTPUT_TOKEN_DESC = "The output tokens of watsonx calls by interval";
    public static final String LLM_SERVICE_OUTPUT_TOKEN_UNIT = "{token}";

    public static final String LLM_SERVICE_REQ_COUNT_NAME = "llm.service.request.count";
    public static final String LLM_SERVICE_REQ_COUNT_DESC = "The total count of watsonx calls by interval";
    public static final String LLM_SERVICE_REQ_COUNT_UNIT = "{count}";
}