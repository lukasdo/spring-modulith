package org.springframework.modulith;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.Change.JavaClassChange;
import org.springframework.modulith.Change.JavaTestClassChange;
import org.springframework.modulith.Change.OtherFileChange;

class ChangesTest {
	@Test
	void shouldInterpredModifiedFilePathsCorrectly() {
		// given
		Set<ModifiedFilePath> modifiedFilePaths = Set.of(
			new ModifiedFilePath("spring-modulith-junit/src/main/java/org/springframework/modulith/Changes.java"),
			new ModifiedFilePath("spring-modulith-junit/src/test/java/org/springframework/modulith/ChangesTest.java"),
			new ModifiedFilePath(
				"spring-modulith-junit/src/main/resources/META-INF/additional-spring-configuration-metadata.json"));

		// when
		Set<Change> result = Changes.toChanges(modifiedFilePaths);

		// then
		assertThat(result).containsExactlyInAnyOrder(
			new JavaClassChange("org.springframework.modulith.Changes"),
			new JavaTestClassChange("org.springframework.modulith.ChangesTest"),
			new OtherFileChange("spring-modulith-junit/src/main/resources/META-INF/additional-spring-configuration-metadata.json")
		);
	}
}
