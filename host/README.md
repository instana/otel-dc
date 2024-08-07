# Data Collector for Hosts


## Requirements

- Java 11+

Ensure that Java SDK 11+ is installed.
```bash
java -version
```


## Run Data Collector with release (for end users)
Download the latest  [Release package](https://github.com/instana/otel-dc/releases/tag/Release) according to the operating system.


1) Download the installation package:
```bash
wget https://github.com/instana/otel-dc/releases/download/v1.0.3/otel-dc-host-0.2.3.tar
```

2) Extract the package to the desired deployment location:
```bash
tar vxf otel-dc-host-0.2.3.tar
cd otel-dc-host-0.2.3
```

3) Make sure following configuration files are correct for your environment:
  - config/config.yaml
  - config/logging.properties

Refine configuration file (config/config.yaml) according to your host. Right now we provide Data Collector for following hosts:
  - SNMP Host
  - IBM MQ appliance
  - Simple Linux Host

*Note:* The default configuration file config/config.yaml is for linux host. If you want to monitor an IBM MQ appliance, you can copy config/config-mqappliance.yaml to config/config.yaml, or you can also use environment variable "DC_CONFIG" to specify the configuration file, for example:
```bash
export DC_CONFIG=config/config-mqappliance.yaml
```

4) Run Data Collector
Run the Data Collector with the following command according to your current system:
```bash
nohup ./bin/otel-dc-host
```

## Specific Parameters for Host Data Collectors

| Parameter          | Scope        | Description                                                             | Example values            |
|--------------------|--------------|-------------------------------------------------------------------------|---------------------------|
| host.system        | global       | The engine of this host data collector                                  | snmp_host or mq_appliance |  
| snmp.host          | snmp_host    | The endpoint of SNMP host                                               | udp:9.112.252.102/161     |  
| host.name          | snmp_host    | Optional: use this to overwrite the value got by SNMP                   | stantest0.fyre.ibm.com    |  
| os.type            | snmp_host    | Optional: use this to overwrite the value got by SNMP                   | linux                     |  
| community          | snmp_host    | Optional: The community string (SNMP v1 or v2c) (default: public)       | public                    |  
| retries            | snmp_host    | Optional: times to retry (default: 3)                                   | 3                         |  
| timeout            | snmp_host    | Optional: timeout in ms (default: 450)                                  | 450                       |  
| version            | snmp_host    | Optional: version of SNMP (default:2c)                                  | 1, 2, 2c, 3               |
| securityLevel      | snmp_host    | Optional: Choose 1:NOAUTH_NOPRIV 2:AUTH_NOPRIV 3:AUTH_PRIV (default: 1) | 1, 2, 3                   |  
| authPassword       | snmp_host    | Optional: Auth password (version 3)                                     | password1                 |  
| privacyPassword    | snmp_host    | Optional: Privacy password (version 3)                                  | password1                 |  
| securityName       | snmp_host    | Optional: Security name (user) (version 3)                              | user1                     |  
| authType           | snmp_host    | Optional: OID of the Protocol for Auth (version 3)                      | 1.3.6.1.6.3.10.1.1.3      |  
| privacyType        | snmp_host    | Optional: OID of the Protocol for Privacy (version 3)                   | 1.3.6.1.6.3.10.1.2.2      |  
| appliance.host     | mq_appliance | host name for MQ appliance                                              | testbox1.mqappliance.com  |  
| appliance.user     | mq_appliance | user name for MQ appliance                                              | admin                     |  
| appliance.password | mq_appliance | password for MQ appliance                                               | password1                 |  

*Note: We support SNMP version 1, 2c, 3 (USM mode) *

#### OID of the Protocol for Authentication (SNMP version 3)

| Protocol          | OID                  |
|-------------------|----------------------|
| Auth-NONE         | 1.3.6.1.6.3.10.1.1.1 |  
| AuthMD5           | 1.3.6.1.6.3.10.1.1.2 |  
| AuthSHA           | 1.3.6.1.6.3.10.1.1.3 |  
| AuthHMAC128SHA224 | 1.3.6.1.6.3.10.1.1.4 |  
| AuthHMAC192SHA256 | 1.3.6.1.6.3.10.1.1.5 |  
| AuthHMAC256SHA384 | 1.3.6.1.6.3.10.1.1.6 |  
| AuthHMAC384SHA512 | 1.3.6.1.6.3.10.1.1.7 |  

#### OID of the Protocol for Privacy (SNMP version 3)

| Protocol   | OID                        |
|------------|----------------------------|
| Priv-NONE  | 1.3.6.1.6.3.10.1.2.1       |  
| PrivDES    | 1.3.6.1.6.3.10.1.2.2       |  
| Priv3DES   | 1.3.6.1.6.3.10.1.2.3       |  
| PrivAES128 | 1.3.6.1.6.3.10.1.2.4       |  
| PrivAES192 | 1.3.6.1.4.1.4976.2.2.1.1.1 |  
| PrivAES256 | 1.3.6.1.4.1.4976.2.2.1.1.2 |  


## Build & Run (for developers)

1) Make sure Java SDK 11+ is installed.
```bash
java -version
```

2) Get the source code from `github.com`.
```bash
git clone https://github.com/instana/otel-dc.git
cd otel-dc/host
```

3) Build with Gradle
```bash
./gradlew clean build
```
*Note: gradle 7.4 will be installed if you do not have it.*

4) Make sure following configuration files are correct for your environment:
  - config/config.yaml
  - config/logging.properties

Refine configuration file (config/config.yaml) according to your host. Right now we provide Data Collector for following hosts:
  - SNMP Host
  - IBM MQ appliance
  - Simple Linux Host

*Note:* The default configuration file config/config.yaml is for linux host. If you want to monitor an IBM MQ appliance, you can copy config/config-mqappliance.yaml to config/config.yaml, or you can also use environment variable "DC_CONFIG" to specify the configuration file, for example:
```bash
export DC_CONFIG=config/config-mqappliance.yaml
```

5) Start up your OTLP backend which accept OTLP connections. Right now we support following protocols:
- otlp/grpc
- otlp/http

```bash
./gradlew run
```
*Note:* If you want to monitor IBM MQ appliance, you need to install **expect** before starting the Data Collector. You can run the command `which expect` to check if **expect** is already installed.


6) *Appendix:* Run binary in "build/distributions"
Find the deployment package (e.g.:otel-dc-rdb-*.tar/otel-dc-host-*.tar/otel-dc-host-*.tar) generated by gradle in the "build/distributions/" directory, extract deployment files:
```bash
cd build/distributions/
tar vxf otel-dc-host-*.tar
rm -f *.tar *.zip
cd otel-dc-host-*
```

Modify the configuration files.
Then, make sure following configuration files are correct for your environment.:
  - config/config.yaml
  - config/logging.properties

Run the Data Collector with following command according to your current implentation:
```bash
export DC_CONFIG=config/config.yaml
export JAVA_OPTS=-Dconfig/logging.properties
bin/otel-dc-rdb
```
Or run Data Collector in background
```bash
export DC_CONFIG=config/config.yaml
export JAVA_OPTS=-Dconfig/logging.properties
nohup bin/otel-dc-host > /dev/null 2>&1 &
```