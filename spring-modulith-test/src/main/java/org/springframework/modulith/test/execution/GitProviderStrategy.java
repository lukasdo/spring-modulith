package org.springframework.modulith.test.execution;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.Set;

public interface GitProviderStrategy {

    Set<String> getModifiedFiles() throws IOException, GitAPIException;

    int getPriority();
}
