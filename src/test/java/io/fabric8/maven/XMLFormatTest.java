package io.fabric8.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.approvaltests.Approvals;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class XMLFormatTest {

    @Test
    public void should_ident() {
        XMLFormat xmlFormat = XMLFormat.builder().indent("    ").textMode(XMLFormat.TextMode.TRIM).build();
        String result = xmlFormat.format(new StringReader("<root><child/></root>"));
        Approvals.verify(result);
    }

    @Test
    public void should_ident_and_insert_line_break_on_major_sections() {
        InputStream resourceAsStream = getClass().getResourceAsStream("no-spaces-pom.xml");
        assertThat(resourceAsStream).isNotNull();
        XMLFormat xmlFormat = XMLFormat.builder().indent("    ").textMode(XMLFormat.TextMode.TRIM)
                .insertLineBreakBetweenMajorSections().build();
        String result = xmlFormat.format(new InputStreamReader(resourceAsStream));
        Approvals.verify(result);
    }

    @Test
    public void find_indentation_should_return_4_blanks() throws Exception {
        URL resource = getClass().getResource("spaces-pom.xml");
        assertThat(resource).isNotNull();
        Path pom = Paths.get(resource.toURI());
        assertThat(XMLFormat.findIndentation(pom)).isEqualTo("    ");
    }

}
