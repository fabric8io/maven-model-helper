package io.fabric8.maven;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.LineSeparator;
import org.jdom2.output.XMLOutputter;

/**
 * Output format for an XML document
 * <p>
 * This experimental class is a simple wrapper around JDOM2's {@link XMLOutputter} to provide a simple way to format XML
 */
public class XMLFormat {

    public static final XMLFormat DEFAULT = XMLFormat.builder().textMode(TextMode.PRESERVE).build();

    private final String indent;

    private final boolean insertLineBreakBetweenMajorSections;

    private final TextMode textMode;

    private final String lineSeparator;

    private XMLFormat(Builder builder) {
        this.indent = builder.indent;
        this.insertLineBreakBetweenMajorSections = builder.insertLineBreakBetweenMajorSections;
        this.textMode = builder.textMode;
        this.lineSeparator = builder.lineSeparator;
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

    public TextMode getTextMode() {
        return textMode;
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
            XMLOutputter xmlOutputter = createXmlOutputter();
            return xmlOutputter.outputString(document);
        } catch (JDOMException e) {
            throw new RuntimeException("Could not parse XML", e);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read XML", e);
        }
    }

    public void format(Reader reader, Writer writer) {
        Document document;
        try {
            document = new SAXBuilder().build(reader);
            XMLOutputter xmlOutputter = createXmlOutputter();
            xmlOutputter.output(document, writer);
        } catch (JDOMException e) {
            throw new RuntimeException("Could not parse XML", e);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read XML", e);
        }
    }

    XMLOutputter createXmlOutputter() {
        XMLOutputter xmlOutputter = new XMLOutputter();
        Format format = Format.getRawFormat();
        format.setIndent(indent);
        format.setLineSeparator(lineSeparator);
        format.setTextMode(Format.TextMode.valueOf(textMode.name()));
        if (insertLineBreakBetweenMajorSections) {
            // Insert line breaks between major sections
            xmlOutputter.setXMLOutputProcessor(new LineBreakProcessor());
        }
        xmlOutputter.setFormat(format);
        return xmlOutputter;
    }

    /**
     * Find the indentation used in the POM file
     *
     * @param pom the path to the POM file
     * @return the indentation used in the POM file
     */
    static String findIndentation(Path pom) {
        try (BufferedReader br = Files.newBufferedReader(pom)) {
            String line;
            while ((line = br.readLine()) != null) {
                int idx = line.indexOf("<");
                // We don't care about the first line or unindented lines
                if (idx > 0) {
                    return line.substring(0, idx);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read POM file: " + pom, e);
        }
        return XMLFormat.DEFAULT.getIndent();
    }

    public enum TextMode {
        /**
         * Mode for literal text preservation.
         */
        PRESERVE,

        /**
         * Mode for text trimming (left and right trim).
         */
        TRIM,

        /**
         * Mode for text normalization (left and right trim plus internal
         * whitespace is normalized to a single space.
         *
         * @see org.jdom2.Element#getTextNormalize
         */
        NORMALIZE,

        /**
         * Mode for text trimming of content consisting of nothing but
         * whitespace but otherwise not changing output.
         */
        TRIM_FULL_WHITE;
    }

    /**
     * Create a new builder
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a new builder with the given {@link XMLFormat}
     *
     * @param format the format
     * @return a new builder
     */
    public static Builder builder(XMLFormat format) {
        return new Builder()
                .indent(format.getIndent())
                .insertLineBreakBetweenMajorSections(format.isInsertLineBreakBetweenMajorSections())
                .textMode(format.getTextMode());
    }

    public static class Builder {
        private String indent = "  ";

        private boolean insertLineBreakBetweenMajorSections = false;

        private TextMode textMode = TextMode.TRIM;

        private String lineSeparator = LineSeparator.UNIX.value();

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

        public Builder textMode(TextMode textMode) {
            this.textMode = textMode;
            return this;
        }

        public Builder lineSeparator(String lineSeparator) {
            this.lineSeparator = lineSeparator;
            return this;
        }

        public XMLFormat build() {
            return new XMLFormat(this);
        }
    }
}
