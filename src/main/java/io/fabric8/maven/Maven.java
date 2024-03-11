package io.fabric8.maven;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;

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
            // https://github.com/fabric8-launcher/maven-model-helper/issues/43
            SortedProperties sortedProps = new SortedProperties();
            sortedProps.putAll(model.getProperties());
            model.setProperties(sortedProps);
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
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(rdr);
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
     * Shortcut to writeModel(model,model.getPomFile().toPath(),writer);
     *
     * @param model the model to write
     * @param writer the writer to write the model to
     */
    public static void writeModel(Model model, Writer writer) {
        writeModel(model, model.getPomFile() != null ? model.getPomFile().toPath() : null, () -> writer);
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
        if (pom == null || pom.toFile().length() == 0L) {
            // Initialize an empty XML
            try (Writer writer = writerSupplier.get()) {
                MavenXpp3Writer mavenXpp3Writer = new MavenXpp3Writer();
                mavenXpp3Writer.write(writer, model);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not write POM file: " + pom, e);
            }
        } else {
            Document document;
            try (InputStream is = Files.newInputStream(pom)) {
                document = new SAXBuilder().build(is);
            } catch (JDOMException e) {
                throw new RuntimeException("Could not parse POM file: " + pom, e);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read POM file: " + pom, e);
            }
            String indentation = findIndentation(pom);
            try (Writer writer = writerSupplier.get()) {
                MavenJDOMWriter mavenJDOMWriter = new MavenJDOMWriter();
                Format format = Format.getPrettyFormat();
                format.setIndent(indentation);
                format.setLineSeparator(System.lineSeparator());
                mavenJDOMWriter.write(model, document, writer, format);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not write to Writer", e);
            }
        }
    }

    /**
     * Find the indentation used in the POM file
     *
     * @param pom the path to the POM file
     * @return the indentation used in the POM file
     */
    private static String findIndentation(Path pom) {
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
        return "  ";
    }
}
