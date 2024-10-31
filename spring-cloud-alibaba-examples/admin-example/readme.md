# Spring Cloud Alibaba Admin Example

## Project description

This project demonstrates how to use Spring Cloud Alibaba Admin related Starter to obtain monitoring data for Spring Cloud Alibaba instances. And displayed on Prometheus and Grafana.

The data in Admin comes from the native embedding points in Starter using MicroMeter.

This project only includes Nacos, RocketMQ, Sentinel indicators. If you want to view Seata related indicators, you can start Seata Example and follow the steps below to view them.

## Nacos Server 2.4.2 is properly configured and started

In Nacos 2.4.2, functions related to user authentication are added. When starting Nacos Server for the first time, it needs to be configured correctly to avoid the problem of startup failure.

### Download Nacos Server

> The Nacos serv version used in this example is 2.2.3!

Nacos supports both direct download and source code construction. **Nacos Server version 2.2.3 is recommended for Spring Cloud Alibaba 2022.x.**

1. Direct download: [Nacos Server download page](https://github.com/alibaba/nacos/releases)
2. Source code construction: Enter Nacos [Github project page](https://github.com/alibaba/nacos), git clone the code to the local compilation and packaging [参考文档](https://nacos.io/zh-cn/docs/quick-start.html).

### Configure the Nacos Server

Open the `\nacos-server-2.2.3\conf\application.properties` configuration file and modify the following configuration items:

#### Configure the data source

Take the MySQL database as an example here, and use the `nacos-server-2.2.3\conf\mysql-schema.sql` initialization database table file. Modify the following configuration as well

```properties
#*************** Config Module Related Configurations ***************#
### If use MySQL as datasource:
spring.datasource.platform=mysql

### Count of DB:
db.num=1

### Connect URL of DB:
db.url.0=jdbc:mysql://127.0.0.1:3306/nacos?characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useUnicode=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
db.user.0=root
db.password.0=root

### Connection pool configuration: hikariCP
db.pool.config.connectionTimeout=30000
db.pool.config.validationTimeout=10000
db.pool.config.maximumPoolSize=20
db.pool.config.minimumIdle=2
```

#### Turn on authentication

**Note: If it is not enabled, login failure exception will occur in 2.4.2!**

```properties
### The auth system to use, currently only 'nacos' and 'ldap' is supported:
nacos.core.auth.system.type=nacos

### If turn on auth system:
nacos.core.auth.enabled=true
```

#### Set the server authentication key

```properties
nacos.core.auth.server.identity.key=test
nacos.core.auth.server.identity.value=test
```

#### Set the default token

```properties
### The default token (Base64 String):
nacos.core.auth.plugin.nacos.token.secret.key=SecretKey012345678901234567890123456789012345678901234567890123456789
```

** When using the Nacos service discovery and configuration function, be sure to configure `username` and `password` attribute, otherwise the user will not be found! **

#### Open API authentication

Authentication is required when using the Open api interface in nacos server 2.4.2: For more details, please refer to: [Nacos api authentication](https://nacos.io/zh-cn/docs/auth.html)

1. Obtain accessToken: Use username and password to log in to the nacos server:

   `curl -X POST '127.0.0.1:8848/nacos/v1/auth/login' -d 'username=nacos&password=nacos'`

   If the username and password are correct, the returned information is as follows:

   `{"accessToken":"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJuYWNvcyIsImV4cCI6MTYwNTYyOTE2Nn0.2TogGhhr11_vLEjqKko1HJHUJEmsPuCxkur-CfNojDo", "tokenTtl": 18000, "globalAdmin": true}`

2. Use accessToken to request the nacos api interface:

   `curl -X GET '127.0.0.1:8848/nacos/v1/cs/configs?accessToken=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJuYWNvcyIsImV4cCI6MTYwNTYyMzkyM30.O-s2yWfDSUZ7Svd3Vs7jy9tsfDNHs1SuebJB4KlNY8Q&dataId=nacos.example.1&group=nacos_group'`

### Start the Nacos Server

1. Start Nacos Server, enter the folder after downloading to the local and decompressing (enter the folder after compiling and packaging by using the source code construction method), then enter its relative folder `nacos/bin`, and execute the following command according to the actual situation of the operating system. [详情参考此文档](https://nacos.io/zh-cn/docs/quick-start.html)。

   1. Linux/Unix/Mac operating system, execute the command

      `sh startup.sh -m standalone`

   2. Windows operating system, executing command

      `cmd startup.cmd`

2. Access Nacos Server Console.

   The browser enters the address http://127.0.0.1:8848/nacos , **The first login needs to bind the nacos user, because the new version adds authentication, and the user name and password need to be configured during application registration and configuration binding.**

## Configure RocketMQ and start it

### Spring Cloud Alibaba RocketMQ

**Firstly, it is necessary to start the Name Server and Broker of RocketMQ**

### Download and Startup RocketMQ

You should startup Name Server and Broker before using RocketMQ Binder.

1. Download [RocketMQ](https://archive.apache.org/dist/rocketmq/4.3.2/rocketmq-all-4.3.2-bin-release.zip) and unzip it.
2. Startup Name Server

```
sh bin/mqnamesrv
```

3. Startup Broker

```
sh bin/mqbroker -n localhost:9876
```

## Admin application example

#### After completing the configuration of the front-end components, start admin-prometheus-consumer-example和admin-prometheus-provider-example in order

```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@LoadBalancerClients({
		@LoadBalancerClient("service-provider")
})
public class ConsumerApplication {

	private static final Logger log = LoggerFactory
			.getLogger(ConsumerApplication.class);

	@Autowired
	private StreamBridge streamBridge;

	public static void main(String[] args) {
		SpringApplication.run(ConsumerApplication.class, args);
	}

	@Bean
	public Consumer<Message<SimpleMsg>> consumer() {
		return msg -> {
			log.info(Thread.currentThread().getName() + " Consumer Receive New Messages: " + msg.getPayload().getMsg());
		};
	}
}
```

```java
@EnableDiscoveryClient
@SpringBootApplication
public class ProviderApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProviderApplication.class, args);
	}

	@Autowired
	private StreamBridge streamBridge;

	private static final Logger log = LoggerFactory
			.getLogger(ProviderApplication.class);

	@Bean
	public ApplicationRunner producer() {
		return args -> {
			Thread.sleep(30000);
			for (int i = 0; i < 100; i++) {
				String key = "KEY" + i;
				Map<String, Object> headers = new HashMap<>();
				headers.put(MessageConst.PROPERTY_KEYS, key);
				headers.put(MessageConst.PROPERTY_ORIGIN_MESSAGE_ID, i);
				Message<SimpleMsg> msg = new GenericMessage<SimpleMsg>(new SimpleMsg("Hello RocketMQ " + i), headers);
				streamBridge.send("producer-out-0", msg);
			}
		};
	}
}
```

#### Use Nacos RestTemplate, OpenFeign, Reactive respectively

1. Address bar input

`http://localhost:18083/echo-rest/test`

`http://localhost:18083/echo-feign/test`

`http://localhost:18083/service-call/test`

2. Input` http://localhost:18083/actuator/metrics `view metrics

```text
"spring.cloud.rpc.openfeign.qps"
"spring.cloud.rpc.reactive.qps"
"spring.cloud.rpc.restTemplate.qps"
```

3. Input` http://localhost:18083/actuator/metrics/spring -Cloud.rpc.reactive. qps ` can view detailed data

```json
{
	"name": "spring-cloud.rpc.reactive.qps",
	"description": "Spring Cloud Alibaba QPS metrics when use Reactive RPC Call.",
	"baseUnit": "SECONDS",
	"measurements": [{
		"statistic": "COUNT",
		"value": 17
	}],
	"availableTags": [{
		"tag": "sca.reactive.rpc.method",
		"values": ["GET"]
	},
	{
		"tag": "sca.reactive.rpc",
		"values": ["url: http://10.2.64.89:18080/echo/11  method: GET  status: 200 OK"]
	}]
}
```

#### Using Sentinel RestTemplate for Grading and Flow

1. Enter in the address bar
   `http://localhost:18083/rt`

   `http://localhost:18083/get`

2. Input`http://localhost:18083/actuator/metrics`View metrics

```text
"spring.cloud.alibaba.sentinel.degrade.sum"
"spring.cloud.alibaba.sentinel.flow.sum"
```

3. input`http://localhost:18083/actuator/metrics/spring.cloud.alibaba.sentinel.degrade.sum`can view detailed data

```json
{
	"name": "spring.cloud.alibaba.sentinel.degrade.sum",
	"measurements": [{
		"statistic": "COUNT",
		"value": 16
	}],
	"availableTags": []
}
```

## Integrate Prometheus and Grafana 

#### First provide the address` http://localhost:18083/actuator/prometheus `View data transmitted to Prometheus

<img src="./images/image-20241025103000343.png" alt="image-20241025103000343.png" style="zoom: 50%;" />

**Start Prometheus and Grafana through Docker. Before starting, modify the IP address of the targets location in the config. yml folder of Prometheus to the IP address of your personal computer**

```yaml
  - job_name: 'admin-prometheus'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: [ '127.0.0.1:18083' ]
```

**Input` http://localhost:9090/targets?search= `There are two addresses for scraping metrics**

<img src="./images/image-20241025103641209.png" alt="image-20241025103641209.png" style="zoom: 50%;" />

<img src="./images/image-20241025103649642.png" alt="image-20241025103649642.png" style="zoom: 50%;" />

**Then search in the search box to see the metrics**

<img src="./images/image-20241024225418675.png" alt="image-20241024225418675.png" style="zoom: 50%;" />

<img src="./images/image-20241024225435691.png" alt="image-20241024225435691.png" style="zoom: 50%;" />

#### Starting Grafana with Docker

**Enter admin for both account and password, then click on 'skip'**

<img src="./images/image-20241024225527267.png" alt="image-20241024225527267.png" style="zoom: 50%;" />

**Add data source**

<img src="./images/image-20241024225604518.png" alt="image-20241024225604518.png" style="zoom: 50%;" />

**Enter personal computer IP+9090 here**

<img src="./images/image-20241024225633698.png" alt="image-20241024225633698.png" style="zoom: 50%;" />

**Is the test successful**

<img src="./images/image-20241024225708457.png" alt="image-20241024225708457.png" style="zoom: 50%;" />

**Return to dashboard and import the JSON file from the import folder as the Grafana panel**

[This panel is modified based on the SLS JVM monitoring dashboard](https://grafana.com/grafana/dashboards/12856-jvm-micrometer/)

<img src="./images/image-20241024225744092.png" alt="image-20241024225744092.png" style="zoom: 50%;" />

<img src="./images/image-20241024225835039.png" alt="image-20241024225835039.png" style="zoom: 50%;" />

<img src="./images/image-20241025001135834.png" alt="image-20241025001135834.png" style="zoom: 50%;" />

<img src="./images/image-20241025001146055.png" alt="image-20241025001146055.png" style="zoom: 50%;" />
