/*
 * Copyright 2026 Bloomreach, Inc. (https://www.bloomreach.com)
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

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.onehippo.cms7.essentials.plugin.sdk.services.WebXmlServiceImpl;
import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs {@link FilterMappingsInstruction} against the brXM 17 archetype site {@code web.xml}.
 *
 * <p>Regression for FORGE-434: the instruction re-indented the whole file, leaving a whitespace-only line before
 * nearly every element, and added the context-param, listener and filter mapping again on every run.</p>
 */
class FilterMappingsInstructionTest {

    private static final String SPRING_LISTENER = "<listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>";
    private static final String HST_LISTENER = "<listener-class>org.hippoecm.hst.site.container.HstContextLoaderListener</listener-class>";

    @TempDir
    Path webInf;

    private FilterMappingsInstruction instruction;

    @BeforeEach
    void setUp() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/archetype/web.xml")) {
            Files.copy(in, webInf.resolve("web.xml"), StandardCopyOption.REPLACE_EXISTING);
        }
        final ProjectService projectService = stub(ProjectService.class, webInf);
        instruction = new FilterMappingsInstruction();
        inject(instruction, "webXmlService", new WebXmlServiceImpl(projectService));
        inject(instruction, "projectService", projectService);
    }

    @Test
    void addsSpringSecurityFilterBeforeHstFilter() throws Exception {
        instruction.execute(Collections.emptyMap());

        final String webXml = Files.readString(webInf.resolve("web.xml"));
        assertEquals(1, count(webXml, "<filter-class>org.springframework.web.filter.DelegatingFilterProxy</filter-class>"));
        assertEquals(1, count(webXml, "<filter-mapping>\n    <filter-name>springSecurityFilterChain</filter-name>"));
        assertTrue(webXml.indexOf("<filter-mapping>\n    <filter-name>springSecurityFilterChain</filter-name>")
                        < webXml.indexOf("<filter-mapping>\n    <filter-name>HstFilter</filter-name>"),
                "springSecurityFilterChain must be mapped before the HstFilter");
    }

    @Test
    void addsContextParamAndListenerWithoutReformatting() throws Exception {
        assertEquals(Instruction.Status.SUCCESS, instruction.execute(Collections.emptyMap()));

        final List<String> lines = Files.readAllLines(webInf.resolve("web.xml"));
        assertTrue(lines.stream().noneMatch(line -> !line.isEmpty() && line.isBlank()),
                "web.xml must not contain whitespace-only lines");

        final String webXml = String.join("\n", lines);
        assertEquals(1, count(webXml, "<param-name>contextConfigLocation</param-name>"));
        assertEquals(1, count(webXml, SPRING_LISTENER));
        assertTrue(webXml.contains("\n  <context-param>\n    <param-name>contextConfigLocation</param-name>"),
                "context-param must be indented like its siblings");
        assertTrue(webXml.indexOf(SPRING_LISTENER) < webXml.indexOf(HST_LISTENER),
                "Spring ContextLoaderListener must be registered before the HstContextLoaderListener");
    }

    @Test
    void runningTwiceDoesNotDuplicateOrReformat() throws Exception {
        instruction.execute(Collections.emptyMap());
        final String afterFirstRun = Files.readString(webInf.resolve("web.xml"));

        instruction.execute(Collections.emptyMap());

        assertEquals(afterFirstRun, Files.readString(webInf.resolve("web.xml")));
    }

    private static int count(final String text, final String fragment) {
        int count = 0;
        for (int i = text.indexOf(fragment); i >= 0; i = text.indexOf(fragment, i + 1)) {
            count++;
        }
        return count;
    }

    private static void inject(final Object target, final String fieldName, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(final Class<T> type, final Path webInf) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) ->
                switch (method.getName()) {
                    case "getWebInfPathForModule" -> args[0] == Module.SITE_WEBAPP ? webInf : null;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> type.getSimpleName() + " stub";
                    default -> method.getReturnType() == boolean.class ? Boolean.TRUE : null;
                });
    }
}
