/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.modulith.core;

import static org.assertj.core.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ApplicationModuleDetectionStrategy}.
 *
 * @author Oliver Drotbohm
 */
class ApplicationModuleDetectionStrategyLookupTests {

	@Test // GH-652
	void usesExplicitlyAnnotatedStrategyIfConfigured() {

		System.setProperty("spring.config.additional-location", "classpath:detection/explicitly-annotated.properties");

		assertThat(ApplicationModuleDetectionStrategyLookup.getStrategy())
				.isEqualTo(ApplicationModuleDetectionStrategy.explicitlyAnnotated());
	}

	@Test // GH-652
	void usesDirectSubPackagesStrategyIfConfigured() {

		System.setProperty("spring.config.additional-location", "classpath:detection/direct-sub-packages.properties");

		assertThat(ApplicationModuleDetectionStrategyLookup.getStrategy())
				.isEqualTo(ApplicationModuleDetectionStrategy.directSubPackage());
	}

	@Test // GH-652
	void usesCustomStrategyIfConfigured() {

		System.setProperty("spring.config.additional-location", "classpath:detection/custom-type.properties");

		assertThat(ApplicationModuleDetectionStrategyLookup.getStrategy())
				.isInstanceOf(TestStrategy.class);
	}

	static class TestStrategy implements ApplicationModuleDetectionStrategy {

		@Override
		public Stream<JavaPackage> getModuleBasePackages(JavaPackage basePackage) {
			return null;
		}
	}
}
