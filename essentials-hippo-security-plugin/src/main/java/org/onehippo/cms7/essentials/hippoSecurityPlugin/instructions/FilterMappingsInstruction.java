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

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.inject.Inject;

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
import org.onehippo.cms7.essentials.sdk.api.service.WebXmlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterMappingsInstruction implements Instruction {
    private static final Logger log = LoggerFactory.getLogger(FilterMappingsInstruction.class);
    private static final String FILTER_CLASS = "org.springframework.web.filter.DelegatingFilterProxy";
    private static final String FILTER_NAME = "springSecurityFilterChain";
    private static final List<String> URL_PATTERNS = Collections.singletonList("/*");
    private static final List<WebXmlService.Dispatcher> DISPATCHERS = Collections.singletonList(WebXmlService.Dispatcher.REQUEST);
    private static final Module MODULE = Module.SITE_WEBAPP;
    private static final Map<String, String> initParams = new HashMap<>();
    public static final String HST_FILTER = "HstFilter";


    @Inject
    private WebXmlService webXmlService;

    @Inject
    private ProjectService projectService;
    @Override
    public Status execute(final Map<String, Object> parameters) {
        try {
            // filter
            webXmlService.addFilter(MODULE, FILTER_NAME, FILTER_CLASS, initParams);
            webXmlService.insertFilterMapping(MODULE, FILTER_NAME, URL_PATTERNS, HST_FILTER);
            webXmlService.addDispatchersToFilterMapping(MODULE, HST_FILTER, DISPATCHERS);
            webXmlService.addDispatchersToFilterMapping(MODULE, FILTER_NAME, DISPATCHERS);
            // context
            final File webXml = projectService.getWebInfPathForModule(MODULE).resolve("web.xml").toFile();
            final Document doc = new SAXReader().read(webXml);
            final Node node = doc.selectSingleNode("/web-app");

            if (node instanceof Element) {
                final Element element = (Element) node;
                final Namespace namespace = element.getNamespace();
                final Element context = new BaseElement("context-param", namespace);
                final Element paramName = new BaseElement("param-name", namespace);
                paramName.setText("contextConfigLocation");
                context.add(paramName);
                final Element paramValue = new BaseElement("param-value", namespace);
                paramValue.setText("\n      /WEB-INF/applicationContext.xml\n" +
                        "      /WEB-INF/applicationContext-security.xml");
                context.add(paramValue);

                // listener
                final Element listener = new BaseElement("listener", namespace);
                final Element clazzElement = new BaseElement("listener-class", namespace);
                clazzElement.setText("org.springframework.web.context.ContextLoaderListener");
                listener.add(clazzElement);
                //
                element.elements().add(3, listener);
                element.elements().add(3, context);


            }
            Utils.prettyPrint(webXml, doc.asXML().getBytes());


        } catch (Exception e) {
            log.error("Error executing file mappings instruction:", e);
        }


        return Status.SUCCESS;
    }

    @Override
    public void populateChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(Type.EXECUTE, "Install Spring Security filter into Site web.xml.");
    }
}
