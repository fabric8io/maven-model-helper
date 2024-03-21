package io.fabric8.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;
import java.util.stream.IntStream;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.approvaltests.Approvals;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xmlunit.assertj.XmlAssert;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MavenTest {

    @Test
    void should_read_model() {
        Path basePom = Paths.get("pom.xml");
        Model model = Maven.readModel(basePom);
        Assertions.assertThat(model).isNotNull();
        assertThat(model.getPomFile().getAbsolutePath()).isEqualTo(basePom.toAbsolutePath().toString());
        assertThat(model.getParent().getGroupId()).isEqualTo("org.jboss");
        assertThat(model.getArtifactId()).isEqualTo("maven-model-helper");
    }

    @Test
    void should_read_model_using_reader() {
        Model model = Maven.readModel(
                new StringReader("<project><groupId>org.jboss</groupId><artifactId>maven-model-helper</artifactId></project>"));
        Assertions.assertThat(model).isNotNull();
        assertThat(model.getGroupId()).isEqualTo("org.jboss");
        assertThat(model.getArtifactId()).isEqualTo("maven-model-helper");
    }

    @Test
    void should_read_model_using_inputstream() {
        Model model = Maven.readModel(
                new ByteArrayInputStream(
                        "<project><groupId>org.jboss</groupId><artifactId>maven-model-helper</artifactId></project>"
                                .getBytes()));
        Assertions.assertThat(model).isNotNull();
        assertThat(model.getGroupId()).isEqualTo("org.jboss");
        assertThat(model.getArtifactId()).isEqualTo("maven-model-helper");
    }

    @Test
    void should_fail_read_model_using_bogus_inputstream() {
        assertThatRuntimeException().isThrownBy(() -> Maven.readModel(new ByteArrayInputStream("<projectt>".getBytes())));
    }

    @Test
    void should_read_model_string() {
        Path basePom = Paths.get("pom.xml");
        Model model = Maven.readModel(basePom);
        Assertions.assertThat(model).isNotNull();
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
        Approvals.verify(pom);
    }

    @Test
    void should_write_model_formatted() {
        Model model = new Model();
        model.setGroupId("org.example");
        model.setArtifactId("example");
        model.setVersion("1.0");
        StringWriter sw = new StringWriter();
        Maven.writeModel(model, sw, XMLFormat.builder().indent("    ").build());
        Approvals.verify(sw.toString());
    }

    @Test
    void should_write_existing_model_formatted() throws Exception {
        Path basePom = Paths.get(getClass().getResource("spaces-pom.xml").toURI());
        Model model = Maven.readModel(basePom);
        StringWriter sw = new StringWriter();
        Maven.writeModel(model, basePom, () -> sw, XMLFormat.builder()
                .indent("    ")
                .insertLineBreakBetweenMajorSections()
                .build());
        Approvals.verify(sw.toString());
    }

    @Test
    void should_write_model_with_sorted_properties(@TempDir Path tempDir) throws IOException {
        Path basePom = Paths.get("pom.xml");
        Model model = Maven.readModel(basePom);

        Properties properties = model.getProperties();
        assertThat(properties).isInstanceOf(SortedProperties.class);

        properties.put("c", "three");
        properties.put("a", "one");
        properties.put("b", "two");

        // Write pom
        Path pom = tempDir.resolve("temp-pom.xml");
        Maven.writeModel(model, pom);
        assertThat(Files.readAllLines(pom).stream().map(String::trim))
                .containsSequence("<a>one</a>", "<b>two</b>", "<c>three</c>");
    }

    @Test
    void should_write_model_with_sorted_properties_using_reader(@TempDir Path tempDir) throws IOException {
        Path basePom = Paths.get("pom.xml");
        Model model = Maven.readModel(basePom);

        Properties properties = model.getProperties();
        assertThat(properties).isInstanceOf(SortedProperties.class);

        properties.put("c", "three");
        properties.put("a", "one");
        properties.put("b", "two");

        // Write pom
        Path pom = tempDir.resolve("temp-pom.xml");
        Maven.writeModel(model, pom);
        assertThat(Files.readAllLines(pom).stream().map(String::trim))
                .containsSequence("<a>one</a>", "<b>two</b>", "<c>three</c>");
    }

    @Test
    void should_preserve_parent_relative_path(@TempDir Path tempDir) throws Exception {
        URL resource = getClass().getResource("parent/parent-pom.xml");
        Path parentPom = Paths.get(resource.toURI());
        Path newPath = tempDir.resolve("new-pom.xml");
        Model model = Maven.readModel(parentPom);
        assertThat(model.getParent().getRelativePath()).isNotNull();

        Maven.writeModel(model, newPath);
        XmlAssert.assertThat(newPath)
                .withNamespaceContext(Collections.singletonMap("maven", "http://maven.apache.org/POM/4.0.0"))
                .valueByXPath("//maven:project/maven:parent/maven:relativePath")
                .isEqualTo("../../pom.xml");
    }

    @Test
    void should_save_full_pom_on_new_file(@TempDir Path tempDir) throws Exception {
        URL resource = getClass().getResource("full-pom.xml");
        Path basePom = Paths.get(resource.toURI());
        Model model = Maven.readModel(basePom);

        Path newPom = tempDir.resolve("new-pom.xml");
        Maven.writeModel(model, newPom);

        Approvals.verify(Files.readString(newPom));
    }

    @Test
    void should_save_full_pom_on_updated_full_file(@TempDir Path tempDir) throws Exception {
        URL resource = getClass().getResource("full-pom.xml");
        Path basePom = Paths.get(resource.toURI());
        Model model = Maven.readModel(basePom);

        Path updatedPom = tempDir.resolve("updated-full-pom.xml");
        Files.copy(basePom, updatedPom);
        Maven.writeModel(model, updatedPom);

        Approvals.verify(Files.readString(updatedPom));
    }

    @Test
    void should_save_full_pom_on_updated_minimal_file(@TempDir Path tempDir) throws Exception {
        URL resource = getClass().getResource("full-pom.xml");
        Path basePom = Paths.get(resource.toURI());
        Model model = Maven.readModel(basePom);

        Path updatedPom = tempDir.resolve("updated-minimal-pom.xml");
        Files.writeString(updatedPom, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\" />");
        Maven.writeModel(model, updatedPom);

        Approvals.verify(Files.readString(updatedPom));
    }

    @Test
    void should_write_scm_tag(@TempDir Path tempDir) throws Exception {
        Path pomXml = tempDir.resolve("pom.xml");
        Model newModel = Maven.newModel();
        Scm scm = new Scm();
        scm.setUrl("https://github.com/fabric8-launcher/maven-model-helper.git");
        scm.setDeveloperConnection("scm:git:git@github.com:fabric8-launcher/maven-model-helper.git");
        scm.setTag("HEAD");
        newModel.setScm(scm);
        Maven.writeModel(newModel, pomXml);
        XmlAssert.assertThat(pomXml)
                .withNamespaceContext(Collections.singletonMap("maven", "http://maven.apache.org/POM/4.0.0"))
                .valueByXPath("//maven:project/maven:scm/maven:url")
                .isEqualTo(scm.getUrl());
        XmlAssert.assertThat(pomXml)
                .withNamespaceContext(Collections.singletonMap("maven", "http://maven.apache.org/POM/4.0.0"))
                .valueByXPath("//maven:project/maven:scm/maven:developerConnection")
                .isEqualTo(scm.getDeveloperConnection());
        // HEAD is not written
        XmlAssert.assertThat(pomXml)
                .withNamespaceContext(Collections.singletonMap("maven", "http://maven.apache.org/POM/4.0.0"))
                .valueByXPath("//maven:project/maven:scm/maven:tag")
                .isEmpty();
    }

    @Test
    void should_respect_insertion_order(@TempDir Path tempDir) throws Exception {
        URL resource = getClass().getResource("parent/parent-pom.xml");
        Path basePom = Paths.get(resource.toURI());
        Model model = Maven.readModel(basePom);

        Path updatedPom = tempDir.resolve("updated-pom.xml");
        Files.copy(basePom, updatedPom);

        Dependency dependency = model.getDependencies().stream()
                .filter(d -> d.getArtifactId().equals("quarkus-junit5-internal")).findFirst().orElseThrow();
        dependency.setVersion("1.0.0");
        dependency.setOptional("true");
        Maven.writeModel(model, updatedPom);

        assertThat(Files.readString(updatedPom))
                .contains("<dependency>\n" +
                        "      <groupId>io.quarkus</groupId>\n" +
                        "      <artifactId>quarkus-junit5-internal</artifactId>\n" +
                        "      <version>1.0.0</version>\n" +
                        "      <scope>test</scope>\n" +
                        "      <optional>true</optional>\n" +
                        "    </dependency>");
    }

    @Test
    void should_write_model_in_dependencies_order(@TempDir Path tempDir) throws Exception {
        URL resource = getClass().getResource("parent/parent-pom.xml");
        Path parentPom = Paths.get(resource.toURI());
        Model model = Maven.readModel(parentPom);

        Dependency dep = new Dependency();
        dep.setGroupId("org.example");
        dep.setArtifactId("example");
        dep.setVersion("1.0");
        int idx = IntStream.range(0, model.getDependencies().size())
                .filter(i -> "test".equals(model.getDependencies().get(i).getScope()))
                .findFirst()
                .orElse(model.getDependencies().size()); // default to size if no "test" scope is found
        model.getDependencies().add(idx, dep);
        Path updatedPom = tempDir.resolve("updated-pom.xml");
        Files.copy(parentPom, updatedPom);
        Maven.writeModel(model, updatedPom);
        Approvals.verify(Files.readString(updatedPom));
    }

    @Test
    void should_write_model_to_writer() throws Exception {
        URL resource = getClass().getResource("parent/parent-pom.xml");
        Path parentPom = Paths.get(resource.toURI());
        Model model = Maven.readModel(parentPom);

        Dependency dep = new Dependency();
        dep.setGroupId("org.example");
        dep.setArtifactId("example");
        dep.setVersion("1.0");

        model.getDependencies().add(dep);

        StringWriter sw = new StringWriter();
        Maven.writeModel(model, sw);
        Approvals.verify(sw.toString());
    }

    @Test
    void should_keep_spaces_and_new_lines() throws Exception {
        URL resource = getClass().getResource("spaces-pom.xml");
        Path parentPom = Paths.get(resource.toURI());
        Model model = Maven.readModel(parentPom);

        Dependency dep = new Dependency();
        dep.setGroupId("org.example");
        dep.setArtifactId("example");
        dep.setVersion("1.0");

        model.getDependencies().add(dep);

        StringWriter sw = new StringWriter();
        Maven.writeModel(model, sw);
        Approvals.verify(sw.toString());
    }

    @Test
    void should_keep_attributes() throws Exception {
        Path pom = Paths.get(getClass().getResource("attributes.xml").toURI());
        Model model = Maven.readModel(pom);
        StringWriter sw = new StringWriter();
        Maven.writeModel(model, sw);
        Approvals.verify(sw.toString());
    }

    @Test
    void should_write_attributes() throws Exception {
        Path pom = Paths.get(getClass().getResource("spaces-pom.xml").toURI());
        Model model = Maven.readModel(pom);
        model.setChildProjectUrlInheritAppendPath(false);
        StringWriter sw = new StringWriter();
        Maven.writeModel(model, sw);
        Approvals.verify(sw.toString());
    }

    @Test
    void should_use_line_separator() {
        Model model = Maven.newModel();
        StringWriter sw = new StringWriter();
        Maven.writeModel(model, sw, XMLFormat.builder().lineSeparator("\n\n").build());
        Approvals.verify(sw.toString());
    }

    @Test
    void should_support_no_line_separator() {
        Model model = Maven.newModel();
        StringWriter sw = new StringWriter();
        Maven.writeModel(model, sw, XMLFormat.builder().lineSeparator(null).build());
        Approvals.verify(sw.toString());
    }

    @Test
    void should_not_add_extra_line_break() throws Exception {
        Path pom = Paths.get(getClass().getResource("extra-line-pom.xml").toURI());
        Model model = Maven.readModel(pom);
        DependencyManagement dependencyManagement = model.getDependencyManagement();
        Dependency dep = new Dependency();
        dep.setGroupId("org.springframework.boot");
        dep.setArtifactId("spring-boot-dependencies");
        dep.setVersion("${spring-boot.version}");
        dep.setScope("import");
        dependencyManagement.addDependency(dep);
        StringWriter sw = new StringWriter();
        Maven.writeModel(model, sw);
        Approvals.verify(sw.toString());
    }

}
