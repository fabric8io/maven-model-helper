package io.fabric8.maven.merge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;
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
        return dependency.getGroupId() + ":" + dependency.getArtifactId();
    }

    @Override
    protected void mergeModelBase_Properties(ModelBase target, ModelBase source, boolean sourceDominant,
            Map<Object, Object> context) {
        Properties merged = new Properties();
        if (sourceDominant) {
            merged.putAll(target.getProperties());
            merged.putAll(source.getProperties());
        } else {
            merged.putAll(source.getProperties());
            merged.putAll(target.getProperties());
        }
        target.setProperties(merged);
        target.setLocation("properties", InputLocation.merge(target.getLocation("properties"),
                source.getLocation("properties"), sourceDominant));
    }

    @Override
    protected Object getProfileKey(Profile profile) {
        return profile.getId();
    }

    @Override
    protected void mergeModelBase_Modules(ModelBase target, ModelBase source, boolean sourceDominant,
            Map<Object, Object> context) {
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
}
