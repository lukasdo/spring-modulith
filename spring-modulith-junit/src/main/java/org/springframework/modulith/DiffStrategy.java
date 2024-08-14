package org.springframework.modulith;

import static org.springframework.modulith.ModulithExecutionExtension.CONFIG_PROPERTY_PREFIX;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;
import org.springframework.util.ClassUtils;

/**
 * Implementation to get changes between HEAD and a complete or abbreviated SHA-1
 */
public class DiffStrategy implements GitProviderStrategy {
	private static final Logger log = LoggerFactory.getLogger(DiffStrategy.class);

	@Override
	public Set<String> getModifiedFiles(PropertyResolver propertyResolver) throws IOException {
		String commitIdToCompareTo = propertyResolver.getProperty(CONFIG_PROPERTY_PREFIX + ".reference-commit");

		try (var gitDir = new FileRepositoryBuilder().findGitDir().build()) {
			String compareTo;
			if (commitIdToCompareTo == null || commitIdToCompareTo.isEmpty()) {
				log.warn("No reference-commit configured, comparing to HEAD~1.");
				compareTo = "HEAD~1^{tree}";
			} else {
				log.info("Comparing to git commit #{}", commitIdToCompareTo);
				compareTo = "%s^{tree}".formatted(commitIdToCompareTo);
			}

			Git git = new Git(gitDir);

			ObjectReader reader = git.getRepository().newObjectReader();
			CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
			ObjectId random = git.getRepository().resolve(compareTo);
			oldTreeIter.reset(reader, random);
			CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
			ObjectId newTree = git.getRepository().resolve("HEAD^{tree}");
			newTreeIter.reset(reader, newTree);

			DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
			diffFormatter.setRepository(git.getRepository());
			List<DiffEntry> entries = diffFormatter.scan(oldTreeIter, newTreeIter);

			return entries.stream()
				// Consider old path of file as well?
				.map(DiffEntry::getNewPath)
				.map(ClassUtils::convertResourcePathToClassName)
				.filter(s -> s.contains(PACKAGE_PREFIX)) // DELETED will be filtered as new path will be /dev/null
				.filter(s -> s.endsWith(CLASS_FILE_SUFFIX))
				.map(s -> s.substring(s.lastIndexOf(PACKAGE_PREFIX) + PACKAGE_PREFIX.length() + 1,
					s.length() - CLASS_FILE_SUFFIX.length()))
				.collect(Collectors.toSet());
		}
	}
}
