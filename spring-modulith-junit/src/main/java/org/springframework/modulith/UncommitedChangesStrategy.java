package org.springframework.modulith;

import com.tngtech.archunit.thirdparty.com.google.common.collect.Streams;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.core.env.PropertyResolver;

/**
 * Implementation to get latest local file changes.
 *
 * @author Lukas Dohmen
 */
public class UncommitedChangesStrategy implements GitProviderStrategy {


	@Override
	public Set<FileChange> getModifiedFiles(PropertyResolver propertyResolver) throws IOException, GitAPIException {
		try (var gitDir = new FileRepositoryBuilder().findGitDir().build()) {
			Git git = new Git(gitDir);
			Status status = git.status().call();

			return Streams.concat(status.getUncommittedChanges().stream(), status.getUntracked().stream())
				.map(FileChange::new)
				.collect(Collectors.toSet());
		}
	}

}
