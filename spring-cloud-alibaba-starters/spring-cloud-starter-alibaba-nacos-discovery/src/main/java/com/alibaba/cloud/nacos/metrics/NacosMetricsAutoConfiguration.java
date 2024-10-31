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

package com.alibaba.cloud.nacos.metrics;

import com.alibaba.cloud.nacos.ConditionalOnNacosDiscoveryEnabled;
import com.alibaba.cloud.nacos.metrics.aop.NacosDiscoveryMetricsReactiveBeanPostProcessor;
import com.alibaba.cloud.nacos.metrics.aop.NacosDiscoveryMetricsRestBeanPostProcessor;
import com.alibaba.cloud.nacos.metrics.aop.interceptor.NacosDiscoveryMetricsOpenFeignInterceptor;
import com.alibaba.cloud.nacos.metrics.aop.interceptor.NacosDiscoveryMetricsReactiveInterceptor;
import com.alibaba.cloud.nacos.metrics.aop.interceptor.NacosDiscoveryMetricsRestTemplateInterceptor;
import feign.Client;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration(proxyBeanMethods = false)
@ConditionalOnDiscoveryEnabled
@ConditionalOnNacosDiscoveryEnabled
public class NacosMetricsAutoConfiguration {

	@Bean
	InitializingBean forcePrometheusPostProcessor(BeanPostProcessor meterRegistryPostProcessor, PrometheusMeterRegistry registry) {
		return () -> meterRegistryPostProcessor.postProcessAfterInitialization(registry, "");
	}

	@ConditionalOnClass(WebClient.class)
	@ConditionalOnBean(WebClient.Builder.class)
	protected static class NacosMetricsReactiveConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public NacosDiscoveryMetricsReactiveInterceptor nacosDiscoveryMetricsReactiveInterceptor() {
			return new NacosDiscoveryMetricsReactiveInterceptor();
		}

		@Bean
		public NacosDiscoveryMetricsReactiveBeanPostProcessor nacosDiscoveryMetricsReactiveBeanPostProcessor() {
			return new NacosDiscoveryMetricsReactiveBeanPostProcessor();
		}
	}

	@ConditionalOnClass(RestTemplate.class)
	protected static class NacosMetricsRestConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public NacosDiscoveryMetricsRestTemplateInterceptor nacosDiscoveryMetricsRestTemplateInterceptor() {
			return new NacosDiscoveryMetricsRestTemplateInterceptor();
		}

		@Bean
		public NacosDiscoveryMetricsRestBeanPostProcessor nacosDiscoveryMetricsRestBeanPostProcessor() {
			return new NacosDiscoveryMetricsRestBeanPostProcessor();
		}
	}

	@ConditionalOnClass(Client.class)
	protected static class NacosMetricsOpenFeignConfiguration {

		@Bean(name = "nacosDiscoveryMetricsOpenFeignInterceptor")
		@ConditionalOnMissingBean
		public NacosDiscoveryMetricsOpenFeignInterceptor nacosDiscoveryMetricsOpenFeignInterceptor() {
			return new NacosDiscoveryMetricsOpenFeignInterceptor();
		}

	}

}
