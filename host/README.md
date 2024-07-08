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
wget https://github.com/instana/otel-dc/releases/download/v1.0.1/otel-dc-host-0.2.2.tar
```

2) Extract the package to the desired deployment location:
```bash
tar vxf otel-dc-host-0.2.2.tar
cd otel-dc-host-0.2.2
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

| Parameter          | Scope                 | Description                                                                 | Example                   |
|--------------------|-----------------------|-----------------------------------------------------------------------------|---------------------------|
| host.system        | global                | The engine of this host data collector                                      | snmp_host or mq_appliance |  
| snmp.host          | instance/snmp_host    | The endpoint of SNMP host                                                   | udp:9.112.252.102/161     |  
| host.name          | instance/snmp_host    | Optional: use this to overwrite the value got by SNMP                       | stantest0.fyre.ibm.com    |  
| os.type            | instance/snmp_host    | Optional: use this to overwrite the value got by SNMP                       | linux                     |  
| community          | instance/snmp_host    | Optional: The community string (default: public)                            | public                    |  
| retries            | instance/snmp_host    | Optional: times to retry (default: 3)                                       | 3                         |  
| timeout            | instance/snmp_host    | Optional: timesout in ms (default: 450)                                     | 450                       |  
| version            | instance/snmp_host    | Optional: version of SNMP (0:version1, 1:version2c, 3:version3) (default:1) | 1                         |  
| securityLevel      | instance/snmp_host    | Optional: Choose 1:NOAUTH_NOPRIV 2:AUTH_NOPRIV 3:AUTH_PRIV (default: 1)     | 3                         |  
| authPassword       | instance/snmp_host    | Optional: Auth password (version 3) (default: "")                           | password                  |  
| privacyPassword    | instance/snmp_host    | Optional: Privacy password (version 3) (default: "")                        | password                  |  
| appliance.host     | instance/mq_appliance | host name for MQ appliance                                                  | testbox1.mqappliance.com  |  
| appliance.user     | instance/mq_appliance | user name for MQ appliance                                                  | admin                     |  
| appliance.password | instance/mq_appliance | password for MQ appliance                                                   | xxxx                      |  


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