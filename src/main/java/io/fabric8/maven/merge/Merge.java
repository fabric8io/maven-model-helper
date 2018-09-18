package io.fabric8.maven.merge;

import java.nio.file.Path;
import java.nio.file.Paths;

import io.fabric8.maven.Maven;
import org.apache.maven.model.Model;
import org.apache.maven.model.merge.ModelMerger;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class Merge {

    /**
     * This class takes two parameters:
     * - target: the path to the target pom.xml
     * - source: the source pom.xml containing
     *
     * After execution, the target pom.xml should contain the merged contents from both poms
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("You need to specify the target and at least one source file");
            System.exit(1);
        }
        Path targetPath = Paths.get(args[0]).toAbsolutePath();
        Model targetModel = Maven.readModel(targetPath);
        boolean sourceDominant = Boolean.getBoolean("sourceDominant");
        ModelMerger merger = new SmartModelMerger();

        for (int i = 1; i < args.length; i++) {
            Path sourcePath = Paths.get(args[i]).toAbsolutePath();
            Model sourceModel = Maven.readModel(sourcePath);
            merger.merge(targetModel, sourceModel, sourceDominant, null);
        }
        Maven.writeModel(targetModel);
    }
}