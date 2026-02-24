package io.fabric8.maven.merge;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.merge.ModelMerger;
import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;

import io.fabric8.maven.Maven;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
class SmartModelMergerTest {

    @Test
    void should_copy_dependencies() {
        ModelMerger merger = new SmartModelMerger();
        Model source = new Model();
        source.setGroupId("org.example");
        source.setArtifactId("example");
        source.setVersion("1.0");

        Dependency dependency = new Dependency();
        dependency.setGroupId("foo");
        dependency.setArtifactId("bar");
        source.addDependency(dependency);

        Model target = new Model();
        target.setGroupId("org.example");
        target.setArtifactId("example");
        target.setVersion("2.0");

        merger.merge(target, source, false, null);

        assertThat(target.getDependencies()).contains(dependency);
    }

    @Test
    public void should_copy_profiles() {
        ModelMerger merger = new SmartModelMerger();
        Model source = new Model();
        source.setGroupId("org.example");
        source.setArtifactId("example");
        source.setVersion("1.0");

        Profile profile = new Profile();
        profile.setId("foo");
        profile.setModules(Arrays.asList("A", "B", "C"));
        source.addProfile(profile);

        Model target = new Model();
        target.setGroupId("org.example");
        target.setArtifactId("example");
        target.setVersion("2.0");

        merger.merge(target, source, false, null);

        assertThat(target.getProfiles()).contains(profile);
    }

    @Test
    public void should_merge_profiles() {
        ModelMerger merger = new SmartModelMerger();
        Model source = new Model();
        source.setGroupId("org.example");
        source.setArtifactId("example");
        source.setVersion("1.0");

        Profile profile = new Profile();
        profile.setId("foo");
        profile.setModules(Arrays.asList("A", "B", "C"));
        source.addProfile(profile);

        Model target = new Model();
        target.setGroupId("org.example");
        target.setArtifactId("example2");
        target.setVersion("2.0");

        Profile profile2 = new Profile();
        profile2.setId("foo");
        profile2.setModules(Arrays.asList("D", "E", "F"));
        target.addProfile(profile2);
        merger.merge(target, source, true, null);

        assertThat(target.getProfiles()).hasSize(1);
        assertThat(target.getProfiles().get(0).getModules()).containsExactly("A", "B", "C", "D", "E", "F");
    }

    @Test
    void should_keep_target_indent_2() throws URISyntaxException {
        ModelMerger merger = new SmartModelMerger();
        final Model source = Maven.readModel(Paths.get(getClass().getResource("indent/source-pom.xml").toURI()));
        final Path targetFile = Paths.get(getClass().getResource("indent/pom-2.xml").toURI());
        final Model target = Maven.readModel(targetFile);
        merger.merge(target, source, false, null);
        Maven.writeModel(target, targetFile);
        Approvals.verify(targetFile.toFile());
    }

    @Test
    void should_keep_target_indent_4() throws URISyntaxException {
        ModelMerger merger = new SmartModelMerger();
        final Model source = Maven.readModel(Paths.get(getClass().getResource("indent/source-pom.xml").toURI()));
        final Path targetFile = Paths.get(getClass().getResource("indent/pom-4.xml").toURI());
        final Model target = Maven.readModel(targetFile);
        merger.merge(target, source, true, null);
        Maven.writeModel(target, targetFile);
        Approvals.verify(targetFile.toFile());
    }

    @Test
    void should_preserve_packaging() {
        String basePom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project>\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>org.acme</groupId>\n" +
                "    <artifactId>my-app</artifactId>\n" +
                "    <version>1.0.0-SNAPSHOT</version>\n" +
                "    <packaging>quarkus</packaging>\n" +
                "</project>\n";

        String extensionPom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project>\n" +
                "    <properties>\n" +
                "        <property-from-extension>value</property-from-extension>\n" +
                "    </properties>\n" +
                "</project>\n";

        ModelMerger merger = new SmartModelMerger();
        final Model source = Maven.readModel(new StringReader(basePom));
        final Model target = Maven.readModel(new StringReader(extensionPom));
        merger.merge(target, source, true, null);
        StringWriter writer = new StringWriter();
        Maven.writeModel(target, writer);
        Approvals.verify(writer.toString());
    }
}
