package io.fabric8.maven;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.merge.ModelMerger;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class ModelMergerTest {

    @Test
    public void should_copy_dependencies() {
        ModelMerger merger = new ModelMerger();
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
}
