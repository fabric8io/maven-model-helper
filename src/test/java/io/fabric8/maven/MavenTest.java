package io.fabric8.maven;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
class MavenTest {

    @Test
    void should_read_model() {
        Path basePom = Paths.get("pom.xml");
        Model model = Maven.readModel(basePom);
        assertThat(model).isNotNull();
        assertThat(model.getPomFile().getAbsolutePath()).isEqualTo(basePom.toAbsolutePath().toString());
        assertThat(model.getParent().getGroupId()).isEqualTo("org.jboss");
        assertThat(model.getArtifactId()).isEqualTo("maven-model-helper");
    }

    @Test
    void should_read_model_using_reader() {
        Model model = Maven.readModel(new StringReader("<project><groupId>org.jboss</groupId><artifactId>maven-model-helper</artifactId></project>"));
        assertThat(model).isNotNull();
        assertThat(model.getGroupId()).isEqualTo("org.jboss");
        assertThat(model.getArtifactId()).isEqualTo("maven-model-helper");
    }


    @Test
    void should_read_model_string() {
        Path basePom = Paths.get("pom.xml");
        Model model = Maven.readModel(basePom.toAbsolutePath().toString());
        assertThat(model).isNotNull();
        assertThat(model.getPomFile().getAbsolutePath()).isEqualTo(basePom.toAbsolutePath().toString());
        assertThat(model.getParent().getGroupId()).isEqualTo("org.jboss");
        assertThat(model.getArtifactId()).isEqualTo("maven-model-helper");
    }

    @Test
    void should_write_model(@TempDir Path tempDir) throws IOException {
        File pom = tempDir.resolve("temp-pom.xml").toFile();
        Model model = new Model();
        model.setPomFile(pom);
        model.setGroupId("org.example");
        model.setArtifactId("example");
        model.setVersion("1.0");
        Maven.writeModel(model);
        assertThat(Files.readAllLines(pom.toPath()).stream().map(String::trim))
                .contains("<groupId>org.example</groupId>", "<artifactId>example</artifactId>", "<version>1.0</version>");
    }

}
