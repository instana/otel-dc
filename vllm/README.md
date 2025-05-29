# ODCV (OTel Data Collector for vLLM)

ODCV (OTel Data Collector for vLLM) is the tool or template to generate OpenTelemetry metrics for various vLLM Applications. All implementation are based on predefined OpenTelemetry Semantic Conventions.

## Requirements

- Java 11+

Ensure that Java SDK 11+ is installed.
```bash
java -version
```

## Installation

1) Download the installation package:
```bash
curl -O https://github.com/instana/otel-dc/releases/download/v1.0.8/otel-dc-vllm-1.0.1.tar
```
2) Extract the package to the desired deployment location:
```bash
tar vxf otel-dc-vllm-1.0.1.tar
```

## Configuration

### Configure otel dc
```bash
cd otel-dc-vllm-1.0.1
vi config/config.yaml
```
The following options are required：
- `otel.vllm.metrics.url`: List of endpoints of vllm servers to collect the metrics from. If tls is enabled please use https.
- `otel.agentless.mode`: The connection mode of the OTel data connector, the default mode is agentless.
- `otel.backend.url`: The gRPC endpoint of the Instana backend or Instana agent, that depends on agentless or not.
- `callback.interval`: The time interval in seconds to post data to backend or agent.
- `otel.service.name`: The Data Collector name, which can be any string that you choose.

## Run ODCV
Run the Data Collector with the following command according to your current system:
```bash
nohup ./bin/otel-dc-vllm >/dev/null 2>&1 &
```

## Reference
If your platform is not supported by the pre-built binaries or if you prefer to compile from source, you can follow the steps in [DEVELOP.md](DEVELOP.md)
