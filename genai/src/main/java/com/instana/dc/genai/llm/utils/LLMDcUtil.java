package com.instana.dc.genai.llm.utils;

import java.util.Currency;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LLMDcUtil {

    private LLMDcUtil(){ }

    private static final Logger logger = Logger.getLogger(LLMDcUtil.class.getName());

    public static final String LLM_PRICES_PROPERTIES = "config/prices.properties";
    public static final String CURRENCY = "currency";
    public static final String USD = "USD";

    public static final String LLM_USER_NAME = "llm.user";
    public static final String LLM_USER_DESC = "The user of the LLM";
    public static final String LLM_USER_UNIT = "{user}";

    public static final String LLM_MODEL_ID_NAME = "llm.model.id";
    public static final String LLM_MODEL_ID_DESC = "The model ID of the LLM";
    public static final String LLM_MODEL_ID_UNIT = "{model}";

    public static final String LLM_STATUS_NAME = "llm.status";
    public static final String LLM_STATUS_DESC = "The status of the LLM dc";
    public static final String LLM_STATUS_UNIT = "{status}";

    public static final String LLM_DURATION_NAME = "llm.response.duration";
    public static final String LLM_DURATION_DESC = "The average duration of LLM calls by interval";
    public static final String LLM_DURATION_UNIT = "ms";

    public static final String LLM_DURATION_MAX_NAME = "llm.response.duration.max";
    public static final String LLM_DURATION_MAX_DESC = "The maximum duration of LLM calls by interval";
    public static final String LLM_DURATION_MAX_UNIT = "ms";

    public static final String LLM_COST_NAME = "llm.usage.cost";
    public static final String LLM_COST_DESC = "The total cost of LLM calls by interval";
    public static final String LLM_COST_UNIT = "{cost}";

    public static final String LLM_INPUT_COST_NAME = "llm.usage.input_cost";
    public static final String LLM_INPUT_COST_DESC = "The input cost of LLM calls by interval";
    public static final String LLM_INPUT_COST_UNIT = "{cost}";

    public static final String LLM_OUTPUT_COST_NAME = "llm.usage.output_cost";
    public static final String LLM_OUTPUT_COST_DESC = "The output cost of LLM calls by interval";
    public static final String LLM_OUTPUT_COST_UNIT = "{cost}";

    public static final String LLM_TOKEN_NAME = "llm.usage.total_tokens";
    public static final String LLM_TOKEN_DESC = "The total tokens of LLM calls by interval";
    public static final String LLM_TOKEN_UNIT = "{token}";

    public static final String LLM_INPUT_TOKEN_NAME = "llm.usage.input_tokens";
    public static final String LLM_INPUT_TOKEN_DESC = "The input tokens of LLM calls by interval";
    public static final String LLM_INPUT_TOKEN_UNIT = "{token}";

    public static final String LLM_OUTPUT_TOKEN_NAME = "llm.usage.output_tokens";
    public static final String LLM_OUTPUT_TOKEN_DESC = "The output tokens of LLM calls by interval";
    public static final String LLM_OUTPUT_TOKEN_UNIT = "{token}";

    public static final String LLM_REQ_COUNT_NAME = "llm.request.count";
    public static final String LLM_REQ_COUNT_DESC = "The total count of LLM calls by interval";
    public static final String LLM_REQ_COUNT_UNIT = "{count}";

    public static final String LLM_SERVICE_COST_NAME = "llm.service.usage.cost";
    public static final String LLM_SERVICE_COST_DESC = "The total cost of LLM calls by interval";
    public static final String LLM_SERVICE_COST_UNIT = "{cost}";

    public static final String LLM_SERVICE_INPUT_COST_NAME = "llm.service.usage.input_cost";
    public static final String LLM_SERVICE_INPUT_COST_DESC = "The input cost of LLM calls by interval";
    public static final String LLM_SERVICE_INPUT_COST_UNIT = "{cost}";

    public static final String LLM_SERVICE_OUTPUT_COST_NAME = "llm.service.usage.output_cost";
    public static final String LLM_SERVICE_OUTPUT_COST_DESC = "The output cost of LLM calls by interval";
    public static final String LLM_SERVICE_OUTPUT_COST_UNIT = "{cost}";

    public static final String LLM_SERVICE_TOKEN_NAME = "llm.service.usage.total_tokens";
    public static final String LLM_SERVICE_TOKEN_DESC = "The total tokens of LLM calls by interval";
    public static final String LLM_SERVICE_TOKEN_UNIT = "{token}";

    public static final String LLM_SERVICE_INPUT_TOKEN_NAME = "llm.service.usage.input_tokens";
    public static final String LLM_SERVICE_INPUT_TOKEN_DESC = "The input tokens of LLM calls by interval";
    public static final String LLM_SERVICE_INPUT_TOKEN_UNIT = "{token}";

    public static final String LLM_SERVICE_OUTPUT_TOKEN_NAME = "llm.service.usage.output_tokens";
    public static final String LLM_SERVICE_OUTPUT_TOKEN_DESC = "The output tokens of LLM calls by interval";
    public static final String LLM_SERVICE_OUTPUT_TOKEN_UNIT = "{token}";

    public static final String LLM_SERVICE_REQ_COUNT_NAME = "llm.service.request.count";
    public static final String LLM_SERVICE_REQ_COUNT_DESC = "The total count of LLM calls by interval";
    public static final String LLM_SERVICE_REQ_COUNT_UNIT = "{count}";

    public static String currencySymbolOf(String currencyCode) {
        try {
            return Currency.getInstance(currencyCode).getSymbol();
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Invalid currency code: {0}, using USD as default", currencyCode);
            return Currency.getInstance(USD).getSymbol();
        }
    }
}
