package io.fabric8.maven.cmd;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class MergeTest {

    @Test
    public void should_merge_both_poms() throws Exception {
        Path target = Paths.get(getClass().getResource("target-pom.xml").toURI());
        Path source = Paths.get(getClass().getResource("source-pom.xml").toURI());
        Path result = Paths.get(getClass().getResource("result-pom.xml").toURI());
        String[] args ={
                target.toString(),
                source.toString()
        };
        Merge.main(args);
        assertThat(target).hasSameContentAs(result);
    }
}
