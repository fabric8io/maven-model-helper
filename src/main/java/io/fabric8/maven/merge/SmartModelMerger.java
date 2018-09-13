package io.fabric8.maven.merge;

import java.util.Objects;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.merge.ModelMerger;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class SmartModelMerger extends ModelMerger {

    @Override
    protected Object getDependencyKey(Dependency dependency) {
        return new DependencyKey(dependency);
    }

    // Maven's Dependency class does not implement equals/hashCode
    private class DependencyKey {

        private final Dependency dependency;

        private DependencyKey(Dependency dependency) {
            this.dependency = dependency;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DependencyKey that = (DependencyKey) o;
            return Objects.equals(dependency.getGroupId(), that.dependency.getGroupId()) &&
                    Objects.equals(dependency.getArtifactId(), that.dependency.getArtifactId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(dependency.getGroupId(), dependency.getArtifactId());
        }
    }
}
