# Spring Cloud Alibaba Admin Example

## 项目说明

本项目演示如何使用 Spring Cloud Alibaba Admin 相关 Starter 获得对 Spring Cloud Alibaba实例的监控数据。并展示到Prometheus和Grafana中。

Admin中数据来自于使用MicroMeter在Starter中的原生埋点。

此项目只包括Nacos，RocketMQ，Sentinel指标，如果想查看Seata相关指标，可以启动Seata Example按照下列步骤进行查看。

## 正确配置并启动 Nacos Server 2.4.2

在 Nacos 2.4.2 中，加入了用户鉴权相关的功能，在首次启动 Nacos Server 时，需要正确配置，避免出现启动失败的问题。

### 下载 Nacos Server

> 本示例中使用 Nacos Server 版本为 2.4.2！

Nacos 支持直接下载和源码构建两种方式。**推荐在 Spring Cloud Alibaba 2023.x 中使用 Nacos Server 2.4.2 版本。**

1. 直接下载：[Nacos Server 下载页](https://github.com/alibaba/nacos/releases)
2. 源码构建：进入 Nacos [Github 项目页面](https://github.com/alibaba/nacos)，将代码 git clone 到本地自行编译打包，[参考文档](https://nacos.io/zh-cn/docs/quick-start.html)。

### 配置 Nacos Server

打开 `\nacos-server-2.4.2\conf\application.properties` 配置文件，修改以下配置项：

#### 配置数据源

此处以 MySQL 数据库为例，使用 `nacos-server-2.4.2\conf\mysql-schema.sql` 初始化数据库表文件。同时修改以下配置

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

#### 开启鉴权

**注意：不开启在 2.4.2 中会出现登陆失败异常！**

```properties
### The auth system to use, currently only 'nacos' and 'ldap' is supported:
nacos.core.auth.system.type=nacos

### If turn on auth system:
nacos.core.auth.enabled=true
```

#### 设置服务端验证 key

```properties
nacos.core.auth.server.identity.key=test
nacos.core.auth.server.identity.value=test
```

#### 设置默认 token

```properties
### The default token (Base64 String):
nacos.core.auth.plugin.nacos.token.secret.key=SecretKey012345678901234567890123456789012345678901234567890123456789
```

**在使用 Nacos 服务发现和配置功能时，一定要配置 `username` 和 `password` 属性，否则会出现用户未找到异常！**

#### Open API 鉴权

在 nacos server 2.4.2 中使用 Open api 接口时需要鉴权：更多细节请参考：[Nacos api 鉴权](https://nacos.io/zh-cn/docs/auth.html)

1. 获取 accessToken：使用用户名和密码登陆 nacos server：

   `curl -X POST '127.0.0.1:8848/nacos/v1/auth/login' -d 'username=nacos&password=nacos'`

   若用户名和密码正确,返回信息如下:

   `{"accessToken":"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJuYWNvcyIsImV4cCI6MTYwNTYyOTE2Nn0.2TogGhhr11_vLEjqKko1HJHUJEmsPuCxkur-CfNojDo","tokenTtl":18000,"globalAdmin":true}`

2. 使用 accessToken 请求 nacos api 接口：
   
   `curl -X GET '127.0.0.1:8848/nacos/v1/cs/configs?accessToken=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJuYWNvcyIsImV4cCI6MTYwNTYyMzkyM30.O-s2yWfDSUZ7Svd3Vs7jy9tsfDNHs1SuebJB4KlNY8Q&dataId=nacos.example.1&group=nacos_group'`

### 单机启动 Nacos Server

1. 启动 Nacos Server，进入下载到本地并解压完成后的文件夹(使用源码构建的方式则进入编译打包好的文件夹)，再进去其相对文件夹 `nacos/bin`，并对照操作系统实际情况执行如下命令。[详情参考此文档](https://nacos.io/zh-cn/docs/quick-start.html)。

   1. Linux/Unix/Mac 操作系统，执行命令 

      `sh startup.sh -m standalone`

   2. Windows 操作系统，执行命令 

      `cmd startup.cmd -m standalone`

2. 访问 Nacos Server Console

   浏览器输入地址 http://127.0.0.1:8848/nacos ，**首次登陆需要绑定 nacos 用户，因为新版本增加了鉴权，需要应用注册和配置绑定时配置用户名和密码。**

## 正确配置RocketMQ并启动

### Spring Cloud Alibaba RocketMQ

**首先需要启动 RocketMQ 的 Name Server 和 Broker。**

1. 下载[RocketMQ最新的二进制文件](https://www.apache.org/dyn/closer.cgi?path=rocketmq/4.3.2/rocketmq-all-4.3.2-bin-release.zip)，并解压
2. 启动 Name Server

```bash
sh bin/mqnamesrv
```

3. 启动 Broker

```bash
sh bin/mqbroker -n localhost:9876
```

Windows操作系统直接启动 `mqnamesrv.cmd`

然后 `start mqbroker.cmd -n localhost:9876 autoCreateTopicEnable=true`

## Admin 应用示例

#### 完成前置组件配置后，按照顺序启动admin-prometheus-consumer-example和admin-prometheus-provider-example

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

#### 分别使用Nacos RestTemplate，OpenFeign，Reactive

1. 地址栏分别输入

`http://localhost:18083/echo-rest/test`

`http://localhost:18083/echo-feign/test`

`http://localhost:18083/service-call/test`

2. 输入`http://localhost:18083/actuator/metrics`查看指标

```text
"spring.cloud.rpc.openfeign.qps"
"spring.cloud.rpc.reactive.qps"
"spring.cloud.rpc.restTemplate.qps"
```

3. 输入`http://localhost:18083/actuator/metrics/spring-cloud.rpc.reactive.qps`可查看详细数据

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

#### 使用Sentinel RestTemplate的degrade和flow

1. 在地址栏输入
    `http://localhost:18083/rt`

   `http://localhost:18083/get`

2. 输入`http://localhost:18083/actuator/metrics`查看指标

```text
"spring.cloud.alibaba.sentinel.degrade.sum"
"spring.cloud.alibaba.sentinel.flow.sum"
```

3. 输入`http://localhost:18083/actuator/metrics/spring.cloud.alibaba.sentinel.degrade.sum`查看具体指标

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

## 接入Prometheus和Grafana

#### 可以先通过地址`http://localhost:18083/actuator/prometheus`查看传输给Prometheus的数据

<img src="./images/image-20241025103000343.png" alt="image-20241025103000343.png" style="zoom: 50%;" />

**通过docker启动Prometheus和Grafana，启动前修改prometheus文件夹下的config.yml 的targets位置的ip地址请修改为个人电脑的ip**

```yaml
  - job_name: 'admin-prometheus'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: [ '127.0.0.1:18083' ]
```

**输入`http://localhost:9090/targets?search=`有两个刮取指标的地址**

<img src="./images/image-20241025103641209.png" alt="image-20241025103641209.png" style="zoom: 50%;" />

<img src="./images/image-20241025103649642.png" alt="image-20241025103649642.png" style="zoom: 50%;" />

**然后再搜索框进行搜索能看到指标**

<img src="./images/image-20241024225418675.png" alt="image-20241024225418675.png" style="zoom: 50%;" />

<img src="./images/image-20241024225435691.png" alt="image-20241024225435691.png" style="zoom: 50%;" />

#### 使用docker启动grafana

**账户密码均输入admin，然后点击skip**

<img src="./images/image-20241024225527267.png" alt="image-20241024225527267.png" style="zoom: 50%;" />

**添加数据源**

<img src="./images/image-20241024225604518.png" alt="image-20241024225604518.png" style="zoom: 50%;" />

**此处输入个人电脑ip+9090**

<img src="./images/image-20241024225633698.png" alt="image-20241024225633698.png" style="zoom: 50%;" />

**测试是否成功**

<img src="./images/image-20241024225708457.png" alt="image-20241024225708457.png" style="zoom: 50%;" />

**返回到dashboard中import导入文件夹中的json文件作为grafana面板**

[此面板基于SLS JVM监控大盘进行修改](https://grafana.com/grafana/dashboards/12856-jvm-micrometer/)

<img src="./images/image-20241024225744092.png" alt="image-20241024225744092.png" style="zoom: 50%;" />

<img src="./images/image-20241024225835039.png" alt="image-20241024225835039.png" style="zoom: 50%;" />

<img src="./images/image-20241025001135834.png" alt="image-20241025001135834.png" style="zoom: 50%;" />

<img src="./images/image-20241025001146055.png" alt="image-20241025001146055.png" style="zoom: 50%;" />
