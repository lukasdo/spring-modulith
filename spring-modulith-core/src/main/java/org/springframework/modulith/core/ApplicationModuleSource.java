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

import java.util.stream.Stream;

import org.springframework.util.Assert;

/**
 * The source of an {@link ApplicationModule}. Essentially a {@link JavaPackage} and associated naming strategy for the
 * module. This will be used when constructing sources from a base package and an
 * {@link ApplicationModuleDetectionStrategy} so that the names of the module to be created for the detected packages
 * become the trailing name underneath the base package. For example, scanning from {@code com.acme}, an
 * {@link ApplicationModule} located in {@code com.acme.foo.bar} would be named {@code foo.bar}.
 *
 * @author Oliver Drotbohm
 * @since 1.3
 */
record ApplicationModuleSource(
		JavaPackage moduleBasePackage,
		String moduleName) {

	/**
	 * Returns a {@link Stream} of {@link ApplicationModuleSource}s by applying the given
	 * {@link ApplicationModuleDetectionStrategy} to the given base package.
	 *
	 * @param pkg must not be {@literal null}.
	 * @param strategy must not be {@literal null}.
	 * @param fullyQualifiedModuleNames whether to use fully qualified module names.
	 * @return will never be {@literal null}.
	 */
	public static Stream<ApplicationModuleSource> from(JavaPackage pkg, ApplicationModuleDetectionStrategy strategy,
			boolean fullyQualifiedModuleNames) {

		Assert.notNull(pkg, "Base package must not be null!");
		Assert.notNull(strategy, "ApplicationModuleDetectionStrategy must not be null!");

		return strategy.getModuleBasePackages(pkg)
				.flatMap(it -> it.andSubPackagesAnnotatedWith(org.springframework.modulith.ApplicationModule.class))
				.map(it -> new ApplicationModuleSource(it, fullyQualifiedModuleNames ? it.getName() : pkg.getTrailingName(it)));
	}

	/**
	 * Creates a new {@link ApplicationModuleSource} for the given {@link JavaPackage} and name.
	 *
	 * @param pkg must not be {@literal null}.
	 * @param name must not be {@literal null} or empty.
	 * @return will never be {@literal null}.
	 */
	public static ApplicationModuleSource from(JavaPackage pkg, String name) {

		Assert.hasText(name, "Name must not be null or empty!");

		return new ApplicationModuleSource(pkg, name);
	}
}
