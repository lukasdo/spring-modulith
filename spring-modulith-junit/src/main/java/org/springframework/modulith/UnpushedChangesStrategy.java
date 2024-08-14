package org.springframework.modulith;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.core.env.PropertyResolver;

/**
 * Implementation to get unpushed changes
 */
public class UnpushedChangesStrategy implements GitProviderStrategy {
	@Override
	public Set<FileChange> getModifiedFiles(PropertyResolver propertyResolver) throws IOException, GitAPIException {
		try (var gitDir = new FileRepositoryBuilder().findGitDir().build()) {
			Git git = new Git(gitDir);
			List<DiffEntry> diffEntries = git.diff().call();

			return JGitSupport.convertDiffEntriesToFileChanges(diffEntries);
		}
	}

}
