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
import java.util.Optional;
import java.util.Set;


/**
 * Junit Extension to skip test execution
 *
 * @author Lukas Dohmen
 */
public class ModulithExtension implements ExecutionCondition {

    private static final Logger log = LoggerFactory.getLogger(ModulithExtension.class);

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        try {
            Set<Class<?>> modifiedFiles = ChangedFilesStore.getInstance().getModifiedFiles();
            if (modifiedFiles.isEmpty()) {
                //TODO: Add param to execute tests even though there are no changes
                log.trace("No files changed not running tests");
                return ConditionEvaluationResult.disabled("ModulithExtension: No changes detected");
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
                                    "ModulithExtension: No changes in module", module);
                        }
                    }
                }
            }

        } catch (IOException | ClassNotFoundException | GitAPIException e) {
            log.error("Error while evaluating test execution", e);
            return ConditionEvaluationResult.enabled("ModulithExtension: Changes detected in current module, executing tests");
        }

        return ConditionEvaluationResult.enabled("ModulithExtension: Changes detected in current module, executing tests");

    }

}
