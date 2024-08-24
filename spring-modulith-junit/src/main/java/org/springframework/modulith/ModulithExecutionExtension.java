package org.springframework.modulith;

import java.util.stream.Collectors;

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
import org.springframework.modulith.git.UncommittedChangesDetector;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.Set;

import static org.springframework.modulith.FileModificationDetector.CLASS_FILE_SUFFIX;
import static org.springframework.modulith.FileModificationDetector.PACKAGE_PREFIX;
import static org.springframework.test.context.junit.jupiter.SpringExtension.getApplicationContext;

// add logging to explain what happens (and why)

/**
 * Junit Extension to skip test execution if no changes happened in the module that the test belongs to.
 *
 * @author Lukas Dohmen
 */
public class ModulithExecutionExtension implements ExecutionCondition {
    public static final String CONFIG_PROPERTY_PREFIX = "spring.modulith.test";
    public static final String PROJECT_ERROR = ModulithExecutionExtension.class.getName() + ".ERROR";
    public static final String PROJECT_ID = ModulithExecutionExtension.class.getName();
    final AnnotatedClassFinder spaClassFinder = new AnnotatedClassFinder(SpringBootApplication.class);
    private static final Logger log = LoggerFactory.getLogger(ModulithExecutionExtension.class);

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (context.getTestMethod().isPresent()) { // Is there something similar like @TestInstance(TestInstance.Lifecycle.PER_CLASS) for Extensions?
            return ConditionEvaluationResult.enabled("Enabled, only evaluating per class");
        }

        ApplicationContext applicationContext = getApplicationContext(context);
        // if there is no applicationContext present, this is probably a unit test -> always execute those, for now

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
            log.trace("No files changed not running tests");
            // We should run all tests when no files are changed. Caching should be done by the build tool, it is out of scope for this library
            return ConditionEvaluationResult.disabled("ModulithExecutionExtension: No changes detected");
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
            boolean isModule = applicationModules.getModuleForPackage(packageName).isPresent();

            // always run test if one of whitelisted files is modified (ant matching)

            if (isModule) {// equals or prefix with a .
                boolean hasChanges = modifiedFiles.stream().map(Class::getPackageName).anyMatch(s -> s.equals(packageName));
                if (hasChanges) {
                    return ConditionEvaluationResult.enabled("ModulithExecutionExtension: No changes in module");
                }
            }
        }

        return ConditionEvaluationResult.disabled("ModulithExtension: No Changes detected in current module, executing tests");
    }


    public void writeChangedFilesToStore(ExtensionContext context, ApplicationContext applicationContext) {
        var strategy = loadGitProviderStrategy(applicationContext);

        ExtensionContext.Store store = context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        store.getOrComputeIfAbsent(PROJECT_ID, s -> {
            Set<Class<?>> changedClasses = new HashSet<>();
            try {
                Set<ModifiedFilePath> modifiedFiles = strategy.getModifiedFiles(applicationContext.getEnvironment());

                Set<String> changedClassNames = modifiedFiles.stream()

                        .map(ModifiedFilePath::path)
                        .map(ClassUtils::convertResourcePathToClassName)
                        .filter(path -> path.contains(PACKAGE_PREFIX)) // DELETED will be filtered as new path will be /dev/null
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

    private FileModificationDetector loadGitProviderStrategy(ApplicationContext applicationContext) {
        var property = applicationContext.getEnvironment()
            .getProperty(CONFIG_PROPERTY_PREFIX + ".changed-files-strategy");

        FileModificationDetector strategy = ServiceLoader.load(FileModificationDetector.class)
            .stream()
            .filter(strategyProvider -> strategyProvider.type().getName().equals(property))
            .findFirst()
            .map(Provider::get)
            .orElseGet(UncommittedChangesDetector::new);

        log.info("Strategy for finding changed files is '{}'", strategy.getClass().getName());

        return strategy;
    }

}
