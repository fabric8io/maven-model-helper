package io.fabric8.maven;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationFile;
import org.apache.maven.model.ActivationOS;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.Developer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Extension;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.model.Notifier;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.model.Resource;
import org.apache.maven.model.Scm;
import org.apache.maven.model.Site;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jdom2.Content;
import org.jdom2.DefaultJDOMFactory;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Text;
import org.jdom2.output.XMLOutputter;

/**
 * Writes a Maven Model to a JDOM Document
 * <p>
 * Based on the implementation in the <a href=
 * "https://github.com/apache/maven-archetype/blob/master/archetype-common/src/main/java/org/apache/maven/archetype/common/MavenJDOMWriter.java">Maven
 * Archetype Plugin</a>
 */
final class MavenJDOMWriter {
    // --------------------------/
    // - Class/Member Variables -/
    // --------------------------/

    /**
     * Field factory.
     */
    private final DefaultJDOMFactory factory;

    /**
     * Field lineSeparator.
     */
    private final String lineSeparator;

    /**
     * Field indentation
     */
    private final String indentation;

    // ----------------/
    // - Constructors -/
    // ----------------/

    public MavenJDOMWriter(String indentation) {
        factory = new DefaultJDOMFactory();
        lineSeparator = "\n";
        this.indentation = indentation;
    }

    /**
     * Method write.
     *
     * @param project The Model to write
     * @param document The Document to write to
     * @param writer The Writer to write to
     * @param xmlOutputter The {@link XMLOutputter} to use for output
     */
    public void write(Model project, Document document, Writer writer, XMLOutputter xmlOutputter) throws java.io.IOException {
        updateModel(project, Counter.initialCounter(), document.getRootElement());
        xmlOutputter.output(document, writer);
    }

    // -----------/
    // - Methods -/
    // -----------/

    /**
     * Method findAndReplaceProperties.
     *
     * @param counter The counter
     * @param parent The parent element
     * @param name The name of the element
     * @param props The properties to add
     */
    private void findAndReplaceProperties(Counter counter, Element parent, String name, Properties props) {
        boolean shouldExist = (props != null) && !props.isEmpty();
        Element element = updateElement(counter, parent, name, shouldExist);
        if (shouldExist) {
            var it = props.keySet().iterator();
            Counter innerCounter = counter.newNextDepthLevelCounter();
            while (it.hasNext()) {
                String key = it.next().toString();
                if ((element.getChild(key, parent.getNamespace()) == null)) {
                    //If it is a new entry, append instead of messing with the existing contents
                    Element newProperty = factory.element(key, parent.getNamespace()).setText(props.getProperty(key));
                    // Move pointer to last element
                    innerCounter.setCurrentIndex(element.getContentSize());
                    insertAtPreferredLocation(element, newProperty, innerCounter);
                } else {
                    findAndReplaceSimpleElement(innerCounter, element, key, props.getProperty(key), null, false);
                }
            }
            // Remove properties that no longer exist
            var itElem = element.getChildren().iterator();
            while (itElem.hasNext()) {
                Element elem = itElem.next();
                String key = elem.getName();
                if (!props.containsKey(key)) {
                    itElem.remove();
                }
            }
        }
    }

    /**
     * Method findAndReplaceSimpleElement.
     *
     * @param counter The counter
     * @param parent The parent element
     * @param name The name of the element
     * @param text The text to add
     * @param defaultValue The default value of the element
     * @param preserveEmpty Whether empty text should still be written
     */
    private void findAndReplaceSimpleElement(Counter counter, Element parent, String name, String text,
            String defaultValue, boolean preserveEmpty) {
        if ((defaultValue != null) && defaultValue.equals(text)) {
            Element element = parent.getChild(name, parent.getNamespace());
            // if exist and is default value or if it doesn't exist, just keep the
            // way it is. otherwise remove it
            if (element != null && !defaultValue.equals(element.getText())) {
                parent.removeContent(element);
                removeExtraIndents(parent.getContent());
            }
            return;
        }

        boolean shouldExist = (text != null) && (preserveEmpty || !text.trim().isEmpty());
        Element element = updateElement(counter, parent, name, shouldExist);
        if (shouldExist) {
            element.setText(text);
        }
    }

    /**
     * Method findAndReplaceSimpleLists.
     *
     * @param counter The counter
     * @param parent The parent element
     * @param list The list to add
     * @param parentName The name of the parent element
     * @param childName The name of the newProperty element
     */
    private void findAndReplaceSimpleLists(Counter counter, Element parent, Collection<String> list,
            String parentName, String childName) {
        boolean shouldExist = (list != null) && (!list.isEmpty());
        Element element = updateElement(counter, parent, parentName, shouldExist);
        if (shouldExist) {
            var it = list.iterator();
            var elIt = element.getChildren(childName, element.getNamespace()).iterator();
            if (!elIt.hasNext()) {
                elIt = null;
            }

            Counter innerCount = counter.newNextDepthLevelCounter();
            while (it.hasNext()) {
                String value = it.next();
                Element el;
                if ((elIt != null) && elIt.hasNext()) {
                    el = elIt.next();
                    if (!elIt.hasNext()) {
                        elIt = null;
                    }
                } else {
                    el = factory.element(childName, element.getNamespace());
                    insertAtPreferredLocation(element, el, innerCount);
                }
                el.setText(value);
                innerCount.increaseCount();
            }
            if (elIt != null) {
                while (elIt.hasNext()) {
                    elIt.next();
                    elIt.remove();
                }
                removeExtraIndents(element.getContent());
            }
        }
    }

    /**
     * Method findAndReplaceXpp3DOM.
     *
     * @param counter The counter
     * @param dom The Xpp3Dom to add
     * @param name The name of the element
     * @param parent The parent element
     */
    private void findAndReplaceXpp3DOM(Counter counter, Element parent, String name, Xpp3Dom dom) {
        boolean shouldExist = (dom != null) && ((dom.getChildCount() > 0) || (dom.getValue() != null));
        Element element = updateElement(counter, parent, name, shouldExist);
        if (shouldExist) {
            replaceXpp3DOM(element, dom, counter.newNextDepthLevelCounter());
        }
    }

    /**
     * Method insertAtPreferredLocation.
     *
     * @param parent The parent element
     * @param counter The counter
     * @param child The newProperty element
     */
    private void insertAtPreferredLocation(Element parent, Element child, Counter counter) {
        int contentIndex = 0;
        int elementCounter = 0;
        var it = parent.getContent().iterator();
        int offset = 0;
        while (it.hasNext() && (elementCounter < counter.getCurrentIndex())) {
            Object next = it.next();
            offset++;
            if (next instanceof Element) {
                elementCounter++;
                contentIndex += offset;
                offset = 0;
            }
        }
        Text lastText = factory.text(lineSeparator + indentation.repeat(counter.getDepth()));
        if (parent.getContentSize() == 0) {
            Text finalText = lastText.clone();
            finalText.setText(finalText.getText().substring(0, finalText.getText().length() - indentation.length()));
            parent.addContent(contentIndex, finalText);
        }
        parent.addContent(contentIndex, child);
        parent.addContent(contentIndex, lastText);
    }

    /**
     * Method iterateContributor.
     *
     * @param counter The counter
     * @param parent The parent element
     * @param list The list to iterate
     */
    private void iterateContributor(Counter counter, Element parent, Collection<Contributor> list) {
        boolean shouldExist = (list != null) && (!list.isEmpty());
        Element element = updateElement(counter, parent, "contributors", shouldExist);
        if (shouldExist) {
            var it = list.iterator();
            var elIt = element.getChildren("contributor", element.getNamespace()).iterator();
            if (!elIt.hasNext()) {
                elIt = null;
            }

            Counter innerCount = counter.newNextDepthLevelCounter();
            while (it.hasNext()) {
                Contributor value = it.next();
                Element el;
                if ((elIt != null) && elIt.hasNext()) {
                    el = elIt.next();
                    if (!elIt.hasNext()) {
                        elIt = null;
                    }
                } else {
                    el = factory.element("contributor", element.getNamespace());
                    insertAtPreferredLocation(element, el, innerCount);
                }
                updateContributor(value, innerCount, el);
                innerCount.increaseCount();
            }
            if (elIt != null) {
                while (elIt.hasNext()) {
                    elIt.next();
                    elIt.remove();
                }
                removeExtraIndents(element.getContent());
            }
        }
    }

    /**
     * Method iterateDependency.
     *
     * @param counter The counter
     * @param parent The parent element
     * @param list The list to iterate
     */
    private void iterateDependency(Counter counter, Element parent, Collection<Dependency> list) {
        boolean shouldExist = (list != null) && (!list.isEmpty());
        Element element = updateElement(counter, parent, "dependencies", shouldExist);
        if (shouldExist) {
            var it = list.iterator();
            var elIt = element.getChildren("dependency", element.getNamespace()).iterator();
            if (!elIt.hasNext()) {
                elIt = null;
            }

            Counter innerCount = counter.newNextDepthLevelCounter();
            while (it.hasNext()) {
                Dependency value = it.next();
                Element el;
                if ((elIt != null) && elIt.hasNext()) {
                    el = elIt.next();
                    if (!elIt.hasNext()) {
                        elIt = null;
                    }
                } else {
                    el = factory.element("dependency", element.getNamespace());
                    insertAtPreferredLocation(element, el, innerCount);
                }
                updateDependency(value, innerCount, el);
                innerCount.increaseCount();
            }
            if (elIt != null) {
                while (elIt.hasNext()) {
                    elIt.next();
                    elIt.remove();
                }
                removeExtraIndents(element.getContent());
            }
        }
    }

    /**
     * When elements are removed from the JDOM tree, there may be Text nodes (used for indentation) that are left behind.
     * This method removes these nodes.
     *
     * @param content The list of content to remove text nodes from
     */
    private void removeExtraIndents(List<Content> content) {
        // We don't want to remove the first or last text node
        for (int i = content.size() - 2; i > 0; i--) {
            Content current = content.get(i);
            if (current instanceof Text) {
                content.remove(i);
            } else {
                // If the current element is not a text node, we can stop
                break;
            }
        }
    }

    /**
     * Method iterateDeveloper.
     *
     * @param counter The counter
     * @param parent The parent element
     * @param list The list to iterate
     */
    private void iterateDeveloper(Counter counter, Element parent, Collection<Developer> list) {
        boolean shouldExist = (list != null) && (!list.isEmpty());
        Element element = updateElement(counter, parent, "developers", shouldExist);
        if (shouldExist) {
            var it = list.iterator();
            var elIt = element.getChildren("developer", element.getNamespace()).iterator();
            if (!elIt.hasNext()) {
                elIt = null;
            }

            Counter innerCount = counter.newNextDepthLevelCounter();
            while (it.hasNext()) {
                Developer value = it.next();
                Element el;
                if ((elIt != null) && elIt.hasNext()) {
                    el = elIt.next();
                    if (!elIt.hasNext()) {
                        elIt = null;
                    }
                } else {
                    el = factory.element("developer", element.getNamespace());
                    insertAtPreferredLocation(element, el, innerCount);
                }
                updateDeveloper(value, innerCount, el);
                innerCount.increaseCount();
            }
            if (elIt != null) {
                while (elIt.hasNext()) {
                    elIt.next();
                    elIt.remove();
                }
                removeExtraIndents(element.getContent());
            }
        }
    }

    /**
     * Method iterateExclusion.
     *
     * @param counter The counter
     * @param parent The parent element
     * @param list The list to iterate
     */
    private void iterateExclusion(Counter counter, Element parent, Collection<Exclusion> list) {
        boolean shouldExist = (list != null) && (!list.isEmpty());
        Element element = updateElement(counter, parent, "exclusions", shouldExist);
        if (shouldExist) {
            var it = list.iterator();
            var elIt = element.getChildren("exclusion", element.getNamespace()).iterator();
            if (!elIt.hasNext()) {
                elIt = null;
            }

            Counter innerCount = counter.newNextDepthLevelCounter();
            while (it.hasNext()) {
                Exclusion value = it.next();
                Element el;
                if ((elIt != null) && elIt.hasNext()) {
                    el = elIt.next();
                    if (!elIt.hasNext()) {
                        elIt = null;
                    }
                } else {
                    el = factory.element("exclusion", element.getNamespace());
                    insertAtPreferredLocation(element, el, innerCount);
                }
                updateExclusion(value, innerCount, el);
                innerCount.increaseCount();
            }
            if (elIt != null) {
                while (elIt.hasNext()) {
                    elIt.next();
                    elIt.remove();
                }
                removeExtraIndents(element.getContent());
            }
        }
    }

    /**
     * Method iterateExtension.
     *
     * @param counter The counter
     * @param parent The parent element
     * @param list The list to iterate
     */
    private void iterateExtension(Counter counter, Element parent, Collection<Extension> list) {
        boolean shouldExist = (list != null) && (!list.isEmpty());
        Element element = updateElement(counter, parent, "extensions", shouldExist);
        if (shouldExist) {
            var it = list.iterator();
            var elIt = element.getChildren("extension", element.getNamespace()).iterator();
            if (!elIt.hasNext()) {
                elIt = null;
            }

            Counter innerCount = counter.newNextDepthLevelCounter();
            while (it.hasNext()) {
                Extension value = it.next();
                Element el;
                if ((elIt != null) && elIt.hasNext()) {
                    el = elIt.next();
                    if (!elIt.hasNext()) {
                        elIt = null;
                    }
                } else {
                    el = factory.element("extension", element.getNamespace());
                    insertAtPreferredLocation(element, el, innerCount);
                }
                updateExtension(value, innerCount, el);
                innerCount.increaseCount();
            }
            if (elIt != null) {
                while (elIt.hasNext()) {
                    elIt.next();
                    elIt.remove();
                }
                removeExtraIndents(element.getContent());
            }
        }
    }

    /**
     * Method iterateLicense.
     *
     * @param counter The counter
     * @param parent The parent element
     * @param list The list to iterate
     */
    private void iterateLicense(Counter counter, Element parent, Collection<License> list) {
        boolean shouldExist = (list != null) && (!list.isEmpty());
        Element element = updateElement(counter, parent, "licenses", shouldExist);
        if (shouldExist) {
            var it = list.iterator();
            var elIt = element.getChildren("license", element.getNamespace()).iterator();
            if (!elIt.hasNext()) {
                elIt = null;
            }

            Counter innerCount = counter.newNextDepthLevelCounter();
            while (it.hasNext()) {
                License value = it.next();
                Element el;
                if ((elIt != null) && elIt.hasNext()) {
                    el = elIt.next();
                    if (!elIt.hasNext()) {
                        elIt = null;
                    }
                } else {
                    el = factory.element("license", element.getNamespace());
                    insertAtPreferredLocation(element, el, innerCount);
                }
                updateLicense(value, innerCount, el);
                innerCount.increaseCount();
            }
            if (elIt != null) {
                while (elIt.hasNext()) {
                    elIt.next();
                    elIt.remove();
                }
                removeExtraIndents(element.getContent());
            }
        }
    }

    /**
     * Method iterateMailingList.
     *
     * @param counter The counter
     * @param parent The parent element
     * @param list The list to iterate
     */
    private void iterateMailingList(Counter counter, Element parent, Collection<MailingList> list) {
        boolean shouldExist = (list != null) && (!list.isEmpty());
        Element element = updateElement(counter, parent, "mailingLists", shouldExist);
        if (shouldExist) {
            var it = list.iterator();
            var elIt = element.getChildren("mailingList", element.getNamespace()).iterator();
            if (!elIt.hasNext()) {
                elIt = null;
            }

            Counter innerCount = counter.newNextDepthLevelCounter();
            while (it.hasNext()) {
                MailingList value = it.next();
                Element el;
                if ((elIt != null) && elIt.hasNext()) {
                    el = elIt.next();
                    if (!elIt.hasNext()) {
                        elIt = null;
                    }
                } else {
                    el = factory.element("mailingList", element.getNamespace());
                    insertAtPreferredLocation(element, el, innerCount);
                }
                updateMailingList(value, innerCount, el);
                innerCount.increaseCount();
            }
            if (elIt != null) {
                while (elIt.hasNext()) {
                    elIt.next();
                    elIt.remove();
                }
                removeExtraIndents(element.getContent());
            }
        }
    }

    /**
     * Method iterateNotifier.
     *
     * @param counter The counter
     * @param parent The parent element
     * @param list The list to iterate
     */
    private void iterateNotifier(Counter counter, Element parent, Collection<Notifier> list) {
        boolean shouldExist = (list != null) && (!list.isEmpty());
        Element element = updateElement(counter, parent, "notifiers", shouldExist);
        if (shouldExist) {
            var it = list.iterator();
            var elIt = element.getChildren("notifier", element.getNamespace()).iterator();
            if (!elIt.hasNext()) {
                elIt = null;
            }

            Counter innerCount = counter.newNextDepthLevelCounter();
            while (it.hasNext()) {
                Notifier value = it.next();
                Element el;
                if ((elIt != null) && elIt.hasNext()) {
                    el = elIt.next();
                    if (!elIt.hasNext()) {
                        elIt = null;
                    }
                } else {
                    el = factory.element("notifier", element.getNamespace());
                    insertAtPreferredLocation(element, el, innerCount);
                }
                updateNotifier(value, innerCount, el);
                innerCount.increaseCount();
            }
            if (elIt != null) {
                while (elIt.hasNext()) {
                    elIt.next();
                    elIt.remove();
                }
                removeExtraIndents(element.getContent());
            }
        }
    }

    /**
     * Method iteratePlugin.
     *
     * @param counter The counter
     * @param parent The parent element
     * @param list The list to iterate
     */
    private void iteratePlugin(Counter counter, Element parent, Collection<Plugin> list) {
        boolean shouldExist = (list != null) && (!list.isEmpty());
        Element element = updateElement(counter, parent, "plugins", shouldExist);
        if (shouldExist) {
            var it = list.iterator();
            var elIt = element.getChildren("plugin", element.getNamespace()).iterator();
            if (!elIt.hasNext()) {
                elIt = null;
            }

            Counter innerCount = counter.newNextDepthLevelCounter();
            while (it.hasNext()) {
                Plugin value = it.next();
                Element el;
                if ((elIt != null) && elIt.hasNext()) {
                    el = elIt.next();
                    if (!elIt.hasNext()) {
                        elIt = null;
                    }
                } else {
                    el = factory.element("plugin", element.getNamespace());
                    insertAtPreferredLocation(element, el, innerCount);
                }
                updatePlugin(value, innerCount, el);
                innerCount.increaseCount();
            }
            if (elIt != null) {
                while (elIt.hasNext()) {
                    elIt.next();
                    elIt.remove();
                }
                removeExtraIndents(element.getContent());
            }
        }
    }

    /**
     * Method iteratePluginExecution.
     *
     * @param counter The counter
     * @param parent The parent element
     * @param list The list to iterate
     */
    private void iteratePluginExecution(Counter counter, Element parent, Collection<PluginExecution> list) {
        boolean shouldExist = (list != null) && (!list.isEmpty());
        Element element = updateElement(counter, parent, "executions", shouldExist);
        if (shouldExist) {
            var it = list.iterator();
            var elIt = element.getChildren("execution", element.getNamespace()).iterator();
            if (!elIt.hasNext()) {
                elIt = null;
            }

            Counter innerCount = counter.newNextDepthLevelCounter();
            while (it.hasNext()) {
                PluginExecution value = it.next();
                Element el;
                if ((elIt != null) && elIt.hasNext()) {
                    el = elIt.next();
                    if (!elIt.hasNext()) {
                        elIt = null;
                    }
                } else {
                    el = factory.element("execution", element.getNamespace());
                    insertAtPreferredLocation(element, el, innerCount);
                }
                updatePluginExecution(value, innerCount, el);
                innerCount.increaseCount();
            }
            if (elIt != null) {
                while (elIt.hasNext()) {
                    elIt.next();
                    elIt.remove();
                }
                removeExtraIndents(element.getContent());
            }
        }
    }

    /**
     * Method iterateProfile.
     *
     * @param counter The counter
     * @param parent The parent element
     * @param list The list to iterate
     */
    private void iterateProfile(Counter counter, Element parent, Collection<Profile> list) {
        boolean shouldExist = (list != null) && (!list.isEmpty());
        Element element = updateElement(counter, parent, "profiles", shouldExist);
        if (shouldExist) {
            var it = list.iterator();
            var elIt = element.getChildren("profile", element.getNamespace()).iterator();
            if (!elIt.hasNext()) {
                elIt = null;
            }

            Counter innerCount = counter.newNextDepthLevelCounter();
            while (it.hasNext()) {
                Profile value = it.next();
                Element el;
                if ((elIt != null) && elIt.hasNext()) {
                    el = elIt.next();
                    if (!elIt.hasNext()) {
                        elIt = null;
                    }
                } else {
                    el = factory.element("profile", element.getNamespace());
                    insertAtPreferredLocation(element, el, innerCount);
                }
                updateProfile(value, innerCount, el);
                innerCount.increaseCount();
            }
            if (elIt != null) {
                while (elIt.hasNext()) {
                    elIt.next();
                    elIt.remove();
                }
                removeExtraIndents(element.getContent());
            }
        }
    }

    /**
     * Method iterateReportPlugin.
     *
     * @param counter The counter
     * @param parent The parent element
     * @param list The list to iterate
     */
    private void iterateReportPlugin(Counter counter, Element parent, Collection<ReportPlugin> list) {
        boolean shouldExist = (list != null) && (!list.isEmpty());
        Element element = updateElement(counter, parent, "plugins", shouldExist);
        if (shouldExist) {
            var it = list.iterator();
            var elIt = element.getChildren("plugin", element.getNamespace()).iterator();
            if (!elIt.hasNext()) {
                elIt = null;
            }

            Counter innerCount = counter.newNextDepthLevelCounter();
            while (it.hasNext()) {
                ReportPlugin value = it.next();
                Element el;
                if ((elIt != null) && elIt.hasNext()) {
                    el = elIt.next();
                    if (!elIt.hasNext()) {
                        elIt = null;
                    }
                } else {
                    el = factory.element("plugin", element.getNamespace());
                    insertAtPreferredLocation(element, el, innerCount);
                }
                updateReportPlugin(value, innerCount, el);
                innerCount.increaseCount();
            }
            if (elIt != null) {
                while (elIt.hasNext()) {
                    elIt.next();
                    elIt.remove();
                }
                removeExtraIndents(element.getContent());
            }
        }
    }

    /**
     * Method iterateReportSet.
     *
     * @param counter The counter
     * @param parent The parent element
     * @param list The list to iterate
     */
    private void iterateReportSet(Counter counter, Element parent, Collection<ReportSet> list) {
        boolean shouldExist = (list != null) && (!list.isEmpty());
        Element element = updateElement(counter, parent, "reportSets", shouldExist);
        if (shouldExist) {
            var it = list.iterator();
            var elIt = element.getChildren("reportSet", element.getNamespace()).iterator();
            if (!elIt.hasNext()) {
                elIt = null;
            }

            Counter innerCount = counter.newNextDepthLevelCounter();
            while (it.hasNext()) {
                ReportSet value = it.next();
                Element el;
                if ((elIt != null) && elIt.hasNext()) {
                    el = elIt.next();
                    if (!elIt.hasNext()) {
                        elIt = null;
                    }
                } else {
                    el = factory.element("reportSet", element.getNamespace());
                    insertAtPreferredLocation(element, el, innerCount);
                }
                updateReportSet(value, innerCount, el);
                innerCount.increaseCount();
            }
            if (elIt != null) {
                while (elIt.hasNext()) {
                    elIt.next();
                    elIt.remove();
                }
                removeExtraIndents(element.getContent());
            }
        }
    }

    /**
     * Method iterateRepository.
     *
     * @param counter The counter
     * @param parent The parent element
     * @param list The list to iterate
     * @param parentTag The tag of the parent element
     * @param childTag The tag of the newProperty element
     */
    private void iterateRepository(Counter counter, Element parent, Collection<Repository> list,
            String parentTag, String childTag) {
        boolean shouldExist = (list != null) && (!list.isEmpty());
        Element element = updateElement(counter, parent, parentTag, shouldExist);
        if (shouldExist) {
            var it = list.iterator();
            var elIt = element.getChildren(childTag, element.getNamespace()).iterator();
            if (!elIt.hasNext()) {
                elIt = null;
            }

            Counter innerCount = counter.newNextDepthLevelCounter();
            while (it.hasNext()) {
                Repository value = it.next();
                Element el;
                if ((elIt != null) && elIt.hasNext()) {
                    el = elIt.next();
                    if (!elIt.hasNext()) {
                        elIt = null;
                    }
                } else {
                    el = factory.element(childTag, element.getNamespace());
                    insertAtPreferredLocation(element, el, innerCount);
                }
                updateRepository(value, innerCount, el);
                innerCount.increaseCount();
            }
            if (elIt != null) {
                while (elIt.hasNext()) {
                    elIt.next();
                    elIt.remove();
                }
                removeExtraIndents(element.getContent());
            }
        }
    }

    /**
     * Method iterateResource.
     *
     * @param counter The counter
     * @param parent The parent element
     * @param list The list to iterate
     * @param parentTag The tag of the parent element
     * @param childTag The tag of the newProperty element
     */
    private void iterateResource(Counter counter, Element parent, Collection<Resource> list,
            String parentTag, String childTag) {
        boolean shouldExist = (list != null) && (!list.isEmpty());
        Element element = updateElement(counter, parent, parentTag, shouldExist);
        if (shouldExist) {
            var it = list.iterator();
            var elIt = element.getChildren(childTag, element.getNamespace()).iterator();
            if (!elIt.hasNext()) {
                elIt = null;
            }

            Counter innerCount = counter.newNextDepthLevelCounter();
            while (it.hasNext()) {
                Resource value = it.next();
                Element el;
                if ((elIt != null) && elIt.hasNext()) {
                    el = elIt.next();
                    if (!elIt.hasNext()) {
                        elIt = null;
                    }
                } else {
                    el = factory.element(childTag, element.getNamespace());
                    insertAtPreferredLocation(element, el, innerCount);
                }
                updateResource(value, innerCount, el);
                innerCount.increaseCount();
            }
            if (elIt != null) {
                while (elIt.hasNext()) {
                    elIt.next();
                    elIt.remove();
                }
                removeExtraIndents(element.getContent());
            }
        }
    }

    /**
     * Method replaceXpp3DOM.
     *
     * @param parent The parent element
     * @param parentDom The parent Xpp3Dom
     * @param counter The counter
     */
    private void replaceXpp3DOM(final Element parent, final Xpp3Dom parentDom, final Counter counter) {
        if (parentDom.getChildCount() > 0) {
            Xpp3Dom[] children = parentDom.getChildren();
            Collection<Xpp3Dom> domChildren = new ArrayList<>(Arrays.asList(children));

            ListIterator<Element> it = parent.getChildren().listIterator();
            while (it.hasNext()) {
                Element elem = it.next();
                Iterator<Xpp3Dom> it2 = domChildren.iterator();
                Xpp3Dom corrDom = null;
                while (it2.hasNext()) {
                    Xpp3Dom dm = it2.next();
                    if (dm.getName().equals(elem.getName())) {
                        corrDom = dm;
                        break;
                    }
                }
                if (corrDom != null) {
                    domChildren.remove(corrDom);
                    replaceXpp3DOM(elem, corrDom, counter.newNextDepthLevelCounter());
                    counter.increaseCount();
                } else {
                    it.remove();
                }
            }

            for (Xpp3Dom dm : domChildren) {
                Element elem = factory.element(dm.getName(), parent.getNamespace());
                for (String attName : dm.getAttributeNames()) {
                    elem.setAttribute(attName, dm.getAttribute(attName));
                }
                insertAtPreferredLocation(parent, elem, counter);
                counter.increaseCount();
                replaceXpp3DOM(elem, dm, counter.newNextDepthLevelCounter());
            }
        } else if (parentDom.getValue() != null) {
            parent.setText(parentDom.getValue());
        }
    }

    /**
     * Method updateActivation.
     *
     * @param value The Activation to update
     * @param counter The counter
     * @param element The parent element
     */
    private void updateActivation(Activation value, Counter counter, Element element) {
        boolean shouldExist = value != null;
        Element root = updateElement(counter, element, "activation", shouldExist);
        if (shouldExist) {
            Counter innerCount = counter.newNextDepthLevelCounter();
            findAndReplaceSimpleElement(innerCount, root, "activeByDefault",
                    (!value.isActiveByDefault()) ? null : String.valueOf(value.isActiveByDefault()), "false", false);
            findAndReplaceSimpleElement(innerCount, root, "jdk", value.getJdk(), null, false);
            updateActivationOS(value.getOs(), innerCount, root);
            updateActivationProperty(value.getProperty(), innerCount, root);
            updateActivationFile(value.getFile(), innerCount, root);
        }
    }

    /**
     * Method updateActivationFile.
     *
     * @param value The ActivationFile to update
     * @param counter The counter
     * @param element The parent element
     */
    private void updateActivationFile(ActivationFile value, Counter counter, Element element) {
        boolean shouldExist = value != null;
        Element root = updateElement(counter, element, "file", shouldExist);
        if (shouldExist) {
            Counter innerCount = counter.newNextDepthLevelCounter();
            findAndReplaceSimpleElement(innerCount, root, "missing", value.getMissing(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "exists", value.getExists(), null, false);
        }
    }

    /**
     * Method updateActivationOS.
     *
     * @param value The ActivationOS to update
     * @param counter The counter
     * @param element The parent element
     */
    private void updateActivationOS(ActivationOS value, Counter counter, Element element) {
        boolean shouldExist = value != null;
        Element root = updateElement(counter, element, "os", shouldExist);
        if (shouldExist) {
            Counter innerCount = counter.newNextDepthLevelCounter();
            findAndReplaceSimpleElement(innerCount, root, "name", value.getName(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "family", value.getFamily(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "arch", value.getArch(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "version", value.getVersion(), null, false);
        }
    }

    /**
     * Method updateActivationProperty.
     *
     * @param value The ActivationProperty to update
     * @param counter The counter
     * @param element The parent element
     */
    private void updateActivationProperty(ActivationProperty value, Counter counter, Element element) {
        boolean shouldExist = value != null;
        Element root = updateElement(counter, element, "property", shouldExist);
        if (shouldExist) {
            Counter innerCount = counter.newNextDepthLevelCounter();
            findAndReplaceSimpleElement(innerCount, root, "name", value.getName(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "value", value.getValue(), null, false);
        }
    }

    /**
     * Method updateBuild.
     *
     * @param value The Build to update
     * @param counter The counter
     * @param element The parent element
     */
    private void updateBuild(Build value, Counter counter, Element element) {
        boolean shouldExist = value != null;
        Element root = updateElement(counter, element, "build", shouldExist);
        if (shouldExist) {
            Counter innerCount = counter.newNextDepthLevelCounter();
            findAndReplaceSimpleElement(innerCount, root, "sourceDirectory", value.getSourceDirectory(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "scriptSourceDirectory", value.getScriptSourceDirectory(),
                    null, false);
            findAndReplaceSimpleElement(innerCount, root, "testSourceDirectory", value.getTestSourceDirectory(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "outputDirectory", value.getOutputDirectory(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "testOutputDirectory", value.getTestOutputDirectory(), null, false);
            iterateExtension(innerCount, root, value.getExtensions());
            findAndReplaceSimpleElement(innerCount, root, "defaultGoal", value.getDefaultGoal(), null, false);
            iterateResource(innerCount, root, value.getResources(), "resources", "resource");
            iterateResource(innerCount, root, value.getTestResources(), "testResources", "testResource");
            findAndReplaceSimpleElement(innerCount, root, "directory", value.getDirectory(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "finalName", value.getFinalName(), null, false);
            findAndReplaceSimpleLists(innerCount, root, value.getFilters(), "filters", "filter");
            updatePluginManagement(value.getPluginManagement(), innerCount, root);
            iteratePlugin(innerCount, root, value.getPlugins());
        } // end if
    }

    /**
     * Method updateBuildBase.
     *
     * @param value The BuildBase to update
     * @param counter The counter
     * @param element The parent element
     */
    private void updateBuildBase(BuildBase value, Counter counter, Element element) {
        boolean shouldExist = value != null;
        Element root = updateElement(counter, element, "build", shouldExist);
        if (shouldExist) {
            Counter innerCount = counter.newNextDepthLevelCounter();
            findAndReplaceSimpleElement(innerCount, root, "defaultGoal", value.getDefaultGoal(), null, false);
            iterateResource(innerCount, root, value.getResources(), "resources", "resource");
            iterateResource(innerCount, root, value.getTestResources(), "testResources", "testResource");
            findAndReplaceSimpleElement(innerCount, root, "directory", value.getDirectory(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "finalName", value.getFinalName(), null, false);
            findAndReplaceSimpleLists(innerCount, root, value.getFilters(), "filters", "filter");
            updatePluginManagement(value.getPluginManagement(), innerCount, root);
            iteratePlugin(innerCount, root, value.getPlugins());
        }
    }

    /**
     * Method updateCiManagement.
     *
     * @param value The CiManagement to update
     * @param counter The counter
     * @param element The parent element
     */
    private void updateCiManagement(CiManagement value, Counter counter, Element element) {
        boolean shouldExist = value != null;
        Element root = updateElement(counter, element, "ciManagement", shouldExist);
        if (shouldExist) {
            Counter innerCount = counter.newNextDepthLevelCounter();
            findAndReplaceSimpleElement(innerCount, root, "system", value.getSystem(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "url", value.getUrl(), null, false);
            iterateNotifier(innerCount, root, value.getNotifiers());
        }
    }

    /**
     * Method updateContributor.
     *
     * @param value The Contributor to update
     * @param counter The counter
     * @param root The parent element
     */
    private void updateContributor(Contributor value, Counter counter, Element root) {
        Counter innerCount = counter.newNextDepthLevelCounter();
        findAndReplaceSimpleElement(innerCount, root, "name", value.getName(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "email", value.getEmail(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "url", value.getUrl(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "organization", value.getOrganization(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "organizationUrl", value.getOrganizationUrl(), null, false);
        findAndReplaceSimpleLists(innerCount, root, value.getRoles(), "roles", "role");
        findAndReplaceSimpleElement(innerCount, root, "timezone", value.getTimezone(), null, false);
        findAndReplaceProperties(innerCount, root, "properties", value.getProperties());
    }

    /**
     * Method updateDependency.
     *
     * @param value The Dependency to update
     * @param root The parent element
     * @param counter The counter
     */
    private void updateDependency(Dependency value, Counter counter, Element root) {
        Counter innerCount = counter.newNextDepthLevelCounter();
        findAndReplaceSimpleElement(innerCount, root, "groupId", value.getGroupId(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "artifactId", value.getArtifactId(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "version", value.getVersion(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "type", value.getType(), "jar", false);
        findAndReplaceSimpleElement(innerCount, root, "classifier", value.getClassifier(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "scope", value.getScope(), "compile", false);
        findAndReplaceSimpleElement(innerCount, root, "systemPath", value.getSystemPath(), null, false);
        iterateExclusion(innerCount, root, value.getExclusions());
        findAndReplaceSimpleElement(innerCount, root, "optional",
                (!value.isOptional()) ? null : String.valueOf(value.isOptional()), "false", false);
    }

    /**
     * Method updateDependencyManagement.
     *
     * @param value The DependencyManagement to update
     * @param counter The counter
     * @param element The parent element
     */
    private void updateDependencyManagement(DependencyManagement value, Counter counter,
            Element element) {
        boolean shouldExist = value != null;
        Element root = updateElement(counter, element, "dependencyManagement", shouldExist);
        if (shouldExist) {
            Counter innerCount = counter.newNextDepthLevelCounter();
            iterateDependency(innerCount, root, value.getDependencies());
        }
    }

    /**
     * Method updateDeploymentRepository.
     *
     * @param value The DeploymentRepository to update
     * @param xmlTag The tag of the parent element
     * @param counter The counter
     * @param element The parent element
     */
    private void updateDeploymentRepository(DeploymentRepository value, String xmlTag, Counter counter,
            Element element) {
        boolean shouldExist = value != null;
        Element root = updateElement(counter, element, xmlTag, shouldExist);
        if (shouldExist) {
            Counter innerCount = counter.newNextDepthLevelCounter();
            updateRepositoryPolicy(value.getReleases(), "releases", innerCount, root);
            updateRepositoryPolicy(value.getSnapshots(), "snapshots", innerCount, root);
            findAndReplaceSimpleElement(innerCount, root, "uniqueVersion",
                    (value.isUniqueVersion()) ? null : String.valueOf(value.isUniqueVersion()), "true", false);
            findAndReplaceSimpleElement(innerCount, root, "id", value.getId(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "name", value.getName(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "url", value.getUrl(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "layout", value.getLayout(), "default", false);
        }
    }

    /**
     * Method updateDeveloper.
     *
     * @param value The Developer to update
     * @param counter The counter
     * @param root The parent element
     */
    private void updateDeveloper(Developer value, Counter counter, Element root) {
        Counter innerCount = counter.newNextDepthLevelCounter();
        findAndReplaceSimpleElement(innerCount, root, "id", value.getId(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "name", value.getName(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "email", value.getEmail(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "url", value.getUrl(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "organization", value.getOrganization(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "organizationUrl", value.getOrganizationUrl(), null, false);
        findAndReplaceSimpleLists(innerCount, root, value.getRoles(), "roles", "role");
        findAndReplaceSimpleElement(innerCount, root, "timezone", value.getTimezone(), null, false);
        findAndReplaceProperties(innerCount, root, "properties", value.getProperties());
    }

    /**
     * Method updateDistributionManagement.
     *
     * @param value The DistributionManagement to update
     * @param counter The counter
     * @param element The parent element
     */
    private void updateDistributionManagement(DistributionManagement value, Counter counter,
            Element element) {
        boolean shouldExist = value != null;
        Element root = updateElement(counter, element, "distributionManagement", shouldExist);
        if (shouldExist) {
            Counter innerCount = counter.newNextDepthLevelCounter();
            updateDeploymentRepository(value.getRepository(), "repository", innerCount, root);
            updateDeploymentRepository(value.getSnapshotRepository(), "snapshotRepository", innerCount, root);
            updateSite(value.getSite(), innerCount, root);
            findAndReplaceSimpleElement(innerCount, root, "downloadUrl", value.getDownloadUrl(), null, false);
            updateRelocation(value.getRelocation(), innerCount, root);
            findAndReplaceSimpleElement(innerCount, root, "status", value.getStatus(), null, false);
        }
    }

    /**
     * Method updateElement.
     *
     * @param counter The counter
     * @param parent The parent element
     * @param name The name of the element
     * @param shouldExist Whether the element should exist
     */
    private Element updateElement(Counter counter, Element parent, String name, boolean shouldExist) {
        Element element = parent.getChild(name, parent.getNamespace());
        if ((element != null) && shouldExist) {
            counter.increaseCount();
        }
        if ((element == null) && shouldExist) {
            element = factory.element(name, parent.getNamespace());
            insertAtPreferredLocation(parent, element, counter);
            counter.increaseCount();
        }
        if (!shouldExist && (element != null)) {
            int index = parent.indexOf(element);
            if (index > 0) {
                Content previous = parent.getContent(index - 1);
                if (previous instanceof Text) {
                    Text txt = (Text) previous;
                    if (txt.getTextTrim().isEmpty()) {
                        parent.removeContent(txt);
                    }
                }
            }
            parent.removeContent(element);
        }
        return element;
    }

    /**
     * Method updateExclusion.
     *
     * @param value The Exclusion to update
     * @param counter The counter
     * @param root The parent element
     */
    private void updateExclusion(Exclusion value, Counter counter, Element root) {
        Counter innerCount = counter.newNextDepthLevelCounter();
        findAndReplaceSimpleElement(innerCount, root, "groupId", value.getGroupId(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "artifactId", value.getArtifactId(), null, false);
    }

    /**
     * Method updateExtension.
     *
     * @param value The Extension to update
     * @param counter The counter
     * @param root The parent element
     */
    private void updateExtension(Extension value, Counter counter, Element root) {
        Counter innerCount = counter.newNextDepthLevelCounter();
        findAndReplaceSimpleElement(innerCount, root, "groupId", value.getGroupId(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "artifactId", value.getArtifactId(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "version", value.getVersion(), null, false);
    }

    /**
     * Method updateIssueManagement.
     *
     * @param value The IssueManagement to update
     * @param counter The counter
     * @param element The parent element
     */
    private void updateIssueManagement(IssueManagement value, Counter counter, Element element) {
        boolean shouldExist = value != null;
        Element root = updateElement(counter, element, "issueManagement", shouldExist);
        if (shouldExist) {
            Counter innerCount = counter.newNextDepthLevelCounter();
            findAndReplaceSimpleElement(innerCount, root, "system", value.getSystem(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "url", value.getUrl(), null, false);
        }
    }

    /**
     * Method updateLicense.
     *
     * @param value The License to update
     * @param counter The counter
     * @param root The parent element
     */
    private void updateLicense(License value, Counter counter, Element root) {
        Counter innerCount = counter.newNextDepthLevelCounter();
        findAndReplaceSimpleElement(innerCount, root, "name", value.getName(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "url", value.getUrl(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "distribution", value.getDistribution(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "comments", value.getComments(), null, false);
    }

    /**
     * Method updateMailingList.
     *
     * @param value The MailingList to update
     * @param counter The counter
     * @param root The parent element
     */
    private void updateMailingList(MailingList value, Counter counter, Element root) {
        Counter innerCount = counter.newNextDepthLevelCounter();
        findAndReplaceSimpleElement(innerCount, root, "name", value.getName(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "subscribe", value.getSubscribe(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "unsubscribe", value.getUnsubscribe(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "post", value.getPost(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "archive", value.getArchive(), null, false);
        findAndReplaceSimpleLists(innerCount, root, value.getOtherArchives(), "otherArchives", "otherArchive");
    }

    /**
     * Method updateModel.
     *
     * @param value The Model to update
     * @param counter The counter
     * @param root The parent element
     */
    private void updateModel(Model value, Counter counter, Element root) {
        Counter innerCount = counter.newNextDepthLevelCounter();
        findAndReplaceAttribute(root, "child.project.url.inherit.append.path", value.getChildProjectUrlInheritAppendPath(),
                "true");
        findAndReplaceSimpleElement(innerCount, root, "modelVersion", value.getModelVersion(), null, false);
        updateParent(value.getParent(), innerCount, root);
        findAndReplaceSimpleElement(innerCount, root, "groupId", value.getGroupId(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "artifactId", value.getArtifactId(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "version", value.getVersion(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "packaging", value.getPackaging(), "jar", false);
        findAndReplaceSimpleElement(innerCount, root, "name", value.getName(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "description", value.getDescription(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "url", value.getUrl(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "inceptionYear", value.getInceptionYear(), null, false);
        updateOrganization(value.getOrganization(), innerCount, root);
        iterateLicense(innerCount, root, value.getLicenses());
        iterateDeveloper(innerCount, root, value.getDevelopers());
        iterateContributor(innerCount, root, value.getContributors());
        iterateMailingList(innerCount, root, value.getMailingLists());
        updatePrerequisites(value.getPrerequisites(), innerCount, root);
        findAndReplaceSimpleLists(innerCount, root, value.getModules(), "modules", "module");
        updateScm(value.getScm(), innerCount, root);
        updateIssueManagement(value.getIssueManagement(), innerCount, root);
        updateCiManagement(value.getCiManagement(), innerCount, root);
        updateDistributionManagement(value.getDistributionManagement(), innerCount, root);
        findAndReplaceProperties(innerCount, root, "properties", value.getProperties());
        updateDependencyManagement(value.getDependencyManagement(), innerCount, root);
        iterateDependency(innerCount, root, value.getDependencies());
        iterateRepository(innerCount, root, value.getRepositories(), "repositories", "repository");
        iterateRepository(innerCount, root, value.getPluginRepositories(), "pluginRepositories", "pluginRepository");
        updateBuild(value.getBuild(), innerCount, root);
        updateReporting(value.getReporting(), innerCount, root);
        iterateProfile(innerCount, root, value.getProfiles());
    }

    /**
     * Method updateNotifier.
     *
     * @param value The Notifier to update
     * @param root The parent element
     * @param counter The counter
     */
    private void updateNotifier(Notifier value, Counter counter, Element root) {
        Counter innerCount = counter.newNextDepthLevelCounter();
        findAndReplaceSimpleElement(innerCount, root, "type", value.getType(), "mail", false);
        findAndReplaceSimpleElement(innerCount, root, "sendOnError",
                (value.isSendOnError()) ? null : String.valueOf(value.isSendOnError()), "true", false);
        findAndReplaceSimpleElement(innerCount, root, "sendOnFailure",
                (value.isSendOnFailure()) ? null : String.valueOf(value.isSendOnFailure()), "true", false);
        findAndReplaceSimpleElement(innerCount, root, "sendOnSuccess",
                (value.isSendOnSuccess()) ? null : String.valueOf(value.isSendOnSuccess()), "true", false);
        findAndReplaceSimpleElement(innerCount, root, "sendOnWarning",
                (value.isSendOnWarning()) ? null : String.valueOf(value.isSendOnWarning()), "true", false);
        findAndReplaceSimpleElement(innerCount, root, "address", value.getAddress(), null, false);
        findAndReplaceProperties(innerCount, root, "configuration", value.getConfiguration());
    }

    /**
     * Method updateOrganization.
     *
     * @param value The Organization to update
     * @param counter The counter
     * @param element The parent element
     */
    private void updateOrganization(Organization value, Counter counter, Element element) {
        boolean shouldExist = value != null;
        Element root = updateElement(counter, element, "organization", shouldExist);
        if (shouldExist) {
            Counter innerCount = counter.newNextDepthLevelCounter();
            findAndReplaceSimpleElement(innerCount, root, "name", value.getName(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "url", value.getUrl(), null, false);
        }
    }

    /**
     * Method updateParent.
     *
     * @param value The Parent to update
     * @param counter The counter
     * @param element The parent element
     */
    private void updateParent(Parent value, Counter counter, Element element) {
        boolean shouldExist = value != null;
        Element root = updateElement(counter, element, "parent", shouldExist);
        if (shouldExist) {
            Counter innerCount = counter.newNextDepthLevelCounter();
            findAndReplaceSimpleElement(innerCount, root, "groupId", value.getGroupId(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "artifactId", value.getArtifactId(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "version", value.getVersion(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "relativePath", value.getRelativePath(), "../pom.xml", true);
        }
    }

    /**
     * Method updatePlugin.
     *
     * @param value The Plugin to update
     * @param counter The counter
     * @param root The parent element
     */
    private void updatePlugin(Plugin value, Counter counter, Element root) {
        Counter innerCount = counter.newNextDepthLevelCounter();
        findAndReplaceSimpleElement(innerCount, root, "groupId", value.getGroupId(), "org.apache.maven.plugins", false);
        findAndReplaceSimpleElement(innerCount, root, "artifactId", value.getArtifactId(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "version", value.getVersion(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "extensions",
                (!value.isExtensions()) ? null : String.valueOf(value.isExtensions()), "false", false);
        iteratePluginExecution(innerCount, root, value.getExecutions());
        iterateDependency(innerCount, root, value.getDependencies());
        findAndReplaceSimpleElement(innerCount, root, "inherited", value.getInherited(), null, false);
        findAndReplaceXpp3DOM(innerCount, root, "configuration", (Xpp3Dom) value.getConfiguration());
    }

    /**
     * Method updatePluginExecution.
     *
     * @param value The PluginExecution to update
     * @param counter The counter
     * @param root The parent element
     */
    private void updatePluginExecution(PluginExecution value, Counter counter, Element root) {
        Counter innerCount = counter.newNextDepthLevelCounter();
        findAndReplaceSimpleElement(innerCount, root, "id", value.getId(), "default", false);
        findAndReplaceSimpleElement(innerCount, root, "phase", value.getPhase(), null, false);
        findAndReplaceSimpleLists(innerCount, root, value.getGoals(), "goals", "goal");
        findAndReplaceSimpleElement(innerCount, root, "inherited", value.getInherited(), null, false);
        findAndReplaceXpp3DOM(innerCount, root, "configuration", (Xpp3Dom) value.getConfiguration());
    }

    /**
     * Method updatePluginManagement.
     *
     * @param value The PluginManagement to update
     * @param counter The counter
     * @param element The parent element
     */
    private void updatePluginManagement(PluginManagement value, Counter counter, Element element) {
        boolean shouldExist = value != null;
        Element root = updateElement(counter, element, "pluginManagement", shouldExist);
        if (shouldExist) {
            Counter innerCount = counter.newNextDepthLevelCounter();
            iteratePlugin(innerCount, root, value.getPlugins());
        }
    }

    /**
     * Method updatePrerequisites.
     *
     * @param value The Prerequisites to update
     * @param counter The counter
     * @param element The parent element
     */
    private void updatePrerequisites(Prerequisites value, Counter counter, Element element) {
        boolean shouldExist = value != null;
        Element root = updateElement(counter, element, "prerequisites", shouldExist);
        if (shouldExist) {
            Counter innerCount = counter.newNextDepthLevelCounter();
            findAndReplaceSimpleElement(innerCount, root, "maven", value.getMaven(), "2.0", false);
        }
    }

    /**
     * Method updateProfile.
     *
     * @param value The Profile to update
     * @param counter The counter
     * @param root The parent element
     */
    private void updateProfile(Profile value, Counter counter, Element root) {
        Counter innerCount = counter.newNextDepthLevelCounter();
        findAndReplaceSimpleElement(innerCount, root, "id", value.getId(), null, false);
        updateActivation(value.getActivation(), innerCount, root);
        updateBuildBase(value.getBuild(), innerCount, root);
        findAndReplaceSimpleLists(innerCount, root, value.getModules(), "modules", "module");
        updateDistributionManagement(value.getDistributionManagement(), innerCount, root);
        findAndReplaceProperties(innerCount, root, "properties", value.getProperties());
        updateDependencyManagement(value.getDependencyManagement(), innerCount, root);
        iterateDependency(innerCount, root, value.getDependencies());
        iterateRepository(innerCount, root, value.getRepositories(), "repositories", "repository");
        iterateRepository(innerCount, root, value.getPluginRepositories(), "pluginRepositories", "pluginRepository");
        updateReporting(value.getReporting(), innerCount, root);
    }

    /**
     * Method updateRelocation.
     *
     * @param value The Relocation to update
     * @param counter The counter
     * @param element The parent element
     */
    private void updateRelocation(Relocation value, Counter counter, Element element) {
        boolean shouldExist = value != null;
        Element root = updateElement(counter, element, "relocation", shouldExist);
        if (shouldExist) {
            Counter innerCount = counter.newNextDepthLevelCounter();
            findAndReplaceSimpleElement(innerCount, root, "groupId", value.getGroupId(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "artifactId", value.getArtifactId(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "version", value.getVersion(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "message", value.getMessage(), null, false);
        }
    }

    /**
     * Method updateReporting.
     *
     * @param value The Reporting to update
     * @param counter The counter
     * @param element The parent element
     */
    private void updateReporting(Reporting value, Counter counter, Element element) {
        boolean shouldExist = value != null;
        Element root = updateElement(counter, element, "reporting", shouldExist);
        if (shouldExist) {
            Counter innerCount = counter.newNextDepthLevelCounter();
            findAndReplaceSimpleElement(innerCount, root, "excludeDefaults",
                    (!value.isExcludeDefaults()) ? null : String.valueOf(value.isExcludeDefaults()), "false", false);
            findAndReplaceSimpleElement(innerCount, root, "outputDirectory", value.getOutputDirectory(), null, false);
            iterateReportPlugin(innerCount, root, value.getPlugins());
        }
    }

    /**
     * Method updateReportPlugin.
     *
     * @param value The ReportPlugin to update
     * @param root The parent element
     * @param counter The counter
     */
    private void updateReportPlugin(ReportPlugin value, Counter counter, Element root) {
        Counter innerCount = counter.newNextDepthLevelCounter();
        findAndReplaceSimpleElement(innerCount, root, "groupId", value.getGroupId(), "org.apache.maven.plugins", false);
        findAndReplaceSimpleElement(innerCount, root, "artifactId", value.getArtifactId(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "version", value.getVersion(), null, false);
        iterateReportSet(innerCount, root, value.getReportSets());
        findAndReplaceSimpleElement(innerCount, root, "inherited", value.getInherited(), null, false);
        findAndReplaceXpp3DOM(innerCount, root, "configuration", (Xpp3Dom) value.getConfiguration());
    }

    /**
     * Method updateReportSet.
     *
     * @param value The ReportSet to update
     * @param counter The counter
     * @param root The parent element
     */
    private void updateReportSet(ReportSet value, Counter counter, Element root) {
        Counter innerCount = counter.newNextDepthLevelCounter();
        findAndReplaceSimpleElement(innerCount, root, "id", value.getId(), "default", false);
        findAndReplaceSimpleLists(innerCount, root, value.getReports(), "reports", "report");
        findAndReplaceSimpleElement(innerCount, root, "inherited", value.getInherited(), null, false);
        findAndReplaceXpp3DOM(innerCount, root, "configuration", (Xpp3Dom) value.getConfiguration());
    }

    /**
     * Method updateRepository.
     *
     * @param value The Repository to update
     * @param counter The counter
     * @param root The parent element
     */
    private void updateRepository(Repository value, Counter counter, Element root) {
        Counter innerCount = counter.newNextDepthLevelCounter();
        updateRepositoryPolicy(value.getReleases(), "releases", innerCount, root);
        updateRepositoryPolicy(value.getSnapshots(), "snapshots", innerCount, root);
        findAndReplaceSimpleElement(innerCount, root, "id", value.getId(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "name", value.getName(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "url", value.getUrl(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "layout", value.getLayout(), "default", false);
    }

    /**
     * Method updateRepositoryPolicy.
     *
     * @param value The RepositoryPolicy to update
     * @param xmlTag The tag of the parent element
     * @param element The parent element
     * @param counter The counter
     */
    private void updateRepositoryPolicy(RepositoryPolicy value, String xmlTag, Counter counter, Element element) {
        boolean shouldExist = value != null;
        Element root = updateElement(counter, element, xmlTag, shouldExist);
        if (shouldExist) {
            Counter innerCount = counter.newNextDepthLevelCounter();
            findAndReplaceSimpleElement(innerCount, root, "enabled",
                    (value.isEnabled()) ? null : String.valueOf(value.isEnabled()), "true", false);
            findAndReplaceSimpleElement(innerCount, root, "updatePolicy", value.getUpdatePolicy(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "checksumPolicy", value.getChecksumPolicy(), null, false);
        }
    }

    /**
     * Method updateResource.
     *
     * @param value The Resource to update
     * @param counter The counter
     * @param root The parent element
     */
    private void updateResource(Resource value, Counter counter, Element root) {
        Counter innerCount = counter.newNextDepthLevelCounter();
        findAndReplaceSimpleElement(innerCount, root, "targetPath", value.getTargetPath(), null, false);
        findAndReplaceSimpleElement(innerCount, root, "filtering",
                (!value.isFiltering()) ? null : String.valueOf(value.isFiltering()), "false", false);
        findAndReplaceSimpleElement(innerCount, root, "directory", value.getDirectory(), null, false);
        findAndReplaceSimpleLists(innerCount, root, value.getIncludes(), "includes", "include");
        findAndReplaceSimpleLists(innerCount, root, value.getExcludes(), "excludes", "exclude");
    }

    /**
     * Method updateScm.
     *
     * @param value The Scm to update
     * @param counter The counter
     * @param element The parent element
     */
    private void updateScm(Scm value, Counter counter, Element element) {
        boolean shouldExist = value != null;
        Element root = updateElement(counter, element, "scm", shouldExist);
        if (shouldExist) {
            Counter innerCount = counter.newNextDepthLevelCounter();
            findAndReplaceSimpleElement(innerCount, root, "connection", value.getConnection(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "developerConnection", value.getDeveloperConnection(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "tag", value.getTag(), "HEAD", false);
            findAndReplaceSimpleElement(innerCount, root, "url", value.getUrl(), null, false);
            findAndReplaceAttribute(root, "child.scm.connection.inherit.append.path",
                    value.getChildScmConnectionInheritAppendPath(), "true");
            findAndReplaceAttribute(root, "child.scm.developerConnection.inherit.append.path",
                    value.getChildScmUrlInheritAppendPath(), "true");
            findAndReplaceAttribute(root, "child.scm.url.inherit.append.path",
                    value.getChildScmDeveloperConnectionInheritAppendPath(), "true");
        }
    }

    private void findAndReplaceAttribute(Element root, String name, String value, String defaultValue) {
        if (value == null || value.equals(defaultValue)) {
            root.removeAttribute(name);
        } else {
            root.setAttribute(name, value);
        }
    }

    /**
     * Method updateSite.
     *
     * @param value The Site to update
     * @param counter The counter
     * @param element The parent element
     */
    private void updateSite(Site value, Counter counter, Element element) {
        boolean shouldExist = value != null;
        Element root = updateElement(counter, element, "site", shouldExist);
        if (shouldExist) {
            Counter innerCount = counter.newNextDepthLevelCounter();
            findAndReplaceAttribute(root, "child.site.url.inherit.append.path", value.getChildSiteUrlInheritAppendPath(),
                    "true");
            findAndReplaceSimpleElement(innerCount, root, "id", value.getId(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "name", value.getName(), null, false);
            findAndReplaceSimpleElement(innerCount, root, "url", value.getUrl(), null, false);
        }
    }

    // -----------------/
    // - Inner Classes -/
    // -----------------/

    /**
     * Class Counter.
     */
    private static final class Counter {
        // --------------------------/
        // - Class/Member Variables -/
        // --------------------------/

        /**
         * Field currentIndex.
         */
        private int currentIndex;

        /**
         * Field level.
         */
        private final int level;

        // ----------------/
        // - Constructors -/
        // ----------------/

        private Counter(int depthLevel) {
            level = depthLevel;
        }

        public static Counter initialCounter() {
            return new Counter(0);
        }

        // -----------/
        // - Methods -/
        // -----------/

        /**
         * Method getCurrentIndex.
         */
        public int getCurrentIndex() {
            return currentIndex;
        }

        public void setCurrentIndex(int currentIndex) {
            this.currentIndex = currentIndex;
        }

        /**
         * Method getDepth.
         */
        public int getDepth() {
            return level;
        }

        /**
         * Method increaseCount.
         */
        public void increaseCount() {
            currentIndex++;
        }

        /**
         * @return a new counter with a depth increased by 1
         */
        public Counter newNextDepthLevelCounter() {
            return new Counter(getDepth() + 1);
        }
    }
}
