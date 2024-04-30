/*
 * (c) Copyright IBM Corp. 2023
 * (c) Copyright Instana Inc.
 */
package com.instana.agent.sensorsdk.semconv;

import io.opentelemetry.api.common.AttributeKey;

@SuppressWarnings("unused")
public class SemanticAttributes {
  private SemanticAttributes() {
  }

  /**
   * The URL of the OpenTelemetry schema for these keys and values.
   */
  public static final String SCHEMA_URL = "https://instana.com/otel/semconv/1.0.0";

  public static final AttributeKey<Long> DB_STATUS = AttributeKey.longKey("db.status");
  public static final AttributeKey<Long> DB_INSTANCE_COUNT = AttributeKey.longKey("db.instance.count");
  public static final AttributeKey<Long> DB_INSTANCE_ACTIVE_COUNT = AttributeKey.longKey("db.instance.active.count");

  public static final AttributeKey<Long> DB_SESSION_COUNT = AttributeKey.longKey("db.session.count");
  public static final AttributeKey<Long> DB_SESSION_ACTIVE_COUNT = AttributeKey.longKey("db.session.active.count");
  public static final AttributeKey<Long> DB_TRANSACTION_COUNT = AttributeKey.longKey("db.transaction.count");
  public static final AttributeKey<Double> DB_TRANSACTION_RATE = AttributeKey.doubleKey("db.transaction.rate");
  public static final AttributeKey<Double> DB_TRANSACTION_LATENCY = AttributeKey.doubleKey("db.transaction.latency");
  public static final AttributeKey<Long> DB_SQL_COUNT = AttributeKey.longKey("db.sql.count");
  public static final AttributeKey<Double> DB_SQL_RATE = AttributeKey.doubleKey("db.sql.rate");
  public static final AttributeKey<Double> DB_SQL_LATENCY = AttributeKey.doubleKey("db.sql.latency");
  public static final AttributeKey<Double> DB_IO_READ_RATE = AttributeKey.doubleKey("db.io.read.rate");
  public static final AttributeKey<Double> DB_IO_WRITE_RATE = AttributeKey.doubleKey("db.io.write.rate");
  public static final AttributeKey<Long> DB_TASK_WAIT_COUNT = AttributeKey.longKey("db.task.wait_count");
  public static final AttributeKey<Double> DB_TASK_AVG_WAIT_TIME = AttributeKey.doubleKey("db.task.avg_wait_time");

  public static final AttributeKey<Double> DB_CACHE_HIT = AttributeKey.doubleKey("db.cache.hit");
  public static final AttributeKey<Double> DB_SQL_ELAPSED_TIME = AttributeKey.doubleKey("db.sql.elapsed_time");
  public static final AttributeKey<Long> DB_LOCK_COUNT = AttributeKey.longKey("db.lock.count");
  public static final AttributeKey<Double> DB_LOCK_TIME = AttributeKey.doubleKey("db.lock.time");

  public static final AttributeKey<Double> DB_CPU_UTILIZATION = AttributeKey.doubleKey("db.cpu.utilization");
  public static final AttributeKey<Double> DB_MEM_UTILIZATION = AttributeKey.doubleKey("db.mem.utilization");
  public static final AttributeKey<Long> DB_DISK_USAGE = AttributeKey.longKey("db.disk.usage");
  public static final AttributeKey<Double> DB_DISK_UTILIZATION = AttributeKey.doubleKey("db.disk.utilization");
  public static final AttributeKey<Long> DB_TABLESPACE_SIZE = AttributeKey.longKey("db.tablespace.size");
  public static final AttributeKey<Long> DB_TABLESPACE_USED = AttributeKey.longKey("db.tablespace.used");
  public static final AttributeKey<Double> DB_TABLESPACE_UTILIZATION = AttributeKey.doubleKey("db.tablespace.utilization");
  public static final AttributeKey<Long> DB_TABLESPACE_MAX = AttributeKey.longKey("db.tablespace.max");

  public static final AttributeKey<Long> DB_DISK_WRITE_COUNT = AttributeKey.longKey("db.disk.write.count");
  public static final AttributeKey<Long> DB_DISK_READ_COUNT = AttributeKey.longKey("db.disk.read.count");

  public static final AttributeKey<Double> DB_BACKUP_CYCLE = AttributeKey.doubleKey("db.backup.cycle");

  public static final AttributeKey<Long> DB_DATABASE_LOG_ENABLED = AttributeKey.longKey("db.database.log.enabled");
  public static final AttributeKey<Long> DB_DATABASE_BUFF_LOG_ENABLED = AttributeKey.longKey("db.database.buff.log.enabled");
  public static final AttributeKey<Long> DB_DATABASE_ANSI_COMPLAINT = AttributeKey.longKey("db.database.ansi.compliant");
  public static final AttributeKey<Long> DB_DATABASE_NLS_ENABLED = AttributeKey.longKey("db.database.nls.enabled");
  public static final AttributeKey<Long> DB_DATABASE_CASE_INCENSITIVE = AttributeKey.longKey("db.database.case.insensitive");

  public static final AttributeKey<String> STATE = AttributeKey.stringKey("state");
  public static final AttributeKey<String> TYPE = AttributeKey.stringKey("type");
  public static final AttributeKey<String> PATH = AttributeKey.stringKey("path");
  public static final AttributeKey<String> SQL_TEXT = AttributeKey.stringKey("sql_text");
  public static final AttributeKey<String> SQL_ID = AttributeKey.stringKey("sql_id");
  public static final AttributeKey<String> LOCK_ID = AttributeKey.stringKey("lock_id");
  public static final AttributeKey<String> BLOCKING_SESS_ID = AttributeKey.stringKey("blocking_sess_id");
  public static final AttributeKey<String> BLOCKER_SESS_ID = AttributeKey.stringKey("blocker_sess_id");
  public static final AttributeKey<String> LOCKED_OBJ_NAME = AttributeKey.stringKey("locked_obj_name");
  public static final AttributeKey<String> TABLESPACE_NAME = AttributeKey.stringKey("tablespace_name");
  public static final AttributeKey<String> DATABASE_NAME = AttributeKey.stringKey("database_name");

  public static final AttributeKey<Long> DATABASE_LOCK_TABLE_OVERFLOW = AttributeKey.longKey("db.overflow.lock.count");
}
