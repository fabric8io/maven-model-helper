package io.fabric8.maven.cmd;

import java.nio.file.Path;
import java.nio.file.Paths;

import io.fabric8.maven.Maven;
import io.fabric8.maven.merge.SmartModelMerger;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.merge.ModelMerger;

import static java.util.Arrays.copyOfRange;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("You need to specify at least one command");
            System.exit(1);
        }

        switch (args[0]) {
            case "merge":
                if (args.length < 3) throw new AssertionError("Insufficient arguments:" + args.length);
                merge(args[1], copyOfRange(args, 2, args.length));
                break;
            case "replace-gav":
                if (args.length != 5) throw new AssertionError("Insufficient arguments:" + args.length);
                replaceGAV(args[1], args[2], args[3], args[4]);
                break;
            case "update-parent":
                if (args.length != 4) throw new AssertionError("Insufficient arguments:" + args.length);
                updateParentPom(args[1], args[2], args[3]);
            default:
                break;
        }

    }

    private static void merge(String target, String... sources) {
        Path targetPath = Paths.get(target).toAbsolutePath();
        Model targetModel = Maven.readModel(targetPath);
        boolean sourceDominant = Boolean.getBoolean("sourceDominant");
        ModelMerger merger = new SmartModelMerger();

        for (String source : sources) {
            Path sourcePath = Paths.get(source).toAbsolutePath();
            Model sourceModel = Maven.readModel(sourcePath);
            merger.merge(targetModel, sourceModel, sourceDominant, null);
        }
        Maven.writeModel(targetModel);
    }

    private static void replaceGAV(String target, String groupId, String artifactId, String version) {
        Path targetPath = Paths.get(target).toAbsolutePath();
        Model targetModel = Maven.readModel(targetPath);
        targetModel.setGroupId(groupId);
        targetModel.setArtifactId(artifactId);
        targetModel.setVersion(version);
        Maven.writeModel(targetModel);
    }

    private static void updateParentPom(String target, String groupId, String artifactId) {
        Path targetPath = Paths.get(target).toAbsolutePath();
        Model targetModel = Maven.readModel(targetPath);
        Parent parent = targetModel.getParent();
        if (parent == null) {
            parent = new Parent();
            targetModel.setParent(parent);
        }
        parent.setGroupId(groupId);
        parent.setArtifactId(artifactId);
        Maven.writeModel(targetModel);
    }

}
