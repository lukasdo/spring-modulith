package org.springframework.modulith;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jgit.diff.DiffEntry;

final class JGitSupport {
	private JGitSupport() {}

	static Set<FileChange> convertDiffEntriesToFileChanges(Collection<DiffEntry> diffEntries) {
		return diffEntries.stream()
			.flatMap(entry -> Stream.of(new FileChange(entry.getNewPath()), new FileChange(entry.getOldPath())))
			.filter(change -> !change.path().equals("/dev/null"))
			.collect(Collectors.toSet());
	}
}
