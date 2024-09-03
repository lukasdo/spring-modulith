package org.springframework.modulith;

import com.tngtech.archunit.core.domain.JavaClass;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.test.context.AnnotatedClassFinder;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModuleDependency;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.modulith.FileModificationDetector.CLASS_FILE_SUFFIX;
import static org.springframework.modulith.FileModificationDetector.PACKAGE_PREFIX;

// add logging to explain what happens (and why)

/**
 * Junit Extension to skip test execution if no changes happened in the module that the test belongs to.
 *
 * @author Lukas Dohmen, David Bilge
 */
public class ModulithExecutionExtension implements ExecutionCondition {
    public static final String CONFIG_PROPERTY_PREFIX = "spring.modulith.test";
    public static final String PROJECT_ERROR = ModulithExecutionExtension.class.getName() + ".ERROR";
    public static final String PROJECT_ID = ModulithExecutionExtension.class.getName();
    final AnnotatedClassFinder spaClassFinder = new AnnotatedClassFinder(SpringBootApplication.class);
    private static final Logger log = LoggerFactory.getLogger(ModulithExecutionExtension.class);

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (context.getTestMethod().isPresent()) {
            // Is there something similar liken @TestInstance(TestInstance.Lifecycle.PER_CLASS) for Extensions?
            return ConditionEvaluationResult.enabled("Enabled, only evaluating per class");
        }

        ExtensionContext.Store store = context.getRoot().getStore(Namespace.create(ModulithExecutionExtension.class));
        this.writeChangedFilesToStore(store);
        Exception e = store.get(PROJECT_ERROR, Exception.class);
        if (e != null) {
            log.error("ModulithExecutionExtension: Error while evaluating test execution", e);
            return ConditionEvaluationResult.enabled(
                "ModulithExecutionExtension: Error while evaluation test execution, enable Tests");
        }

        Set<Class<?>> modifiedFiles = (Set<Class<?>>) store.get(PROJECT_ID, Set.class);
        if (modifiedFiles.isEmpty()) {
            log.trace("No files changed, running tests");
            return ConditionEvaluationResult.enabled("ModulithExecutionExtension: No changes detected");
        }

        log.trace("Found following changed files {}", modifiedFiles);

        Optional<Class<?>> testClass = context.getTestClass();
        if (testClass.isPresent()) {
            Class<?> mainClass = this.spaClassFinder.findFromClass(testClass.get());

            if (mainClass == null) {// TODO:: Try with @ApplicationModuleTest -> main class
                return ConditionEvaluationResult.enabled(
                    "ModulithExecutionExtension: Unable to locate SpringBootApplication Class");
            }
            ApplicationModules applicationModules = ApplicationModules.of(mainClass);

            String packageName = ClassUtils.getPackageName(testClass.get());

            // always run test if one of whitelisted files is modified (ant matching)
            Optional<ApplicationModule> optionalApplicationModule = applicationModules.getModuleForPackage(packageName);
            if (optionalApplicationModule.isPresent()) {

                Set<JavaClass> dependentClasses = getAllDependentClasses(optionalApplicationModule.get(),
                    applicationModules);

                for (Class<?> modifiedFile : modifiedFiles) {

                    if (optionalApplicationModule.get().contains(modifiedFile)) {
                        return ConditionEvaluationResult.enabled(
                            "ModulithExecutionExtension: Changes in module detected, Executing tests");
                    }

                    if (dependentClasses.stream()
                        .anyMatch(applicationModule -> applicationModule.isEquivalentTo(modifiedFile))) {
                        return ConditionEvaluationResult.enabled(
                            "ModulithExecutionExtension: Changes in dependent module detected, Executing tests");
                    }
                }
            }
        }

        return ConditionEvaluationResult.disabled(
            "ModulithExtension: No Changes detected in current module, executing tests");
    }

    public void writeChangedFilesToStore(ExtensionContext.Store store) {

        var environment = new StandardEnvironment();
        ConfigDataEnvironmentPostProcessor.applyTo(environment);

        var strategy = FileModificationDetector.loadFileModificationDetector(environment);

        store.getOrComputeIfAbsent(PROJECT_ID, s -> {
            Set<Class<?>> changedClasses = new HashSet<>();
            try {
                Set<ModifiedFilePath> modifiedFiles = strategy.getModifiedFiles(environment);

                Set<String> changedClassNames = modifiedFiles.stream()
                    .map(ModifiedFilePath::path)
                    .map(ClassUtils::convertResourcePathToClassName)
                    .filter(path -> path.contains(PACKAGE_PREFIX))
                    .filter(path -> path.endsWith(CLASS_FILE_SUFFIX))
                    .map(path -> path.substring(path.lastIndexOf(PACKAGE_PREFIX) + PACKAGE_PREFIX.length() + 1,
                        path.length() - CLASS_FILE_SUFFIX.length()))
                    .collect(Collectors.toSet());

                for (String className : changedClassNames) {
                    try {
                        Class<?> aClass = ClassUtils.forName(className, null);
                        changedClasses.add(aClass);
                    } catch (ClassNotFoundException e) {
                        log.trace("ModulithExecutionExtension: Unable to find class \"{}\"", className);
                    }
                }
                return changedClasses;
            } catch (IOException | GitAPIException e) {
                log.error("ModulithExecutionExtension: Unable to fetch changed files, executing all tests", e);
                store.put(PROJECT_ERROR, e);
                return changedClasses;
            }
        });
    }

    private Set<JavaClass> getAllDependentClasses(ApplicationModule applicationModule,
        ApplicationModules applicationModules) {

        Set<ApplicationModule> dependentModules = new HashSet<>();
        dependentModules.add(applicationModule);
        this.getDependentModules(applicationModule, applicationModules, dependentModules);

        return dependentModules.stream()
            .map(appModule -> appModule.getDependencies(applicationModules))
            .flatMap(applicationModuleDependencies -> applicationModuleDependencies.stream()
                .map(ApplicationModuleDependency::getTargetType))
            .collect(Collectors.toSet());
    }

    private void getDependentModules(ApplicationModule applicationModule, ApplicationModules applicationModules,
        Set<ApplicationModule> modules) {

        Set<ApplicationModule> applicationModuleDependencies = applicationModule.getDependencies(applicationModules)
            .stream()
            .map(ApplicationModuleDependency::getTargetModule)
            .collect(Collectors.toSet());

        modules.addAll(applicationModuleDependencies);
        if (!applicationModuleDependencies.isEmpty()) {
            for (ApplicationModule applicationModuleDependency : applicationModuleDependencies) {
                this.getDependentModules(applicationModuleDependency, applicationModules, modules);
            }
        }
    }

}
