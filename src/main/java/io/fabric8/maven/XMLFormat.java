package io.fabric8.maven;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * Output format for an XML document
 * <p>
 * This experimental class is a simple wrapper around JDOM2's {@link XMLOutputter} to provide a simple way to format XML
 */
public class XMLFormat {

    private final String indent;

    private final boolean insertLineBreakBetweenMajorSections;

    private XMLFormat(Builder builder) {
        this.indent = builder.indent;
        this.insertLineBreakBetweenMajorSections = builder.insertLineBreakBetweenMajorSections;
    }

    /**
     * @return the number of spaces to use for indentation
     */
    public String getIndent() {
        return indent;
    }

    /**
     * @return true if a line break should be inserted between major sections
     */
    public boolean isInsertLineBreakBetweenMajorSections() {
        return insertLineBreakBetweenMajorSections;
    }

    /**
     * Format the XML from the given reader
     *
     * @param reader the reader
     * @return the formatted XML
     */
    public String format(Reader reader) {
        Document document;
        try {
            document = new SAXBuilder().build(reader);
            XMLOutputter xmlOutputter = new XMLOutputter();
            Format format = Format.getPrettyFormat()
                    .setIndent(indent);
            if (insertLineBreakBetweenMajorSections) {
                // Insert line breaks between major sections
                xmlOutputter.setXMLOutputProcessor(new LineBreakProcessor());
            }
            xmlOutputter.setFormat(format);
            return xmlOutputter.outputString(document);
        } catch (JDOMException e) {
            throw new RuntimeException("Could not parse XML", e);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read XML", e);
        }

    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String indent = "  ";

        private boolean insertLineBreakBetweenMajorSections = false;

        Builder() {
        }

        public Builder indent(String indent) {
            this.indent = indent;
            return this;
        }

        public Builder insertLineBreakBetweenMajorSections() {
            return insertLineBreakBetweenMajorSections(true);
        }

        public Builder insertLineBreakBetweenMajorSections(boolean insertLineBreakBetweenMajorSections) {
            this.insertLineBreakBetweenMajorSections = insertLineBreakBetweenMajorSections;
            return this;
        }

        public XMLFormat build() {
            return new XMLFormat(this);
        }
    }
}
