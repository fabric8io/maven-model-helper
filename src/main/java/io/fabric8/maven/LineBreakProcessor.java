package io.fabric8.maven;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.jdom2.Element;
import org.jdom2.output.support.AbstractXMLOutputProcessor;
import org.jdom2.output.support.FormatStack;
import org.jdom2.util.NamespaceStack;

/**
 * Inserts a line break on every major section
 */
class LineBreakProcessor extends AbstractXMLOutputProcessor {

    private String buffer;

    @Override
    protected void printElement(Writer out, FormatStack fstack, NamespaceStack nstack, Element element) throws IOException {
        Element nextSiblingElement = findNextSiblingElement(element);
        boolean shouldBreakLine = false;
        if (nextSiblingElement != null) {
            shouldBreakLine = shouldBreakLine(nextSiblingElement);
        }
        super.printElement(out, fstack, nstack, element);
        buffer = (shouldBreakLine) ? fstack.getLineSeparator() : null;
    }

    private Element findNextSiblingElement(Element element) {
        Element parent = element.getParentElement();
        if (parent != null) {
            List<Element> children = parent.getChildren();
            int idx = children.indexOf(element);
            if (idx < children.size() - 1) {
                return children.get(idx + 1);
            }
        }
        return null;
    }

    @Override
    protected void textRaw(Writer out, String str) throws IOException {
        if (buffer != null) {
            out.write(buffer);
            buffer = null;
        }
        super.textRaw(out, str);
    }

    private static boolean shouldBreakLine(Element element) {
        Element rootElement = element.getDocument().getRootElement();
        // Test if it's a major section
        int idxElement = rootElement.indexOf(element);
        List<Element> children = element.getChildren();
        return (idxElement > -1 && !children.isEmpty());
    }
}
