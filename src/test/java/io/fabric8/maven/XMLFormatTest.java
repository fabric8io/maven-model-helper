package io.fabric8.maven;

import org.approvaltests.Approvals;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class XMLFormatTest {

    @Test
    public void should_ident() {
        XMLFormat xmlFormat = XMLFormat.builder().indent("    ").build();
        String result = xmlFormat.format(new StringReader("<root><child/></root>"));
        Approvals.verify(result);
    }

    @Test
    public void should_ident_and_insert_line_break_on_major_sections() {
        InputStream resourceAsStream = getClass().getResourceAsStream("no-spaces-pom.xml");
        XMLFormat xmlFormat = XMLFormat.builder().indent("    ").insertLineBreakBetweenMajorSections().build();
        String result = xmlFormat.format(new InputStreamReader(resourceAsStream));
        Approvals.verify(result);
    }

}
