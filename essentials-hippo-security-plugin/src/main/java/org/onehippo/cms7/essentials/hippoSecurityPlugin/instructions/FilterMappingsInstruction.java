/*
 * Copyright 2018-2026 Bloomreach Inc. (https://www.bloomreach.com)
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

import jakarta.inject.Inject;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.onehippo.cms7.essentials.plugin.sdk.utils.Dom4JUtils;
import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.onehippo.cms7.essentials.sdk.api.service.WebXmlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class FilterMappingsInstruction implements Instruction {
    private static final Logger log = LoggerFactory.getLogger(FilterMappingsInstruction.class);
    private static final String FILTER_CLASS = "org.springframework.web.filter.DelegatingFilterProxy";
    private static final String FILTER_NAME = "springSecurityFilterChain";
    private static final List<String> URL_PATTERNS = Collections.singletonList("/*");
    private static final List<WebXmlService.Dispatcher> DISPATCHERS = Collections.singletonList(WebXmlService.Dispatcher.REQUEST);
    private static final Module MODULE = Module.SITE_WEBAPP;
    private static final Map<String, String> initParams = new HashMap<>();
    public static final String HST_FILTER = "HstFilter";

    private static final String CONTEXT_CONFIG_LOCATION = "contextConfigLocation";
    private static final String CONTEXT_CONFIG_LOCATION_VALUE = "\n      /WEB-INF/applicationContext.xml\n" +
            "      /WEB-INF/applicationContext-security.xml\n    ";
    private static final String LISTENER_CLASS = "org.springframework.web.context.ContextLoaderListener";
    private static final String HST_LISTENER_CLASS = "org.hippoecm.hst.site.container.HstContextLoaderListener";

    // web.xml uses a default namespace, so match elements by local name
    private static final String CONTEXT_PARAM_SELECTOR = "*[local-name()='context-param']"
            + "[*[local-name()='param-name']='" + CONTEXT_CONFIG_LOCATION + "']";
    private static final String LISTENER_SELECTOR = "*[local-name()='listener'][*[local-name()='listener-class']='%s']";
    private static final String FIRST_FILTER_SELECTOR = "*[local-name()='filter'][1]";
    private static final String FILTER_MAPPING_SELECTOR = "*[local-name()='filter-mapping']"
            + "[*[local-name()='filter-name']='" + FILTER_NAME + "']";


    @Inject
    private WebXmlService webXmlService;

    @Inject
    private ProjectService projectService;
    @Override
    public Status execute(final Map<String, Object> parameters) {
        final File webXml = projectService.getWebInfPathForModule(MODULE).resolve("web.xml").toFile();

        // filter; the SDK does not check for an existing filter mapping, so only insert it once
        webXmlService.addFilter(MODULE, FILTER_NAME, FILTER_CLASS, initParams);
        if (!hasFilterMapping(webXml)) {
            webXmlService.insertFilterMapping(MODULE, FILTER_NAME, URL_PATTERNS, HST_FILTER);
        }
        webXmlService.addDispatchersToFilterMapping(MODULE, HST_FILTER, DISPATCHERS);
        webXmlService.addDispatchersToFilterMapping(MODULE, FILTER_NAME, DISPATCHERS);

        // Spring root application context, loaded before the HST container
        final boolean updated = Dom4JUtils.update(webXml, doc -> {
            final Element webApp = doc.getRootElement();
            if (webApp.selectSingleNode(CONTEXT_PARAM_SELECTOR) == null) {
                final Element contextParam = Dom4JUtils.insertIndentedElement(webApp, "context-param", FIRST_FILTER_SELECTOR);
                Dom4JUtils.addIndentedElement(contextParam, "param-name", CONTEXT_CONFIG_LOCATION);
                Dom4JUtils.addIndentedElement(contextParam, "param-value", CONTEXT_CONFIG_LOCATION_VALUE);
            }
            if (webApp.selectSingleNode(String.format(LISTENER_SELECTOR, LISTENER_CLASS)) == null) {
                final Element listener = Dom4JUtils.insertIndentedElement(webApp, "listener",
                        String.format(LISTENER_SELECTOR, HST_LISTENER_CLASS));
                Dom4JUtils.addIndentedElement(listener, "listener-class", LISTENER_CLASS);
            }
        });
        if (!updated) {
            log.error("Failed to add the Spring application context configuration to {}", webXml);
            return Status.FAILED;
        }
        return Status.SUCCESS;
    }

    private static boolean hasFilterMapping(final File webXml) {
        try {
            final SAXReader reader = new SAXReader();
            reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return reader.read(webXml).getRootElement().selectSingleNode(FILTER_MAPPING_SELECTOR) != null;
        } catch (DocumentException | SAXException e) {
            log.error("Failed to read {}", webXml, e);
            return false;
        }
    }

    @Override
    public void populateChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(Type.EXECUTE, "Install Spring Security filter into Site web.xml.");
    }
}
