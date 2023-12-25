/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.agent.sensorsdk.semconv;

import io.opentelemetry.api.common.AttributeKey;

@SuppressWarnings("unused")
public class ResourceAttributes {
  private ResourceAttributes() {
  }

  /**
   * The URL of the OpenTelemetry schema for these keys and values.
   */
  public static final String SCHEMA_URL = "https://instana.com/otel/semconv/1.0.0";

  public static final AttributeKey<String> SERVER_ADDRESS = AttributeKey.stringKey("server.address");
  public static final AttributeKey<Long> SERVER_PORT = AttributeKey.longKey("server.port");
  public static final AttributeKey<String> DB_VERSION = AttributeKey.stringKey("db.version");
  public static final AttributeKey<String> DB_INSTANCE_NAME = AttributeKey.stringKey("db.instance.name");
  public static final AttributeKey<Long> DB_INSTANCE_NUMBER = AttributeKey.longKey("db.instance.number");
  public static final AttributeKey<String> DB_INSTANCE_HOST = AttributeKey.stringKey("db.instance.host");
  public static final AttributeKey<String> DB_ENTITY_PARENT_ID = AttributeKey.stringKey("db.entity.parent.id");
  public static final AttributeKey<String> DB_ENTITY_TYPE = AttributeKey.stringKey("db.entity.type");
  public static final AttributeKey<String> INSTANA_PLUGIN = AttributeKey.stringKey("INSTANA_PLUGIN");
  public static final String DATABASE = "database";

  public static final class DbEntityTypeValues {
    private DbEntityTypeValues() {
    }

    public static final String DATABASE = "DATABASE";
    public static final String INSTANCE = "INSTANCE";
  }
}
