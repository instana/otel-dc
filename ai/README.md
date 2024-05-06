# ODCAI (OTel based Data Collector for AI)

ODCAI (OTel based Data Collector for AI) is the tool or template to generate OpenTelemetry metrics for various LLM and LLM Applications. All implementation are based on predefined OpenTelemetry Semantic Conventions.

## Requirements

- Java 11+

Ensure that Java SDK 11+ is installed.
```bash
java -version
```

## Installation

1) Download the installation package:
```bash
curl -O https://github.com/instana/otel-dc/releases/download/Release/otel-dc-ai_1.0.0_linux_amd64.tar.gz
```
2) Extract the package to the desired deployment location:
```bash
tar vxf otel-dc-ai_1.0.0_linux_amd64.tar.gz
```

## Configuration
```bash
cd otel-dc-ai-1.0.0
vi config/config.yaml
```
The following options are required：
- `otel.backend.url`：The OTel gRPC address of the agent, for example: http://localhost:4317
- `otel.service.name`：The Data Collector name, which can be any string you choose.
- `price.prompt.tokens.per.kilo`：The unit price per thousand prompt tokens.
- `price.complete.tokens.per.kilo`：The unit price per thousand complete tokens.


## Run ODCAI
Run the Data Collector with the following command according to your current system:
```bash
nohup ./bin/otel-dc-ai &
```

## Reference
If your platform is not supported by the pre-built binaries or if you prefer to compile from source, you can follow the steps in [DEVELOP.md](DEVELOP.md)
