package io.fabric8.maven;

import org.jdom2.Element;
import org.jdom2.output.support.AbstractXMLOutputProcessor;
import org.jdom2.output.support.FormatStack;
import org.jdom2.util.NamespaceStack;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Inserts a line break on every major section
 */
class LineBreakProcessor extends AbstractXMLOutputProcessor {

    @Override
    protected void printElement(Writer out, FormatStack fstack, NamespaceStack nstack, Element element) throws IOException {
        Element rootElement = element.getDocument().getRootElement();
        // Test if it's a major section
        int idxElement = rootElement.indexOf(element);
        List<Element> children = element.getChildren();
        boolean addBreak = (idxElement > -1 && !children.isEmpty());
        super.printElement(out, fstack, nstack, element);
        if (addBreak) {
            out.write(fstack.getLineSeparator());
        }
    }
}
