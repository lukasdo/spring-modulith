package org.springframework.modulith.test.execution;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.Set;

/**
 * Implementation to get unpushed changes
 */
public class UnpushedChangesStrategy implements GitProviderStrategy{
    @Override
    public Set<String> getModifiedFiles() throws IOException, GitAPIException {
        // TODO
        return Set.of();
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
