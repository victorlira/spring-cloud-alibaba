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

package com.alibaba.cloud.nacos.metrics.aop.interceptor;

import java.util.concurrent.TimeUnit;

import feign.InvocationContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Response;
import feign.ResponseInterceptor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import org.springframework.beans.factory.annotation.Autowired;


public class NacosDiscoveryMetricsOpenFeignInterceptor implements ResponseInterceptor, RequestInterceptor {

	@Autowired
	private PrometheusMeterRegistry prometheusMeterRegistry;
	RequestTemplate request;

	@Override
	public Object intercept(InvocationContext invocationContext, Chain chain) throws Exception {

		Response response = invocationContext.response();

		Counter qpsCounter = Counter.builder("spring.cloud.rpc.openfeign.qps")
				.description("Spring Cloud Alibaba QPS metrics when use OpenFeign RPC Call.")
				.baseUnit(TimeUnit.SECONDS.name())
				.tag("sca.openfeign.rpc", "url: " + request.url()
						+ "  method: " + request.method()
						+ "  status: " + response.status())
				.register(prometheusMeterRegistry);

		qpsCounter.increment();

		return null;
	}

	@Override
	public ResponseInterceptor andThen(ResponseInterceptor nextInterceptor) {
		return ResponseInterceptor.super.andThen(nextInterceptor);
	}

	@Override
	public Chain apply(Chain chain) {
		return ResponseInterceptor.super.apply(chain);
	}

	@Override
	public void apply(RequestTemplate requestTemplate) {
		this.request = requestTemplate;
	}
}
