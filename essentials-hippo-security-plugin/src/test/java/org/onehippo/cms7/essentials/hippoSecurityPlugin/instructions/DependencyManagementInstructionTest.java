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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.onehippo.cms7.essentials.plugin.sdk.utils.MavenModelUtils;
import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs {@link DependencyManagementInstruction} against the brXM 17 archetype project pom and checks that the
 * result matches the demo project: managed versions plus Spring exclusions in the project
 * {@code dependencyManagement}.
 */
class DependencyManagementInstructionTest {

    @TempDir
    Path projectDir;

    private File pom;
    private DependencyManagementInstruction instruction;

    @BeforeEach
    void setUp() throws Exception {
        pom = projectDir.resolve("pom.xml").toFile();
        try (InputStream in = getClass().getResourceAsStream("/archetype/pom.xml")) {
            Files.copy(in, pom.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        instruction = new DependencyManagementInstruction();
        final Field field = DependencyManagementInstruction.class.getDeclaredField("projectService");
        field.setAccessible(true);
        field.set(instruction, Proxy.newProxyInstance(ProjectService.class.getClassLoader(),
                new Class<?>[]{ProjectService.class}, (proxy, method, args) ->
                        "getPomPathForModule".equals(method.getName()) && args[0] == Module.PROJECT ? pom.toPath() : null));
    }

    @Test
    void addsManagedDependenciesLikeTheDemo() {
        assertEquals(Instruction.Status.SUCCESS, instruction.execute(Collections.emptyMap()));

        final Map<String, Dependency> managed = managedDependencies();
        assertDependency(managed.get("spring-security-core"), "${spring-security.version}",
                "spring-aop", "spring-beans", "spring-context", "spring-core", "spring-expression");
        assertDependency(managed.get("spring-security-web"), "${spring-security.version}",
                "spring-beans", "spring-context", "spring-core", "spring-expression", "spring-web");
        assertDependency(managed.get("spring-security-config"), "${spring-security.version}",
                "spring-aop", "spring-beans", "spring-context", "spring-core");
        assertDependency(managed.get("hst-springsec"), "${hst-springsec.version}");
        assertNull(managed.get("spring-security-taglibs"), "taglibs are no longer used");
    }

    @Test
    void doesNotIntroduceWhitespaceOnlyLines() throws Exception {
        instruction.execute(Collections.emptyMap());

        final List<String> lines = Files.readAllLines(pom.toPath());
        assertTrue(lines.stream().noneMatch(line -> !line.isEmpty() && line.isBlank()),
                "pom.xml must not contain whitespace-only lines");
    }

    @Test
    void runningTwiceDoesNotChangeThePom() throws Exception {
        instruction.execute(Collections.emptyMap());
        final String afterFirstRun = Files.readString(pom.toPath());

        instruction.execute(Collections.emptyMap());

        assertEquals(afterFirstRun, Files.readString(pom.toPath()));
    }

    @Test
    void keepsExistingManagedDependencyAndAddsMissingExclusions() {
        final Model model = MavenModelUtils.readPom(pom);
        final Dependency existing = new Dependency();
        existing.setGroupId("org.springframework.security");
        existing.setArtifactId("spring-security-core");
        existing.setVersion("7.0.5");
        final Exclusion exclusion = new Exclusion();
        exclusion.setGroupId("org.springframework");
        exclusion.setArtifactId("spring-aop");
        existing.addExclusion(exclusion);
        model.getDependencyManagement().addDependency(existing);
        MavenModelUtils.writePom(model, pom);

        instruction.execute(Collections.emptyMap());

        final Map<String, Dependency> managed = managedDependencies();
        assertEquals(1, MavenModelUtils.readPom(pom).getDependencyManagement().getDependencies().stream()
                .filter(dependency -> "spring-security-core".equals(dependency.getArtifactId())).count());
        assertDependency(managed.get("spring-security-core"), "7.0.5",
                "spring-aop", "spring-beans", "spring-context", "spring-core", "spring-expression");
    }

    private Map<String, Dependency> managedDependencies() {
        return MavenModelUtils.readPom(pom).getDependencyManagement().getDependencies().stream()
                .collect(Collectors.toMap(Dependency::getArtifactId, dependency -> dependency, (a, b) -> a));
    }

    private static void assertDependency(final Dependency dependency, final String version, final String... springExclusions) {
        assertTrue(dependency != null, "managed dependency missing");
        assertEquals(version, dependency.getVersion());
        final List<String> exclusions = dependency.getExclusions().stream()
                .peek(exclusion -> assertEquals("org.springframework", exclusion.getGroupId()))
                .map(Exclusion::getArtifactId)
                .sorted()
                .collect(Collectors.toList());
        assertEquals(List.of(springExclusions), exclusions, dependency.getArtifactId() + " exclusions");
    }
}
