package org.springframework.modulith.test.execution;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Instance to store modified git files between Tests
 *
 * @author Lukas Dohmen
 */
public class ChangedFilesStore {

    private static ChangedFilesStore INSTANCE;
    private final Set<Class<?>> modifiedFiles;

    private ChangedFilesStore(Set<String> modifiedFiles) throws ClassNotFoundException {
        this.modifiedFiles = new HashSet<>();
        for (String file : modifiedFiles) {
            Class<?> aClass = ClassUtils.forName(file, null);
            this.modifiedFiles.add(aClass);
        }
    }

    public static ChangedFilesStore getInstance() throws IOException, ClassNotFoundException, GitAPIException {
        if (INSTANCE == null) {
            GitChangedFiles localChanges = new LocalChanges();
            INSTANCE = new ChangedFilesStore(localChanges.getModifiedFiles());
        }

        return INSTANCE;
    }

    public Set<Class<?>> getModifiedFiles() {
        return modifiedFiles;
    }
}
