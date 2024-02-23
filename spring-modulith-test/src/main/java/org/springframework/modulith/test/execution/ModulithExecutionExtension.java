package org.springframework.modulith.test.execution;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;


/**
 * Junit Extension to skip test execution if no changes happened in the module that the test belongs to.
 *
 * @author Lukas Dohmen
 */
public class ModulithExecutionExtension implements ExecutionCondition {

    private static final Logger log = LoggerFactory.getLogger(ModulithExecutionExtension.class);
    public static final String PROJECT_ID = ModulithExecutionExtension.class.getName();
    public static final String PROJECT_ERROR = ModulithExecutionExtension.class.getName() + ".ERROR";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        this.writeChangedFilesToStore(context);

        ExtensionContext.Store store = context.getStore(ExtensionContext.Namespace.GLOBAL);
        Exception e = store.get(PROJECT_ERROR, Exception.class);
        if (e != null) {
            log.error("ModulithExecutionExtension: Error while evaluating test execution", e);
            return ConditionEvaluationResult.enabled("ModulithExecutionExtension: Error while evaluation test execution, enable Tests");
        }

        Set<Class<?>> modifiedFiles = (Set<Class<?>>) store.get(PROJECT_ID, Set.class);
        if (modifiedFiles.isEmpty()) {
            //TODO: Add param to execute tests even though there are no changes
            log.trace("No files changed not running tests");
            return ConditionEvaluationResult.disabled("ModulithExecutionExtension: No changes detected");
        }

        log.trace("Found following changed files {}", modifiedFiles);
        Optional<Class<?>> testClass = context.getTestClass();


        if (testClass.isPresent()) {
            Class<?> mainClass = ApplicationMainClass.getInstance(testClass.get()).getMainClass();
            ApplicationModules applicationModules = ApplicationModules.of(mainClass);

            boolean isModule;
            for (String module : Arrays.stream(ClassUtils.getPackageName(testClass.get()).split("\\.")).toList()) {

                isModule = applicationModules.getModuleByName(module).isPresent();
                if (isModule) {

                    boolean hasChanges = modifiedFiles.stream().map(Class::getPackageName).anyMatch(s -> s.equals(module));

                    if (!hasChanges) {
                        return ConditionEvaluationResult.disabled(
                                "ModulithExecutionExtension: No changes in module", module);
                    }
                }
            }
        }

        return ConditionEvaluationResult.enabled("ModulithExtension: Changes detected in current module, executing tests");

    }


    public void writeChangedFilesToStore(ExtensionContext context) {
        ExtensionContext.Store store = context.getStore(ExtensionContext.Namespace.GLOBAL);
        // TODO seems to be computing with every test class test suite
        store.getOrComputeIfAbsent(PROJECT_ID, s -> {
            Set<Class<?>> set = new HashSet<>();
            try {
                LocalChangesStrategy localChangesStrategy = new LocalChangesStrategy();
                for (String file : localChangesStrategy.getModifiedFiles()) {
                    try {
                        Class<?> aClass = ClassUtils.forName(file, null);
                        set.add(aClass);
                    } catch (ClassNotFoundException e) {
                        log.trace("ModulithExecutionExtension: Unable to find class for file", file);
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

}
