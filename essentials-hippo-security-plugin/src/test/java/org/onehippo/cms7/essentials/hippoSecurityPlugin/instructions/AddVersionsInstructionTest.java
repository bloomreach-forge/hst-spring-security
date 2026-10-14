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

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.onehippo.cms7.essentials.plugin.sdk.utils.MavenModelUtils;
import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Runs {@link AddVersionsInstruction} against the brXM 17 archetype project pom.
 */
class AddVersionsInstructionTest {

    @TempDir
    Path projectDir;

    private File pom;
    private AddVersionsInstruction instruction;

    @BeforeEach
    void setUp() throws Exception {
        pom = projectDir.resolve("pom.xml").toFile();
        try (InputStream in = getClass().getResourceAsStream("/archetype/pom.xml")) {
            Files.copy(in, pom.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        instruction = new AddVersionsInstruction();
        final Field field = AddVersionsInstruction.class.getDeclaredField("projectService");
        field.setAccessible(true);
        field.set(instruction, Proxy.newProxyInstance(ProjectService.class.getClassLoader(),
                new Class<?>[]{ProjectService.class}, (proxy, method, args) ->
                        "getPomPathForModule".equals(method.getName()) && args[0] == Module.PROJECT ? pom.toPath() : null));
    }

    @Test
    void addsPluginVersion() {
        assertEquals(Instruction.Status.SUCCESS, instruction.execute(Collections.emptyMap()));

        final Properties properties = MavenModelUtils.readPom(pom).getProperties();
        assertNotNull(properties.getProperty("hst-springsec.version"));
    }

    @Test
    void leavesSpringSecurityVersionToTheBrxmParent() {
        instruction.execute(Collections.emptyMap());

        // Spring Security must match the Spring Framework version the platform provides
        assertFalse(MavenModelUtils.readPom(pom).getProperties().containsKey("spring-security.version"),
                "spring-security.version is managed by the brXM parent pom");
    }
}
