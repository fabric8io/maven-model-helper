package io.fabric8.maven.merge;

import java.util.Arrays;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.merge.ModelMerger;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class SmartModelMergerTest {

    @Test
    public void should_copy_dependencies() {
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

        assertThat(target.getProfiles().contains(profile));
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

}
