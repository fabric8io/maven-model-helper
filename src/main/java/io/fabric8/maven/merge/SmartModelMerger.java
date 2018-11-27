package io.fabric8.maven.merge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Profile;
import org.apache.maven.model.merge.ModelMerger;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class SmartModelMerger extends ModelMerger {

    @Override
    protected Object getDependencyKey(Dependency dependency) {
        return new DependencyKey(dependency);
    }

    @Override
    protected Object getProfileKey(Profile profile) {
        return new ProfileKey(profile);
    }

    @Override
    protected void mergeModelBase_Modules(ModelBase target, ModelBase source, boolean sourceDominant, Map<Object, Object> context) {
        Set<String> set = new LinkedHashSet<>();
        set.addAll(source.getModules());
        set.addAll(target.getModules());
        target.setModules(new ArrayList<>(set));
    }

    @Override
    protected void mergeModel_Profiles(Model target, Model source, boolean sourceDominant,
                                       Map<Object, Object> context) {
        List<Profile> src = source.getProfiles();
        if (!src.isEmpty()) {
            List<Profile> tgt = target.getProfiles();
            Map<Object, Profile> merged = new LinkedHashMap<>((src.size() + tgt.size()) * 2);

            for (Profile element : tgt) {
                Object key = getProfileKey(element);
                merged.put(key, element);
            }

            for (Profile element : src) {
                Object key = getProfileKey(element);
                if (sourceDominant || !merged.containsKey(key)) {
                    Profile targetProfile = merged.get(key);
                    if (targetProfile != null) {
                        // Target Profile already exists. Merge contents
                        mergeProfile(targetProfile, element, sourceDominant, context);
                        merged.put(key, targetProfile);
                    } else {
                        merged.put(key, element);
                    }
                }
            }

            target.setProfiles(new ArrayList<>(merged.values()));
        }
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

    // Maven's Profile class does not implement equals/hashCode
    private class ProfileKey {
        private final Profile profile;

        private ProfileKey(Profile profile) {
            this.profile = profile;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProfileKey that = (ProfileKey) o;
            return Objects.equals(profile.getId(), that.profile.getId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(profile.getId());
        }
    }
}
