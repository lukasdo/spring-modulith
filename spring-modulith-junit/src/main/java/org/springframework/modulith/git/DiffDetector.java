package org.springframework.modulith.git;

import static org.springframework.modulith.ModulithExecutionExtension.CONFIG_PROPERTY_PREFIX;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jgit.diff.DiffEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;
import org.springframework.lang.NonNull;
import org.springframework.modulith.FileChange;
import org.springframework.modulith.FileChangeDetector;

/**
 * Implementation to get changes between HEAD and a complete or abbreviated SHA-1 or other revision, like
 * <code>HEAD~2</code>. See {@link org.eclipse.jgit.lib.Repository#resolve(String)} for more information.
 */
public class DiffDetector implements FileChangeDetector {
	private static final Logger log = LoggerFactory.getLogger(DiffDetector.class);

	@Override
	public @NonNull Set<FileChange> getModifiedFiles(@NonNull PropertyResolver propertyResolver) throws IOException {
		String commitIdToCompareTo = propertyResolver.getProperty(CONFIG_PROPERTY_PREFIX + ".reference-commit");

		try (var repo = JGitUtil.buildRepository()) {
			String compareTo;
			if (commitIdToCompareTo == null || commitIdToCompareTo.isEmpty()) {
				log.warn("No reference-commit configured, comparing to HEAD~1.");
				compareTo = "HEAD~1";
			} else {
				log.info("Comparing to git commit #{}", commitIdToCompareTo);
				compareTo = commitIdToCompareTo;
			}

			String localBranch = repo.getFullBranch();
			Stream<DiffEntry> diffs = JGitUtil.diffRefs(repo, compareTo, localBranch);

			return JGitUtil.convertDiffEntriesToFileChanges(diffs).collect(Collectors.toSet());
		}
	}
}
