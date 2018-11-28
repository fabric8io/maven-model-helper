package io.fabric8.maven.cmd;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.fabric8.maven.Maven;
import org.apache.maven.model.Model;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class MainTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void should_merge_both_poms() throws Exception {
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
    public void should_change_project_metadata() throws Exception {
        Path target = temporaryFolder.newFile().toPath();
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
