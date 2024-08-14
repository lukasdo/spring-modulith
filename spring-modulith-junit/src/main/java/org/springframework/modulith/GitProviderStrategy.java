package org.springframework.modulith;

import java.io.IOException;
import java.util.Set;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.core.env.PropertyResolver;

public interface GitProviderStrategy {

	String CLASS_FILE_SUFFIX = ".java";
	String PACKAGE_PREFIX = "src.main.java";

	Set<FileChange> getModifiedFiles(PropertyResolver propertyResolver) throws IOException, GitAPIException;

}
