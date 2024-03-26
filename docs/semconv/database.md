# Database Semantic Conventions

# Introduction

The Semantic Conventions define a common set of (semantic) attributes which provide meaning to data when collecting, producing and consuming it. The benefit to using Semantic Conventions is in following a common naming scheme that can be standardized across a codebase, libraries, and platforms. This allows easier correlation and consumption of data. For more information, see [OpenTelemetry Semantic Conventions](https://github.com/open-telemetry/semantic-conventions/blob/main/docs/README.md).

OpenTelemetry has defined Semantic Conventions for a couple of areas, database is one of them.  See more information at [Semantic Conventions for Database Calls and Systems](https://github.com/open-telemetry/semantic-conventions/blob/main/docs/database/README.md).

Currently, OpenTelemetry official released [DB Metrics](https://github.com/open-telemetry/semantic-conventions/blob/main/docs/database/database-metrics.md) page only defines metrics specific to SQL and NoSQL clients (connection pools related), there is no semantic conventions defined for database server activities, such as number of sessions, etc. 
We are working with the community to push forward a more comprehensive Semantic Convention with a common description for all databases starting from RDBMS.  

This page tries to describe a semantic convention for the attributes and metrics of generic database activities, the definition will be used to develop generic database sensor. We are considering to propose this to the community.

# Resource attributes

## Database

Description: A database.

| Attribute Key                                                                                                                               | Type   | Description                                                                                                                                                      | Example                      | Requirement Level                          |
|---------------------------------------------------------------------------------------------------------------------------------------------|--------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------|--------------------------------------------|
| [`server.address`](https://github.com/open-telemetry/semantic-conventions/blob/main/docs/general/attributes.md)                             | string | Name of the database host.                                                                                                                                       | db.testdb.com                | Recommended                                |
| [`server.port`](https://github.com/open-telemetry/semantic-conventions/blob/main/docs/general/attributes.md)                                | int    | database listen port                                                                                                                                             | 50000,5236                   | Recommended                                |
| [`db.name`](https://github.com/open-telemetry/semantic-conventions/blob/main/docs/database/database-spans.md#call-level-attributes)         | string | This attribute is used to report the name of the database being accessed. For commands that switch the database, this should be set to the target database       | BLUDB, DAMENG                | Conditionally Required: If applicable.     |
| [`db.system`](https://github.com/open-telemetry/semantic-conventions/blob/main/docs/database/database-spans.md#connection-level-attributes) | string | An identifier for the database management system (DBMS) product being used.                                                                                      | db2, damengdb                | Required                                   |
| `db.version`                                                                                                                                | string | The version of the database                                                                                                                                      | V11.5, V8                    | Required                                   |
| `service.name`                                                                                                                              | string | This attribute is used to describe the entity name.                                                                                                              | damengdb@DAMENG              | Required                                   |
| `service.instance.id`                                                                                                                       | string | This attribute is used to describe the entity ID of the current object and consists of server.address, server.port, and db.name.                                 | db.testdb.com:5236@DAMENG    | Conditionally Required: If applicable.     |
| `db.entity.parent.id`                                                                                                                       | string | This attribute is used to describe the parent entity ID of the current object and consists of server.address, server.port and db.name or instance.name together. | db.testdb.com:5236@db2inst1  | Conditionally Required: If applicable.     |
| `db.entity.type`                                                                                                                            | string | This attribute is used to describe the type of the current object.                                                                                               | DATABASE, INSTANCE           | Conditionally Required: If applicable.     |
| `db.database.name`                                                                                                                          | string | Name of the Database.                                                                                                                                            | prod_db, instana             | Conditionally Required: If applicable.     |
| `db.database.owner`                                                                                                                         | string | User name of the database creator.                                                                                                                               | informix, admin              | Conditionally Required: If applicable.     |
| `db.database.created_on`                                                                                                                    | Date   | Creation Date of the database.                                                                                                                                   | 2023-11-16                   | Conditionally Required: If applicable.     |


# Common metric instruments

All metrics in `db.database` instruments should be attached to a Database resource and therefore inherit its attributes, like `db.database.system`.

All metrics in `db.instance` instruments should be attached to a [Instance resource](../database/instance-metrics.md) and therefore inherit its attributes, like `db.instance.name`.

## Availability

| Name                       | Description                                          | Units      | Instrument Type | Value Type | Requirement Level | Attribute Key(s) | Attribute Values |
|----------------------------|------------------------------------------------------|------------|-----------------|------------|-------------------|------------------|------------------|
| `db.status`                | The status of the database. 1 (Active), 0 (Inactive) | {status}   | Gauge           | Int        | Required          |                  |                  |
| `db.instance.count`        | The total number of instances of database.           | {instance} | UpDownCounter   | Int        | Recommended       |                  |                  |
| `db.instance.active.count` | The total number of active instances of database.    | {instance} | UpDownCounter   | Int        | Recommended       |                  |                  |


## Throughput

| Name                       | Description                                 | Unit          | Instrument Type | Value Type | Requirement Level | Attribute Key(s) | Attribute Values |
|----------------------------|---------------------------------------------|---------------|-----------------|------------|-------------------|------------------|------------------|
| `db.session.count`         | The number of database sessions.            | {session}     | UpDownCounter   | Int        | Recommended       |                  |                  |
| `db.session.active.count`  | The number of active database sessions.     | {session}     | UpDownCounter   | Int        | Recommended       |                  |                  |
| `db.transaction.count`     | The number of completed transactions.       | {transaction} | UpDownCounter   | Int        | Recommended       |                  |                  |
| `db.transaction.rate`      | The number of transactions per second.      | {transaction} | Gauge           | Double     | Recommended       |                  |                  |
| `db.transaction.latency`   | The average transaction latency.            | s             | Gauge           | Double     | Recommended       |                  |                  |
| `db.sql.count`             | The number of SQLs                          | {sql}         | UpDownCounter   | Int64      | Recommended       |                  |                  |
| `db.sql.rate`              | The number of SQL per second.               | {sql}         | Gauge           | Double     | Recommended       |                  |                  |
| `db.sql.latency`           | The average SQL latency.                    | s             | Gauge           | Double     | Recommended       |                  |                  |
| `db.io.read.rate`          | The physical read per second.               | By            | Gauge           | Double     | Recommended       |                  |                  |
| `db.io.write.rate`         | The physical write per second.              | By            | Gauge           | Double     | Recommended       |                  |                  |
| `db.task.wait_count`       | Number of waiting tasks.                    | {task}        | UpDownCounter   | Int        | Optional          |                  |                  |
| `db.task.avg_wait_time`    | Average task wait time.                     | s             | Gauge           | Double     | Optional          |                  |                  |

## Performance

| Name                  | Description                             | Unit   | Instrument Type | Value Type | Requirement Level | Attribute Key(s)                                                 | Attribute values |
|-----------------------|-----------------------------------------|--------|-----------------|------------|-------------------|------------------------------------------------------------------|------------------|
| `db.cache.hit`        | The cache hit ratio/percentage          | 1      | Gauge           | Double     | Recommended       | `type`(Recommended) The type of cache.                           | (cache type)     |
| `db.sql.elapsed_time` | The elapsed time in second of the query | s      | UpDownCounter   | Double     | Recommended       | `sql_id`(Required) The sql statement id.                         | (sql id)         |
|                       |                                         |        |                 |            |                   | `sql_text`(Recommended) The text of sql statement.               | (sql text)       |
| `db.lock.count`       | The number of database locks.           | {lock} | UpDownCounter   | Int        | Recommended       | `type`(Recommended) The type of lock.                            | (lock type)      |
| `db.lock.time`        | The lock elapsed time                   | s      | UpDownCounter   | Double     | Recommended       | `lock_id`(Required) The lock ID.                                 | (lock id)        |
|                       |                                         |        |                 |            |                   | `blocking_sess_id`(Recommended) The blocking session identifier. | (session id)     |
|                       |                                         |        |                 |            |                   | `blocker_sess_id`(Recommended)  The blocker session identifier.  | (session id)     |
|                       |                                         |        |                 |            |                   | `locked_obj_name`(Recommended) The locked object name.           | (object name)    |


## Resource Usage

| Name                        | Description                                 | Unit | Instrument Type  | Value Type | Requirement Level | Attribute Key(s)                        | Attribute Values  |
|-----------------------------|---------------------------------------------|------|------------------|------------|-------------------|-----------------------------------------|-------------------|
| `db.disk.usage`             | The size (in bytes) of the used disk space. | By   | UpDownCounter    | Int64      | Recommended       | `path`(Recommended) The disk path.      | (path)            |
| `db.disk.utilization`       | The percentage of used disk space.          | 1    | Gauge            | Double     | Recommended       | `path`(Recommended) The disk path.      | (path)            |
| `db.cpu.utilization`        | The percentage of used CPU.                 | 1    | Gauge            | Double     | Recommended       |                                         |                   |
| `db.mem.utilization`        | The percentage of used memory               | 1    | Gauge            | Double     | Recommended       |                                         |                   |
| `db.tablespace.size`        | The size (in bytes) of the tablespace.      | By   | UpDownCounter    | Int64      | Optional          | `tablespace_name`(Required) identifier. | {tablespace_name} |
| `db.tablespace.used`        | The used size (in bytes) of the tablespace. | By   | UpDownCounter    | Int64      | Optional          | `tablespace_name`(Required) identifier. | {tablespace_name} |
| `db.tablespace.utilization` | The used percentage of the tablespace.      | 1    | Gauge            | Double     | Optional          | `tablespace_name`(Required) identifier. | {tablespace_name} |
| `db.tablespace.max`         | The max size (in bytes) of the tablespace.  | By   | UpDownCounter    | Int64      | Optional          | `tablespace_name`(Required) identifier. | {tablespace_name} |
| `db.disk.writes`            | Actual number of writes to disk             | By   | UpDownCounter    | Int64      | Optional          |                                         |                   |
| `db.disk.reads`             | Actual number of reads to disk              | By   | UpDownCounter    | Int64      | Optional          |                                         |                   |

## Maintenance

| Name                   | Description                                                    | Unit | Instrument Type | Value Type | Requirement Level | Attribute Key(s)    | Attribute Values |
|------------------------|----------------------------------------------------------------|------|-----------------|------------|-------------------|---------------------|------------------|
| `db.backup.cycle`      | Backup cycle.                                                  | s    | Gauge           | Int64      | Recommended       |                     |                  |


## Settings
| Name                          | Description                                                                                   | Unit | Instrument Type | Value Type | Requirement Level | Attribute Key(s)                       | Attribute Values    |
|-------------------------------|-----------------------------------------------------------------------------------------------|------|-----------------|------------|-------------------|----------------------------------------|---------------------|
| db.database.log.enabled       | Database logging is enabled or not. 1 (Active), 0 (Inactive).                                 | By   | Gauge           | Int        | Optional          | db.database.name(Required) identifier. | {db.database.name}  |
| db.database.buff.log.enabled  | Database Buffered logging is enabled or not. 1 (Active), 0 (Inactive).                        | By   | Gauge           | Int        | Optional          | db.database.name(Required) identifier. | {db.database.name}  |
| db.database.ansi.compliant    | Database is ANSI/ISO-compliant or not. 1 (Active), 0 (Inactive).                              | By   | Gauge           | Int        | Optional          | db.database.name(Required) identifier. | {db.database.name}  |
| db.database.nls.enabled       | Database is GLS-enabled or not. 1 (Active), 0 (Inactive).                                     | By   | Gauge           | Int        | Optional          | db.database.name(Required) identifier. | {db.database.name}  | 
| db.database.case.insensitive  | Database is case-insensitive for NCHAR and NVARCHAR columns or not. 1 (Active), 0 (Inactive). | By   | Gauge           | Int        | Optional          | db.database.name(Required) identifier. | {db.database.name}  | 


# Custom metrics

Please follow the guideline if custom metrics sent, follow this specification to name the custom metrics.
1. [Instrument Naming](https://github.com/open-telemetry/opentelemetry-specification/blob/v1.22.0/specification/common/attribute-naming.md)
2. [Attribute Naming](https://github.com/open-telemetry/opentelemetry-specification/blob/v1.22.0/specification/common/attribute-naming.md)

e.g.
`db.metrics.db_engine.type`,
`db.metrics.data_compress.ratio`




# Reference

[Metrics Data Model](https://opentelemetry.io/docs/specs/otel/metrics/data-model/)

[OpenTelemetry Semantic Conventions](https://github.com/open-telemetry/semantic-conventions/tree/main/docs)

[Resource Semantic Conventions](https://github.com/open-telemetry/semantic-conventions/blob/main/docs/resource/README.md)

[Trace Semantic Conventions](https://github.com/open-telemetry/semantic-conventions/blob/main/docs/general/trace.md)

[Metrics Semantic Conventions](https://github.com/open-telemetry/semantic-conventions/blob/main/docs/general/metrics.md)

[General Guidelines](https://github.com/open-telemetry/semantic-conventions/blob/main/docs/general/metrics.md#general-guidelines)

[Attribute Requirement Levels for Semantic Conventions](https://github.com/open-telemetry/opentelemetry-specification/blob/v1.22.0/specification/common/attribute-requirement-level.md)

[Metric Requirement Levels for Semantic Conventions](https://github.com/open-telemetry/opentelemetry-specification/blob/v1.22.0/specification/metrics/metric-requirement-level.md)