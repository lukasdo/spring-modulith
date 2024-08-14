package org.springframework.modulith;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation to get latest local file changes.
 *
 * @author Lukas Dohmen
 */
public class UncommitedChangesStrategy implements GitProviderStrategy {


    @Override
    public Set<String> getModifiedFiles() throws IOException,GitAPIException {
        try (var gitDir = new FileRepositoryBuilder().findGitDir().build()) {
            Git git = new Git(gitDir);
            Status status = git.status().call();
            Set<String> modified = status.getUncommittedChanges();
			//TODO:: Add untracked
            return modified.stream().map(ClassUtils::convertResourcePathToClassName)
                    .filter(s -> s.contains(PACKAGE_PREFIX))
                    .filter(s -> s.endsWith(CLASS_FILE_SUFFIX))
                    .map(s -> s.substring(s.lastIndexOf(PACKAGE_PREFIX) + PACKAGE_PREFIX.length() + 1, s.length() - CLASS_FILE_SUFFIX.length()))
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public int getPriority() {
        return 0;
    }
}
