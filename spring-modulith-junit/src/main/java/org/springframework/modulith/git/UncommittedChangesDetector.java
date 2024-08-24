package org.springframework.modulith.git;

import com.tngtech.archunit.thirdparty.com.google.common.collect.Streams;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.core.env.PropertyResolver;
import org.springframework.lang.NonNull;
import org.springframework.modulith.FileChange;
import org.springframework.modulith.FileChangeDetector;

/**
 * Implementation to get latest local file changes.
 *
 * @author Lukas Dohmen
 */
public class UncommittedChangesDetector implements FileChangeDetector {

	@Override
	public @NonNull Set<FileChange> getModifiedFiles(@NonNull PropertyResolver propertyResolver) throws IOException {

		try (var repo = new FileRepositoryBuilder().findGitDir().build()) {
			return findUncommittedChanges(repo).collect(Collectors.toSet());
		}
	}

	private static Stream<FileChange> findUncommittedChanges(Repository repository) throws IOException {
		try (Git git = new Git(repository)) {
			Status status = git.status().call();

			return Streams.concat(status.getUncommittedChanges().stream(), status.getUntracked().stream())
				.map(FileChange::new);
		} catch (GitAPIException e) {
			throw new IOException("Unable to find uncommitted changes", e);
		}
	}

}
