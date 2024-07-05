# ODCD (OpenTelemetry Data Collector for Telemetry Data)

**[Semantic Convention](docs/semconv)** |
**[Support](docs/support/README.md)** |
**[Changelog](CHANGELOG.md)** |
**[Contributing](CONTRIBUTING.md)** |
**[License](LICENSE)**

---
ODCD (OpenTelemetry Data Collector for Telemetry Data) is a collection of stanalone OpenTelemetry receivers for databases, systems, and apps. All implementations are based on predefined OpenTelemetry Semantic Conventions. A standard OTLP exporter is provided to forward the data from this "Data Collector" to a Telemetry backend or an OpenTelemetry Collector.

<br><br>

# Data Collectors provided

- [OTel Data Collectors for Relational Databases](rdb/README.md) (**Java 8+**)
- [OTel Data Collectors for Host](host/README.md) (**Java 11+**)
- [OTel Data Collectors for LLM](llm/README.md) (**Java 11+**)

