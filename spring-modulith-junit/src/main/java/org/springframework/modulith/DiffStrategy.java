package org.springframework.modulith;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation to get changes between HEAD and a complete or abbreviated SHA-1
 */
public class DiffStrategy implements GitProviderStrategy {
    private String commitIdToCompareTo = "e2228a3c";


    @Override
    public Set<String> getModifiedFiles() throws IOException, GitAPIException {
        try (var gitDir = new FileRepositoryBuilder().findGitDir().build()) {
            Git git = new Git(gitDir);

            String compareTo = "%s^{tree}".formatted(commitIdToCompareTo);
            if (compareTo == null || compareTo.isEmpty()) {
                compareTo = "HEAD~1^{tree}";
            }
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

            for (DiffEntry entry : entries) {
                System.out.println(entry.getChangeType());
            }
            return entries.stream()
                    // Consider old path of file as well?
                    .map(DiffEntry::getNewPath)
                    .map(ClassUtils::convertResourcePathToClassName)
                    .filter(s -> s.contains(PACKAGE_PREFIX)) // DELETED will be filtered as new path will be /dev/null
                    .filter(s -> s.endsWith(CLASS_FILE_SUFFIX))
                    .map(s -> s.substring(s.lastIndexOf(PACKAGE_PREFIX) + PACKAGE_PREFIX.length() + 1, s.length() - CLASS_FILE_SUFFIX.length()))
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
