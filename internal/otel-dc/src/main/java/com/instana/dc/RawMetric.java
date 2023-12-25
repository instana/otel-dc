/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class RawMetric {
    private static final Logger logger = Logger.getLogger(RawMetric.class.getName());

    private final String name;
    private final InstrumentType instrumentType;
    private final String description;
    private final String unit;
    private final boolean isInteger;
    private final String attributeKey;

    private CalculationMode calculationMode;
    private static final double DEFAULT_RATE_UNIT = 1000;
    private double rateUnit = DEFAULT_RATE_UNIT;
    private final Map<String, DataPoint> dps = new ConcurrentHashMap<>();
    private static final long DEFAULT_OUTDATED_TIME = 125000L;
    private long outdatedTime = DEFAULT_OUTDATED_TIME;
    private static final String DEFAULT = "default";
    private boolean clearDps = false;

    public RawMetric(InstrumentType instrumentType, String name, String description, String unit, boolean isInteger, String attributeKey) {
        this.instrumentType = instrumentType;
        this.name = name;
        this.description = description;
        this.unit = unit;
        this.isInteger = isInteger;
        this.attributeKey = attributeKey;

        this.calculationMode = CalculationMode.DIRECT;
    }

    public String getName() {
        return name;
    }

    public InstrumentType getInstrumentType() {
        return instrumentType;
    }

    public String getDescription() {
        return description;
    }

    public String getUnit() {
        return unit;
    }

    public boolean isInteger() {
        return isInteger;
    }

    public String getAttributeKey() {
        return attributeKey;
    }

    public CalculationMode getCalculationMode() {
        return calculationMode;
    }

    public RawMetric setCalculationMode(CalculationMode calculationMode) {
        this.calculationMode = calculationMode;
        return this;
    }

    public double getRateUnit() {
        return rateUnit;
    }

    public RawMetric setRateUnit(double rateUnit) {
        this.rateUnit = rateUnit;
        return this;
    }

    public void purgeOutdatedDps() {
        long tm = System.currentTimeMillis();
        dps.entrySet().removeIf(entry -> tm - entry.getValue().getCurrentTime() > outdatedTime);
    }

    public Map<String, DataPoint> getDataPoints() {
        return dps;
    }

    public long getOutdatedTime() {
        return outdatedTime;
    }

    public RawMetric setOutdatedTime(long outdatedTime) {
        this.outdatedTime = outdatedTime;
        return this;
    }

    public RawMetric setValue(Number value) {
        getDataPoint(null).setValue(value);
        return this;
    }

    public RawMetric setValue(Number value, Map<String, Object> attributes) {
        getDataPoint(null).setValue(value, attributes);
        return this;
    }

    public RawMetric setValue(List<SimpleQueryResult> results) {
        if (results != null) {
            for (SimpleQueryResult result : results) {
                getDataPoint(result.getKey()).setValue(result);
            }
        }
        return this;
    }

    public boolean isClearDps() {
            return clearDps;
    }

    public RawMetric setClearDps(boolean clearDps) {
        this.clearDps = clearDps;
        return this;
    }

    public DataPoint getDataPoint(String key) {
        if (key == null) {
            key = DEFAULT;
        }
        return dps.computeIfAbsent(key, k -> new DataPoint(this, k));
    }

    public static class DataPoint {
        private final RawMetric rawMetric;
        private final String key;

        private Number currentValue, previousValue;
        private long currentTime, previousTime;
        private final Map<String, Object> attributes = new ConcurrentHashMap<>();

        public DataPoint(RawMetric rawMetric, String key) {
            this.rawMetric = rawMetric;
            this.key = key;

            this.currentValue = null;
            this.previousValue = null;
            this.currentTime = 0;
            this.previousTime = 0;
        }

        public RawMetric getRawMetric() {
            return rawMetric;
        }

        public String getKey() {
            return key;
        }

        public Number getValue() {
            if (rawMetric.getCalculationMode() == CalculationMode.DIRECT) {
                return currentValue;
            }
            if (currentValue == null || previousValue == null || currentTime <= previousTime) {
                return null;
            }

            long longDelta = 0;
            double doubleDelta = 0;
            if (rawMetric.isInteger()) {
                longDelta = currentValue.longValue() - previousValue.longValue();
            } else {
                doubleDelta = currentValue.doubleValue() - previousValue.doubleValue();
            }

            long timeDelta = currentTime - previousTime;
            if (rawMetric.isInteger()) {
                return rawMetric.getRateUnit() * longDelta / timeDelta;
            } else {
                return rawMetric.getRateUnit() * doubleDelta / timeDelta;
            }
        }

        public Double getDoubleValue() {
            Number number = getValue();
            if (number == null) {
                return null;
            }
            return number.doubleValue();
        }

        public Long getLongValue() {
            Number number = getValue();
            if (number == null) {
                return null;
            }
            return number.longValue();
        }

        public void setValue(Number value) {
            if (value == null) {
                return;
            }
            this.previousValue = this.currentValue;
            this.previousTime = this.currentTime;
            this.currentValue = value;
            this.currentTime = System.currentTimeMillis();
            logger.info("New metric value: " + rawMetric.getName() + '/' + key + '=' + value);
        }

        public void setValue(Number value, Map<String, Object> attributes) {
            if (value == null) {
                return;
            }
            setValue(value);
            this.attributes.clear();
            if (attributes != null) {
                this.attributes.putAll(attributes);
            }
        }

        public void setValue(SimpleQueryResult result) {
            if (result != null) {
                setValue(result.getValue(), result.getAttributes());
            }
        }

        public Map<String, Object> getAttributes() {
            return attributes;
        }

        public long getCurrentTime() {
            return currentTime;
        }
    }

}
