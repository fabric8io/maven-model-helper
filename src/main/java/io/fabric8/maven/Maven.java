package io.fabric8.maven;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3ReaderEx;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public final class Maven {

    private Maven() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Create a new Maven {@link Model}
     *
     * @return a new {@link Model}
     */
    public static Model newModel() {
        Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setProperties(new SortedProperties());
        return model;
    }

    /**
     * A String version that makes it easier to be called from other languages
     *
     * @see #readModel(Path)
     */
    public static Model readModel(String path) {
        return readModel(Paths.get(path));
    }

    /**
     * Read the {@link Path} as a {@link Model}
     *
     * @param pom a path to a pom.xml file
     * @return the maven {@link Model}
     */
    public static Model readModel(Path pom) {
        try (BufferedReader br = Files.newBufferedReader(pom)) {
            Model model = readModel(br);
            model.setPomFile(pom.toFile());
            return model;
        } catch (IOException io) {
            throw new UncheckedIOException("Error while reading pom.xml", io);
        }
    }

    /**
     * Read the {@link Path} as a {@link Model}
     *
     * @param rdr a Reader on the contents of a pom file
     * @return the maven {@link Model}
     */
    public static Model readModel(Reader rdr) {
        try (Reader reader = rdr) {
            MavenXpp3ReaderEx mavenXpp3Reader = new MavenXpp3ReaderEx();
            Model model = mavenXpp3Reader.read(reader, true, null);
            // https://github.com/fabric8-launcher/maven-model-helper/issues/44
            SortedProperties sortedProps = new SortedProperties();
            sortedProps.putAll(model.getProperties());
            model.setProperties(sortedProps);
            return model;
        } catch (IOException io) {
            throw new UncheckedIOException("Error while reading pom.xml", io);
        } catch (XmlPullParserException e) {
            throw new RuntimeException("Error while parsing pom.xml", e);
        }
    }

    /**
     * Read the {@link InputStream} as a {@link Model}
     *
     * @param inputStream an input stream of a pom.xml file
     * @return the maven {@link Model}
     */
    public static Model readModel(InputStream inputStream) {
        try (XmlStreamReader xmlStreamReader = new XmlStreamReader(inputStream)) {
            return readModel(xmlStreamReader);
        } catch (IOException io) {
            throw new UncheckedIOException("Error while reading stream", io);
        }
    }

    /**
     * Shortcut to writeModel(model,model.getPomFile().toPath());
     *
     * @param model the model to write
     */
    public static void writeModel(Model model) {
        writeModel(model, model.getPomFile().toPath());
    }

    /**
     * Write the Model back to the provided {@link Path}
     *
     * @param model the model to write
     * @param pom the path to the POM file
     */
    public static void writeModel(Model model, Path pom) {
        writeModel(model, pom, () -> {
            try {
                return Files.newBufferedWriter(pom);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not write to Writer", e);
            }
        });
    }

    /**
     * Write the Model back to the provided {@link Path} using the specified {@link XMLFormat}
     *
     * @param model the model to write
     * @param pom the path to the POM file
     * @param format the XML format to use
     */
    public static void writeModel(Model model, Path pom, XMLFormat format) {
        writeModel(model, pom, () -> {
            try {
                return Files.newBufferedWriter(pom);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not write to Writer", e);
            }
        }, format);
    }

    /**
     * Shortcut to writeModel(model,model.getPomFile().toPath(),writer);
     *
     * @param model the model to write
     * @param writer the writer to write the model to
     */
    public static void writeModel(Model model, Writer writer) {
        writeModel(model, model.getPomFile() != null ? model.getPomFile().toPath() : null, () -> writer);
    }

    /**
     * Shortcut to writeModel(model,model.getPomFile().toPath(),writer, format);
     *
     * @param model the model to write
     * @param writer the writer to write the model to
     * @param format the XML format to use
     */
    public static void writeModel(Model model, Writer writer, XMLFormat format) {
        writeModel(model, model.getPomFile() != null ? model.getPomFile().toPath() : null, () -> writer, format);
    }

    /**
     * Write the Model to the {@link Writer} using the provided {@link Path} as a reference
     *
     * @param model the model to write
     * @param pom the path to the POM file
     * @param writer the writer to write the model to
     */
    public static void writeModel(Model model, Path pom, Writer writer) {
        writeModel(model, pom, () -> writer);
    }

    /**
     * Write the Model to the {@link Writer} using the provided {@link Path} as a reference
     *
     * @param model the model to write
     * @param pom the path to the POM file
     * @param writerSupplier the writer supplier to write the model to
     */
    public static void writeModel(Model model, Path pom, Supplier<Writer> writerSupplier) {
        writeModel(model, pom, writerSupplier, null);
    }

    /**
     * Write the Model to the {@link Writer} using the provided {@link Path} as a reference
     *
     * @param model the model to write
     * @param pom the path to the POM file
     * @param writerSupplier the writer supplier to write the model to
     */
    public static void writeModel(Model model, Path pom, Supplier<Writer> writerSupplier, XMLFormat format) {
        if (pom == null || pom.toFile().length() == 0L) {
            // Initialize an empty XML
            try (Writer writer = writerSupplier.get()) {
                if (format != null) {
                    // Format specified, write to a String first
                    StringWriter sw = new StringWriter();
                    MavenXpp3Writer mavenXpp3Writer = new MavenXpp3Writer();
                    mavenXpp3Writer.write(sw, model);
                    format.format(new StringReader(sw.toString()), writer);
                } else {
                    // No format specified, keep original behavior
                    MavenXpp3Writer mavenXpp3Writer = new MavenXpp3Writer();
                    mavenXpp3Writer.write(writer, model);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Could not write POM file: " + pom, e);
            }
        } else {
            Document document;
            try {
                document = new SAXBuilder().build(pom.toFile());
            } catch (JDOMException e) {
                throw new RuntimeException("Could not parse POM file: " + pom, e);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read POM file: " + pom, e);
            }
            String indentation = (format != null && format.getIndent() != null) ? format.getIndent()
                    : XMLFormat.findIndentation(pom);
            try (Writer writer = writerSupplier.get()) {
                MavenJDOMWriter mavenJDOMWriter = new MavenJDOMWriter(indentation);
                XMLOutputter xmlOutputter = format != null ? format.createXmlOutputter()
                        : XMLFormat.DEFAULT.createXmlOutputter();
                mavenJDOMWriter.write(model, document, writer, xmlOutputter);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not write to Writer", e);
            }
        }
    }
}
