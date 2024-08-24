package org.springframework.modulith;

import java.io.IOException;
import java.util.Set;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.core.env.PropertyResolver;
import org.springframework.lang.NonNull;

public interface FileChangeDetector {

	String CLASS_FILE_SUFFIX = ".java";
	String PACKAGE_PREFIX = "src.main.java";

	Set<FileChange> getModifiedFiles(@NonNull PropertyResolver propertyResolver) throws IOException, GitAPIException;

}
