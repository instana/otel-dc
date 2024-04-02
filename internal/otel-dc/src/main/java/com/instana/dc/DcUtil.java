/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.dc;

import com.google.common.util.concurrent.AtomicDouble;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.sdk.resources.Resource;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DcUtil {
    private static final Logger logger = Logger.getLogger(DcUtil.class.getName());

    /* Configurations for the Data Collector:
     */
    public final static String POLLING_INTERVAL = "poll.interval";
    public static final int DEFAULT_POLL_INTERVAL = 30;  //unit is second, read database
    public final static String CALLBACK_INTERVAL = "callback.interval";
    public static final int DEFAULT_CALLBACK_INTERVAL = 60; //unit is second, send to backend
    public final static String OTEL_BACKEND_URL = "otel.backend.url";
    public final static String OTEL_BACKEND_USING_HTTP = "otel.backend.using.http";
    public final static String DEFAULT_OTEL_BACKEND_URL = "http://127.0.0.1:4317";
    public final static String OTEL_SERVICE_NAME = "otel.service.name";
    public final static String DEFAULT_OTEL_SERVICE_NAME = "odcd.default.service";
    public final static String OTEL_SERVICE_INSTANCE_ID = "otel.service.instance.id";
    public final static String DEFAULT_OTEL_SERVICE_INSTANCE_ID = "odcd.default.id";

    //Standard environment variables;
    public static final String OTEL_RESOURCE_ATTRIBUTES = "OTEL_RESOURCE_ATTRIBUTES";
    public static final String OTEL_EXPORTER_OTLP_HEADERS = "OTEL_EXPORTER_OTLP_HEADERS";
    public static final String OTEL_EXPORTER_OTLP_CERTIFICATE = "OTEL_EXPORTER_OTLP_CERTIFICATE";

    //Configuration files;
    public static final String LOGGING_PROP = "config/logging.properties";
    public static final String CONFIG_YAML = "config/config.yaml";
    public static final String CONFIG_ENV = "DC_CONFIG";
    public static final String INSTANA_PLUGIN = "INSTANA_PLUGIN";


    /* Data Collector Utilities:
     */
    public static Resource mergeResourceAttributesFromEnv(Resource resource) {
        String resAttrs = System.getenv(OTEL_RESOURCE_ATTRIBUTES);
        if (resAttrs != null) {
            for (String resAttr : resAttrs.split(",")) {
                String[] kv = resAttr.split("=");
                if (kv.length != 2)
                    continue;
                String key = kv[0].trim();
                String value = kv[1].trim();
                resource = resource.merge(Resource.create(Attributes.of(AttributeKey.stringKey(key), value)));
            }
        }
        return resource;
    }

    public static Map<String, String> getHeadersFromEnv() {
        String resAttrs = System.getenv(OTEL_EXPORTER_OTLP_HEADERS);
        if (resAttrs == null) {
            return null;
        }
        Map<String, String> map = new HashMap<>();
        for (String resAttr : resAttrs.split(",")) {
            String[] kv = resAttr.split("=");
            if (kv.length != 2)
                continue;
            String key = kv[0].trim();
            String value = kv[1].trim();
            map.put(key, value);
        }
        return map;
    }

    public static byte[] getCert() {
        String certFile = System.getenv(OTEL_EXPORTER_OTLP_CERTIFICATE);
        if (certFile != null) {
            try {
                return Files.readAllBytes(new File(certFile).toPath());
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Certification file is invalid: {0}", certFile);
            }
        }
        return null;
    }

    public static void _registerMeterWithLongMetric(Meter meter, InstrumentType instrumentType, String metricName, String unit, String desc, AtomicLong data) {
        switch (instrumentType) {
            case GAUGE:
                meter.gaugeBuilder(metricName).setUnit(unit).setDescription(desc).buildWithCallback(measurement -> measurement.record(data.get()));
                break;
            case COUNTER:
                meter.counterBuilder(metricName).setUnit(unit).setDescription(desc).buildWithCallback(measurement -> measurement.record(data.get()));
                break;
            case UPDOWN_COUNTER:
                meter.upDownCounterBuilder(metricName).setUnit(unit).setDescription(desc).buildWithCallback(measurement -> measurement.record(data.get()));
                break;
            default:
                logger.log(Level.WARNING, "Currently only following instrument types are supported, Gauge, Counter, UpDownCounter, while your type is {0}", instrumentType);
        }
    }

    public static void _registerMeterWithDoubleMetric(Meter meter, InstrumentType instrumentType, String metricName, String unit, String desc, AtomicDouble data) {
        switch (instrumentType) {
            case GAUGE:
                meter.gaugeBuilder(metricName).setUnit(unit).setDescription(desc).buildWithCallback(measurement -> measurement.record(data.get()));
                break;
            case COUNTER:
                meter.counterBuilder(metricName).setUnit(unit).setDescription(desc).buildWithCallback(measurement -> measurement.record((long) data.get()));
                break;
            case UPDOWN_COUNTER:
                meter.upDownCounterBuilder(metricName).setUnit(unit).setDescription(desc).buildWithCallback(measurement -> measurement.record((long) data.get()));
                break;
            default:
                logger.log(Level.WARNING, "Currently only following instrument types are supported, Gauge, Counter, UpDownCounter, while your type is {0}", instrumentType);
        }
    }

    public static Attributes convertMapToAttributes(Map<String, Object> map) {
        AttributesBuilder builder = Attributes.builder();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Long) {
                builder.put(key, (Long) value);
            } else if (value instanceof Double) {
                builder.put(key, (Double) value);
            } else if (value instanceof Boolean) {
                builder.put(key, (Boolean) value);
            } else {
                builder.put(key, value.toString());
            }
        }
        return builder.build();
    }

    public static void registerMetric(Map<String, Meter> meters, RawMetric rawMetric) {
        Consumer<ObservableLongMeasurement> recordLongMetric = measurement -> {
            rawMetric.purgeOutdatedDps();
            boolean clearDps = rawMetric.isClearDps();
            Iterator<Map.Entry<String, RawMetric.DataPoint>> iterator = rawMetric.getDataPoints().entrySet().iterator();
            while (iterator.hasNext()) {
                RawMetric.DataPoint dp = iterator.next().getValue();
                Long value = dp.getLongValue();
                if (value == null)
                    continue;
                measurement.record(value, convertMapToAttributes(dp.getAttributes()));
                if (clearDps) {
                    iterator.remove();
                }
            }
        };
        Consumer<ObservableDoubleMeasurement> recordDoubleMetric = measurement -> {
            rawMetric.purgeOutdatedDps();
            boolean clearDps = rawMetric.isClearDps();
            Iterator<Map.Entry<String, RawMetric.DataPoint>> iterator = rawMetric.getDataPoints().entrySet().iterator();
            while (iterator.hasNext()) {
                RawMetric.DataPoint dp = iterator.next().getValue();
                Double value = dp.getDoubleValue();
                if (value == null)
                    continue;
                measurement.record(value, convertMapToAttributes(dp.getAttributes()));
                if (clearDps) {
                    iterator.remove();
                }
            }
        };

        Meter meter = meters.get(rawMetric.getMeterName());
        switch (rawMetric.getInstrumentType()) {
            case GAUGE:
                if (rawMetric.isInteger())
                    meter.gaugeBuilder(rawMetric.getName()).ofLongs().setUnit(rawMetric.getUnit()).setDescription(rawMetric.getDescription())
                            .buildWithCallback(recordLongMetric);
                else
                    meter.gaugeBuilder(rawMetric.getName()).setUnit(rawMetric.getUnit()).setDescription(rawMetric.getDescription())
                            .buildWithCallback(recordDoubleMetric);
                break;
            case COUNTER:
                if (rawMetric.isInteger())
                    meter.counterBuilder(rawMetric.getName()).setUnit(rawMetric.getUnit()).setDescription(rawMetric.getDescription())
                            .buildWithCallback(recordLongMetric);
                else
                    meter.counterBuilder(rawMetric.getName()).ofDoubles().setUnit(rawMetric.getUnit()).setDescription(rawMetric.getDescription())
                            .buildWithCallback(recordDoubleMetric);
                break;
            case UPDOWN_COUNTER:
                if (rawMetric.isInteger())
                    meter.upDownCounterBuilder(rawMetric.getName()).setUnit(rawMetric.getUnit()).setDescription(rawMetric.getDescription())
                            .buildWithCallback(recordLongMetric);
                else
                    meter.upDownCounterBuilder(rawMetric.getName()).ofDoubles().setUnit(rawMetric.getUnit()).setDescription(rawMetric.getDescription())
                            .buildWithCallback(recordDoubleMetric);
                break;
            default:
                logger.log(Level.WARNING, "Currently only following instrument types are supported, Gauge, Counter, UpDownCounter, while your type is {0}", rawMetric.getInstrumentType());
        }
    }

    public static long getPid() {
        // While this is not strictly defined, almost all commonly used JVMs format this as
        // pid@hostname.
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        int atIndex = runtimeName.indexOf('@');
        if (atIndex >= 0) {
            String pidString = runtimeName.substring(0, atIndex);
            try {
                return Long.parseLong(pidString);
            } catch (NumberFormatException ignored) {
                // Ignore parse failure.
            }
        }
        return -1;
    }

    public static String base64Decode(String encodedStr) {
        return new String(Base64.getDecoder().decode(encodedStr));
    }
}
