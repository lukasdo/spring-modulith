package org.springframework.modulith;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.Set;

public interface GitProviderStrategy {

    String CLASS_FILE_SUFFIX = ".java";
    String PACKAGE_PREFIX = "src.main.java";

    Set<String> getModifiedFiles() throws IOException, GitAPIException;

    int getPriority();
}
