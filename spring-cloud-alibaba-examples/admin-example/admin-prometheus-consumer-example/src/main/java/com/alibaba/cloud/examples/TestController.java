/*
 * Copyright 2013-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.examples;


import java.util.HashMap;
import java.util.Map;

import com.alibaba.cloud.examples.common.SimpleMsg;
import com.alibaba.cloud.examples.feign.EchoClient;
import jakarta.annotation.Resource;
import org.apache.rocketmq.common.message.MessageConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Example of remote invocation of service fusing and load balancing.
 *
 * @author kwings6
 */

@RestController
public class TestController {

	private static final Logger log = LoggerFactory
			.getLogger(ConsumerApplication.class);

	@Autowired
	private RestTemplate urlCleanedRestTemplate;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private EchoClient echoClient;

	@Autowired
	private DiscoveryClient discoveryClient;

	@Autowired
	private StreamBridge streamBridge;

	private static final String SERVICE_PROVIDER_ADDRESS = "http://service-provider";

	@Resource
	private ReactiveDiscoveryClient reactiveDiscoveryClient;

	@Resource
	private WebClient.Builder webClientBuilder;

	@GetMapping("/pro")
	public ApplicationRunner producerDelay() {
		return args -> {
			for (int i = 0; i < 100; i++) {
				String key = "KEY" + i;
				Map<String, Object> headers = new HashMap<>();
				headers.put(MessageConst.PROPERTY_KEYS, key);
				headers.put(MessageConst.PROPERTY_ORIGIN_MESSAGE_ID, i);
				headers.put(MessageConst.PROPERTY_DELAY_TIME_LEVEL, 2);
				Message<SimpleMsg> msg = new GenericMessage(new SimpleMsg("Delay RocketMQ " + i), headers);
				streamBridge.send("producer-out-0", msg);
			}
		};
	}

	@GetMapping("/exp")
	public String exp() {
		return restTemplate.getForObject("https://httpbin.org/status/500", String.class);
	}

	@GetMapping("/rt")
	public String rt() {
		return restTemplate.getForObject("https://httpbin.org/delay/3", String.class);
	}

	@GetMapping("/get")
	public String get() {
		return restTemplate.getForObject("https://httpbin.org/get", String.class);
	}

	@GetMapping("/all-services")
	public Flux<String> allServices() {
		return reactiveDiscoveryClient.getInstances("service-provider")
				.map(serviceInstance -> serviceInstance.getHost() + ":"
						+ serviceInstance.getPort());
	}

	@GetMapping("/service-call/{name}")
	public Mono<String> serviceCall(@PathVariable("name") String name) {
		return webClientBuilder.build().get()
				.uri("http://service-provider/echo/" + name).retrieve()
				.bodyToMono(String.class);
	}

	@GetMapping("/echo-rest/{str}")
	public String rest(@PathVariable String str) {
		return urlCleanedRestTemplate
				.getForObject(SERVICE_PROVIDER_ADDRESS + "/echo/" + str,
						String.class);
	}

	@GetMapping("/index")
	public String index() {
		return restTemplate.getForObject(SERVICE_PROVIDER_ADDRESS, String.class);
	}

	@GetMapping("/test")
	public String test() {
		return restTemplate
				.getForObject(SERVICE_PROVIDER_ADDRESS + "/test", String.class);
	}

	@GetMapping("/sleep")
	public String sleep() {
		return restTemplate
				.getForObject(SERVICE_PROVIDER_ADDRESS + "/sleep", String.class);
	}

	@GetMapping("/notFound-feign")
	public String notFound() {
		return echoClient.notFound();
	}

	@GetMapping("/divide-feign")
	public String divide(@RequestParam Integer a, @RequestParam Integer b) {
		return echoClient.divide(a, b);
	}

	@GetMapping("/divide-feign2")
	public String divide(@RequestParam Integer a) {
		return echoClient.divide(a);
	}

	@GetMapping("/echo-feign/{str}")
	public String feign(@PathVariable String str) {
		return echoClient.echo(str);
	}

	@GetMapping("/services/{service}")
	public Object client(@PathVariable String service) {
		return discoveryClient.getInstances(service);
	}

	@GetMapping("/services")
	public Object services() {
		return discoveryClient.getServices();
	}

}
