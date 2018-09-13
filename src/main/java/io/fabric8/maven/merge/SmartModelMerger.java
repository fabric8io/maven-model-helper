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
        return new DependencyKey(dependency.getGroupId(), dependency.getArtifactId());
    }

    // Maven's Dependency class does not implement equals/hashCode
    private class DependencyKey {

        private final String groupId;

        private final String artifactId;

        private DependencyKey(String groupId, String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DependencyKey that = (DependencyKey) o;
            return Objects.equals(groupId, that.groupId) &&
                    Objects.equals(artifactId, that.artifactId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(groupId, artifactId);
        }
    }
}
