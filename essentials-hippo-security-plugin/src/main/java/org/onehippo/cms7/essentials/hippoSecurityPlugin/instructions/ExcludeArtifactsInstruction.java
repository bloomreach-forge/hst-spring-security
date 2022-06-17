/*
 * Copyright 2018-2022 Bloomreach Inc. (https://www.bloomreach.com)
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

package org.onehippo.cms7.essentials.hippoSecurityPlugin.instructions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.BaseElement;
import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

public class ExcludeArtifactsInstruction implements Instruction {
    private static final Logger log = LoggerFactory.getLogger(ExcludeArtifactsInstruction.class);


    static final Multimap<String, Map.Entry<String, String>> EXCLUDE_MAPPINGS =
            new ImmutableMultimap.Builder<String, Map.Entry<String, String>>()
                    .put("spring-security-core", new AbstractMap.SimpleEntry<>("org.springframework", "spring-aop"))
                    .put("spring-security-core", new AbstractMap.SimpleEntry<>("org.springframework", "spring-expression"))
                    .put("spring-security-core", new AbstractMap.SimpleEntry<>("org.springframework", "spring-core"))
                    .put("spring-security-core", new AbstractMap.SimpleEntry<>("org.springframework", "spring-beans"))
                    .put("spring-security-core", new AbstractMap.SimpleEntry<>("org.springframework", "spring-context"))
                    //
                    .put("spring-security-web", new AbstractMap.SimpleEntry<>("org.springframework", "spring-web"))
                    .put("spring-security-web", new AbstractMap.SimpleEntry<>("org.springframework", "spring-expression"))
                    .put("spring-security-web", new AbstractMap.SimpleEntry<>("org.springframework", "spring-context"))
                    .put("spring-security-web", new AbstractMap.SimpleEntry<>("org.springframework", "spring-core"))
                    .put("spring-security-web", new AbstractMap.SimpleEntry<>("org.springframework", "spring-beans"))
                    //
                    .put("spring-security-config", new AbstractMap.SimpleEntry<>("org.springframework", "spring-aop"))
                    .put("spring-security-config", new AbstractMap.SimpleEntry<>("org.springframework", "spring-context"))
                    .put("spring-security-config", new AbstractMap.SimpleEntry<>("org.springframework", "spring-core"))
                    .put("spring-security-config", new AbstractMap.SimpleEntry<>("org.springframework", "spring-beans"))
                    //
                    .put("spring-security-taglibs", new AbstractMap.SimpleEntry<>("org.springframework", "spring-aop"))
                    .put("spring-security-taglibs", new AbstractMap.SimpleEntry<>("org.springframework", "spring-context"))
                    .put("spring-security-taglibs", new AbstractMap.SimpleEntry<>("org.springframework", "spring-core"))
                    .put("spring-security-taglibs", new AbstractMap.SimpleEntry<>("org.springframework", "spring-beans"))
                    .put("spring-security-taglibs", new AbstractMap.SimpleEntry<>("org.springframework", "spring-web"))
                    .put("spring-security-taglibs", new AbstractMap.SimpleEntry<>("org.springframework", "spring-tx"))
                    .put("spring-security-taglibs", new AbstractMap.SimpleEntry<>("org.springframework", "spring-jdbc"))
                    .put("spring-security-taglibs", new AbstractMap.SimpleEntry<>("org.springframework", "spring-expression"))

                    .build();
    @Inject
    private ProjectService projectService;


    @Override
    public Status execute(final Map<String, Object> map) {

        try {
            final File pomFile = projectService.getPomPathForModule(Module.SITE_WEBAPP).toFile();
            final Document doc = new SAXReader().read(pomFile);
            for (Map.Entry<String, Map.Entry<String, String>> entry : EXCLUDE_MAPPINGS.entries()) {
                final String artifactId = entry.getKey();
                final Map.Entry<String, String> value = entry.getValue();
                addExclusion(doc, artifactId, value.getKey(), value.getValue());
            }
            Utils.prettyPrint(pomFile, doc.asXML().getBytes());
            return Status.SUCCESS;
        } catch (Exception e) {
            log.error("Error executing artifacts instruction:", e);
        }
        return Status.FAILED;
    }

    @Override
    public void populateChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(Type.EXECUTE, "Adding exclusion to maven artifacts");
    }

    private void addExclusion(final Document doc, final String artifactId, final String excludeGroupId, final String excludeArtifactId) {
        final Node node = doc.selectSingleNode("//*[name()='artifactId' and text() ='" + artifactId + "']");
        if (node != null && node.getParent() != null) {
            final Element element = node.getParent();
            final Namespace namespace = element.getNamespace();
            Element exclusions = element.element("exclusions");
            if (exclusions == null) {
                exclusions = new BaseElement("exclusions", namespace);
                element.add(exclusions);
            }
            final Element exclusion = new BaseElement("exclusion", namespace);
            final Element artifactElement = new BaseElement("artifactId", namespace);
            artifactElement.setText(excludeArtifactId);
            final Element groupElement = new BaseElement("groupId", namespace);
            groupElement.setText(excludeGroupId);
            exclusion.add(groupElement);
            exclusion.add(artifactElement);
            exclusions.add(exclusion);
        }

    }



}
