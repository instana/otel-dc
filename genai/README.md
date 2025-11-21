# ODCG (OTel Data Collector for GenAI)

ODCG (OTel Data Collector for GenAI) is the tool or template to generate OpenTelemetry metrics for various GenAI applications, including LLM and VectorDB services. All implementations are based on predefined OpenTelemetry Semantic Conventions.

## Requirements

- Java 11+

Ensure that Java SDK 11+ is installed.
```bash
java -version
```

## Installation

1) Download the installation package:
```bash
curl -O https://github.com/instana/otel-dc/releases/download/v1.0.13/otel-dc-genai-1.0.10.tar
```
2) Extract the package to the desired deployment location:
```bash
tar vxf otel-dc-genai-1.0.10.tar
```

## Configuration

### Configure otel dc
```bash
cd otel-dc-genai-1.0.10
vi config/config.yaml
```
The following options are required：
- `otel.agentless.mode`: The connection mode of the OTel data connector, the default mode is agentless.
- `otel.backend.url`: The gRPC endpoint of the Instana backend or Instana agent, that depends on agentless or not.
- `callback.interval`: The time interval in seconds to post data to backend or agent.
- `otel.service.name`: The Data Collector name, which can be any string that you choose.
- `otel.service.port`: The listen port of Data Collector for receiving the metrics data from the instrumented applications, the default port is 8000.


### Configure model price
```bash
vi config/prices.properties
```
Customize more price items by the following format:
```
<aiSystem>.<modelId>.input=0.0
<aiSystem>.<modelId>.output=0.0
```
The <modelId> can be set to '*' to match all modelIds within the aiSystem, but this has a lower priority than items with a specific modelId specified.

## Run ODCG
Run the Data Collector with the following command according to your current system:
```bash
nohup ./bin/otel-dc-genai &
```

## Package Renaming notice

This project was previously named otel-dc-llm. Starting from version 1.0.8, it has been renamed to otel-dc-genai. 
The versioning continues seamlessly from the previous package, with otel-dc-genai-1.0.8 being the successor to otel-dc-llm-1.0.7.
If you're upgrading from the previous package (otel-dc-llm), switch to otel-dc-genai starting from version 1.0.8 or later.

## Reference
If your platform is not supported by the pre-built binaries or if you prefer to compile from source, you can follow the steps in [DEVELOP.md](DEVELOP.md)
