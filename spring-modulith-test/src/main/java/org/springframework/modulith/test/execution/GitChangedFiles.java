package org.springframework.modulith.test.execution;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.Set;

public interface GitChangedFiles {

    Set<String> getModifiedFiles() throws IOException, GitAPIException;
}
