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

package com.alibaba.cloud.stream.binder.rocketmq.metrics;

import com.alibaba.cloud.stream.binder.rocketmq.properties.RocketMQProducerProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.prometheus.PrometheusMeterRegistry;

/**
 * @author kwings6
 */
public final class MessageMeter {
	private MessageMeter() {
	}

	public static DistributionSummary getMQProducerSumCounter(String destination, RocketMQProducerProperties producerProperties,
			PrometheusMeterRegistry prometheusMeterRegistry) {
		return DistributionSummary.builder("spring.cloud.alibaba.rocketmq.producer.message.sum")
				.description("Spring Cloud Alibaba RocketMQ Producer Message Sum")
				.tag("destination", destination)
				.tag("nameServer", producerProperties.getGroup())
				.tag("group", producerProperties.getGroup())
				.tag("producerType", producerProperties.getProducerType())
				.tag("sendType", producerProperties.getSendType())
				.tag("accessChannel", producerProperties.getAccessChannel())
				.register(prometheusMeterRegistry);
	}

	public static DistributionSummary getMQProducerFailCounter(RocketMQProducerProperties producerProperties,
			PrometheusMeterRegistry prometheusMeterRegistry) {
		return DistributionSummary.builder("spring.cloud.alibaba.rocketmq.producer.message.fail.sum")
				.description("Spring Cloud Alibaba RocketMQ Producer Message Fail Sum")
				.tag("nameServer", producerProperties.getGroup())
				.tag("group", producerProperties.getGroup())
				.tag("producerType", producerProperties.getProducerType())
				.tag("sendType", producerProperties.getSendType())
				.tag("accessChannel", producerProperties.getAccessChannel())
				.register(prometheusMeterRegistry);

	}


	public static Counter getMQConsumerSumCounter(String topic, PrometheusMeterRegistry prometheusMeterRegistry) {
		return Counter.builder("spring.cloud.alibaba.rocketmq.consumer.message.success.sum")
				.description("Spring Cloud Alibaba RocketMQ Message Consumer Success Sum")
				.tag("topic", topic)
				.register(prometheusMeterRegistry);
	}
}
