/*
 * Copyright 2024 Bloomreach, Inc. (https://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.essentials.hippoSecurityPlugin;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the essentials plugin's bundled {@code applicationContext-security.xml}
 * template contains both {@code /binaries/**} and {@code /content/**} patterns with
 * {@code security="none"}.
 *
 * <p>Regression for FORGE-559: the template was missing the {@code /content/**} pattern.
 * The {@code /binaries} servlet internally translates requests to {@code /content/**}, so
 * both patterns must be explicitly excluded from Spring Security filtering.</p>
 */
class EssentialsPluginTemplatePatternTest {

    private static List<String> securityNonePatterns;

    @BeforeAll
    static void parseTemplate() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        // Disable external entity resolution to prevent XXE
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        // siteFiles are not on the classpath — they are template files copied to the site
        // application during plugin installation. Load directly from the source tree.
        java.io.File templateFile = new java.io.File(
                "src/main/resources/siteFiles/applicationContext-security.xml");
        assertTrue(templateFile.exists(),
                "Template file not found: " + templateFile.getAbsolutePath());
        try (InputStream in = new java.io.FileInputStream(templateFile)) {
            Document doc = builder.parse(in);

            securityNonePatterns = new ArrayList<>();
            NodeList httpElements = doc.getElementsByTagName("http");
            for (int i = 0; i < httpElements.getLength(); i++) {
                Element el = (Element) httpElements.item(i);
                String security = el.getAttribute("security");
                String pattern = el.getAttribute("pattern");
                if ("none".equals(security) && !pattern.isEmpty()) {
                    securityNonePatterns.add(pattern);
                }
            }
        }
    }

    @Test
    void templateContainsBinariesNonePattern() {
        assertTrue(securityNonePatterns.contains("/binaries/**"),
                "Template must declare <http pattern=\"/binaries/**\" security=\"none\"/>");
    }

    @Test
    void templateContainsContentNonePattern() {
        // The /binaries servlet internally translates to /content/**, so both must be excluded.
        assertTrue(securityNonePatterns.contains("/content/**"),
                "Template must declare <http pattern=\"/content/**\" security=\"none\"/> "
                        + "(internal path translation from /binaries/** servlet)");
    }

    @Test
    void templateContainsCssNonePattern() {
        assertTrue(securityNonePatterns.contains("/css/**"),
                "Template must declare <http pattern=\"/css/**\" security=\"none\"/>");
    }

    @Test
    void templateContainsWebfilesNonePattern() {
        assertTrue(securityNonePatterns.contains("/webfiles/**"),
                "Template must declare <http pattern=\"/webfiles/**\" security=\"none\"/>");
    }
}
