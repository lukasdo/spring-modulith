package org.springframework.modulith.test.execution;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.AnnotatedClassFinder;

import java.io.IOException;

/**
 * Instance to store main class between Test Executions
 *
 * @author Lukas Dohmen
 */
public class ApplicationMainClass {

    private static ApplicationMainClass INSTANCE;
    private final Class<?> mainClass;

    private ApplicationMainClass(Class<?> testClass) {
        AnnotatedClassFinder annotatedClassFinder = new AnnotatedClassFinder(SpringBootApplication.class);
        this.mainClass = annotatedClassFinder.findFromClass(testClass);
    }

    public static ApplicationMainClass getInstance(Class<?> testClass) {
        // What happens when in a monorepo multiple projects will be executed
        if (INSTANCE == null) {
            INSTANCE = new ApplicationMainClass(testClass);
        }

        return INSTANCE;
    }

    public Class<?> getMainClass() {
        return mainClass;
    }
}
