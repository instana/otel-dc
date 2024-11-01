# ODCD (OpenTelemetry Data Collector Drivers)

**[Semantic Convention](docs/semconv)** |
**[Changelog](CHANGELOG.md)** |
**[Contributing](CONTRIBUTING.md)** |
**[License](LICENSE)**

---
ODCD (OpenTelemetry Data Collector Drivers) is a collection of standalone OpenTelemetry receivers for databases, systems, apps, and more. All implementations are based on predefined OpenTelemetry Semantic Conventions. A standard OTLP exporter is provided to forward the data from this "Data Collector" to a telemetry backend or an OpenTelemetry Collector.
<br>


## Data Collectors Provided

- [OTel Data Collectors for Relational Databases](rdb/README.md) (**Java 8+**)
- [OTel Data Collectors for Host](host/README.md) (**Java 11+**)
- [OTel Data Collectors for LLM](llm/README.md) (**Java 11+**)


## Common Parameters for Data Collectors

| Parameter                 | Scope     | Description                                                                                                           | Example                |
|---------------------------|-----------|-----------------------------------------------------------------------------------------------------------------------|------------------------|
| otel.backend.url          | instance  | The OTLP URL of the OTel Backend. E.g., http://localhost:4317 (grpc) or http://localhost:4318/v1/metrics (http)       | http://127.0.0.1:4317  |  
| otel.backend.using.http   | instance  | false (default, using otlp/grpc) or true (using otlp/http)                                                            | false                  |  
| otel.service.name         | instance  | The OTel Service name (required by OTel)                                                                         | DamengDC               |  
| otel.service.instance.id  | instance  | The OTel Service instance ID (which is the ID of this database entity. It can be generated by DC if it is not provided)   | 1.2.3.4:5236@MYDB      |  
| poll.interval             | instance  | The time interval in seconds to query metrics                                                                         | 25                     |  
| callback.interval         | instance  | The time interval in seconds to post data to backend                                                                  | 30                     |
