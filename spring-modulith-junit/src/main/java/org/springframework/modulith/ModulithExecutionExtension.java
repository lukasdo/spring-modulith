package org.springframework.modulith;

import static org.springframework.test.context.junit.jupiter.SpringExtension.getApplicationContext;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.Set;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.AnnotatedClassFinder;
import org.springframework.context.ApplicationContext;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.util.ClassUtils;


/**
 * Junit Extension to skip test execution if no changes happened in the module that the test belongs to.
 *
 * @author Lukas Dohmen
 */
public class ModulithExecutionExtension implements ExecutionCondition {
    public static final String CONFIG_PROPERTY_PREFIX = "spring.modulith.test";
    public static final String PROJECT_ERROR = ModulithExecutionExtension.class.getName() + ".ERROR";
    public static final String PROJECT_ID = ModulithExecutionExtension.class.getName();

    private static final Logger log = LoggerFactory.getLogger(ModulithExecutionExtension.class);
    final AnnotatedClassFinder annotatedClassFinder = new AnnotatedClassFinder(SpringBootApplication.class);

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {

        if (context.getTestMethod()
            .isPresent()) { // Is there something similar like @TestInstance(TestInstance.Lifecycle.PER_CLASS) for Extensions?
            return ConditionEvaluationResult.enabled("Enabled, only evaluating per class");
        }

        ApplicationContext applicationContext = getApplicationContext(context);

        this.writeChangedFilesToStore(context, applicationContext);

        ExtensionContext.Store store = context.getStore(ExtensionContext.Namespace.GLOBAL);
        Exception e = store.get(PROJECT_ERROR, Exception.class);
        if (e != null) {
            log.error("ModulithExecutionExtension: Error while evaluating test execution", e);
            return ConditionEvaluationResult.enabled(
                "ModulithExecutionExtension: Error while evaluation test execution, enable Tests");
        }

        Set<Class<?>> modifiedFiles = (Set<Class<?>>) store.get(PROJECT_ID, Set.class);
        if (modifiedFiles.isEmpty()) {
            // What happens when there are no changes at all?
            log.trace("No files changed not running tests");
            return ConditionEvaluationResult.disabled("ModulithExecutionExtension: No changes detected");
        }

        log.trace("Found following changed files {}", modifiedFiles);

        Optional<Class<?>> testClass = context.getTestClass();
        if (testClass.isPresent()) {
            Class<?> mainClass = this.annotatedClassFinder.findFromClass(testClass.get());

            if (mainClass == null) {
                return ConditionEvaluationResult.enabled(
                    "ModulithExecutionExtension: Unable to locate SpringBootApplication Class");
            }
            ApplicationModules applicationModules = ApplicationModules.of(mainClass);

            // What happens when changes occur in shared module
            String packageName = ClassUtils.getPackageName(testClass.get());
            boolean isModule = applicationModules.getModuleForPackage(packageName).isPresent();

            if (isModule) {
                boolean hasChanges = modifiedFiles.stream()
                    .map(Class::getPackageName)
                    .anyMatch(s -> s.equals(packageName));
                if (hasChanges) {
                    return ConditionEvaluationResult.enabled("ModulithExecutionExtension: No changes in module");
                }
            }
        }

        return ConditionEvaluationResult.disabled(
            "ModulithExtension: No Changes detected in current module, executing tests");
    }


    public void writeChangedFilesToStore(ExtensionContext context, ApplicationContext applicationContext) {
        var strategy = loadGitProviderStrategy(applicationContext);

        ExtensionContext.Store store = context.getStore(ExtensionContext.Namespace.GLOBAL);
        // TODO computing with every test class -> A store is bound to its extension context lifecycle.
        // Use something like ApplicationContext Caching in Spring -> ContextCache
        store.getOrComputeIfAbsent(PROJECT_ID, s -> {
            Set<Class<?>> set = new HashSet<>();
            try {
                for (String file : strategy.getModifiedFiles()) {
                    try {
                        Class<?> aClass = ClassUtils.forName(file, null);
                        set.add(aClass);
                    } catch (ClassNotFoundException e) {
                        log.trace("ModulithExecutionExtension: Unable to find class for file {}", file);
                    }
                }
                return set;
            } catch (IOException | GitAPIException e) {
                log.error("ModulithExecutionExtension: Unable to fetch changed files, executing all tests", e);
                store.put(PROJECT_ERROR, e);
                return set;
            }
        });
    }

    private GitProviderStrategy loadGitProviderStrategy(ApplicationContext applicationContext) {
        var property = applicationContext.getEnvironment()
            .getProperty(CONFIG_PROPERTY_PREFIX + ".changed-files-strategy");

        GitProviderStrategy strategy = ServiceLoader.load(GitProviderStrategy.class)
            .stream()
            .filter(strategyProvider -> strategyProvider.type().getName().equals(property))
            .findFirst()
            .map(Provider::get)
            .orElseGet(UncommitedChangesStrategy::new);

        log.info("Strategy for finding changed files is '{}'", strategy.getClass().getName());

        return strategy;
    }

}
