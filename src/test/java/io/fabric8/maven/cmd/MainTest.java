package io.fabric8.maven.cmd;

import java.nio.file.Path;
import java.nio.file.Paths;

import io.fabric8.maven.Maven;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
class MainTest {

    @Test
    void should_merge_both_poms() throws Exception {
        Path target = Paths.get(getClass().getResource("target-pom.xml").toURI());
        Path source = Paths.get(getClass().getResource("source-pom.xml").toURI());
        Path result = Paths.get(getClass().getResource("result-pom.xml").toURI());
        String[] args = {
                "merge",
                target.toString(),
                source.toString()
        };
        Main.main(args);
        assertThat(target).hasSameContentAs(result);
    }

    @Test
    void should_change_project_metadata(@TempDir Path tempDir) {
        Path target = tempDir.resolve("foo.pom");
        Maven.writeModel(new Model(), target);
        String[] args = {
                "update-metadata",
                target.toString(),
                "my-name",
                "my-description"
        };
        Main.main(args);
        Model model = Maven.readModel(target);
        assertThat(model.getName()).isEqualTo("my-name");
        assertThat(model.getDescription()).isEqualTo("my-description");
    }
}
