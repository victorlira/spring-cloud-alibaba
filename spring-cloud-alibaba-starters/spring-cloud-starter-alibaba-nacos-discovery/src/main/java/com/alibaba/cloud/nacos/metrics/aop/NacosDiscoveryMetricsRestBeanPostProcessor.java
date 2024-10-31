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

package com.alibaba.cloud.nacos.metrics.aop;

import java.util.Arrays;

import com.alibaba.cloud.nacos.metrics.aop.interceptor.NacosDiscoveryMetricsRestTemplateInterceptor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.web.client.RestTemplate;


public class NacosDiscoveryMetricsRestBeanPostProcessor implements BeanPostProcessor {
	@Autowired
	private NacosDiscoveryMetricsRestTemplateInterceptor restTemplateInterceptor;

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof RestTemplate) {
			RestTemplate restTemplate = (RestTemplate) bean;
			restTemplate.setInterceptors(Arrays.asList(restTemplateInterceptor));
		}
		return bean;
	}
}
