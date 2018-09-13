package io.fabric8.maven.cmd;

import java.nio.file.Path;
import java.nio.file.Paths;

import io.fabric8.maven.Maven;
import org.apache.maven.model.Model;
import org.apache.maven.model.merge.ModelMerger;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class Merge {

    public static void main(String[] args) {
        Path targetPath = Paths.get(args[0]).toAbsolutePath();
        Path sourcePath = Paths.get(args[1]).toAbsolutePath();
        Model targetModel = Maven.readModel(targetPath);
        Model sourceModel = Maven.readModel(sourcePath);

        ModelMerger merger = new ModelMerger();
        merger.merge(targetModel, sourceModel, false, null);

        Maven.writeModel(targetModel);
    }
}
