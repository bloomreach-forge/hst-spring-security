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
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import jakarta.inject.Inject;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.onehippo.cms7.essentials.plugin.sdk.utils.MavenModelUtils;
import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the Spring Security and HST Spring Security Support versions in the project POM, excluding the Spring
 * Framework artifacts already provided by the platform. The site components then depend on them without a version.
 */
public class DependencyManagementInstruction implements Instruction {
    private static final Logger log = LoggerFactory.getLogger(DependencyManagementInstruction.class);

    private static final String SPRING_SECURITY_GROUP_ID = "org.springframework.security";
    // Defined by the brXM parent pom, so Spring Security matches the platform's Spring Framework version
    private static final String SPRING_SECURITY_VERSION = "${spring-security.version}";
    private static final String SPRING_GROUP_ID = "org.springframework";

    private static final List<ManagedDependency> MANAGED_DEPENDENCIES = List.of(
            new ManagedDependency(SPRING_SECURITY_GROUP_ID, "spring-security-core", SPRING_SECURITY_VERSION,
                    "spring-aop", "spring-beans", "spring-context", "spring-core", "spring-expression"),
            new ManagedDependency(SPRING_SECURITY_GROUP_ID, "spring-security-web", SPRING_SECURITY_VERSION,
                    "spring-beans", "spring-context", "spring-core", "spring-expression", "spring-web"),
            new ManagedDependency(SPRING_SECURITY_GROUP_ID, "spring-security-config", SPRING_SECURITY_VERSION,
                    "spring-aop", "spring-beans", "spring-context", "spring-core"),
            new ManagedDependency("org.onehippo.forge.hst-springsec", "hst-springsec",
                    "${" + AddVersionsInstruction.HST_SPRINGSEC_VERSION + "}"));

    @Inject
    private ProjectService projectService;

    @Override
    public Status execute(final Map<String, Object> parameters) {
        final File pom = projectService.getPomPathForModule(Module.PROJECT).toFile();
        final Model model = MavenModelUtils.readPom(pom);
        if (model == null) {
            return Status.FAILED;
        }
        if (model.getDependencyManagement() == null) {
            model.setDependencyManagement(new DependencyManagement());
        }
        final DependencyManagement dependencyManagement = model.getDependencyManagement();

        boolean changed = false;
        for (ManagedDependency managedDependency : MANAGED_DEPENDENCIES) {
            changed |= managedDependency.addTo(dependencyManagement);
        }
        if (changed && !MavenModelUtils.writePom(model, pom)) {
            log.error("Failed to add managed Spring Security dependencies to {}", pom);
            return Status.FAILED;
        }
        return Status.SUCCESS;
    }

    @Override
    public void populateChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(Type.EXECUTE, "Add managed Spring Security dependencies to the project POM.");
    }

    private record ManagedDependency(String groupId, String artifactId, String version, String... springExclusions) {

        /**
         * Adds this dependency to the dependency management, or the missing Spring exclusions to an existing entry,
         * keeping any version the project already manages.
         *
         * @return whether the dependency management was changed
         */
        boolean addTo(final DependencyManagement dependencyManagement) {
            Dependency dependency = dependencyManagement.getDependencies().stream()
                    .filter(existing -> groupId.equals(existing.getGroupId()) && artifactId.equals(existing.getArtifactId()))
                    .findFirst()
                    .orElse(null);
            boolean changed = false;
            if (dependency == null) {
                dependency = new Dependency();
                dependency.setGroupId(groupId);
                dependency.setArtifactId(artifactId);
                dependency.setVersion(version);
                dependencyManagement.addDependency(dependency);
                changed = true;
            }
            for (String exclusionArtifactId : springExclusions) {
                if (dependency.getExclusions().stream().noneMatch(exclusion -> SPRING_GROUP_ID.equals(exclusion.getGroupId())
                        && exclusionArtifactId.equals(exclusion.getArtifactId()))) {
                    final Exclusion exclusion = new Exclusion();
                    exclusion.setGroupId(SPRING_GROUP_ID);
                    exclusion.setArtifactId(exclusionArtifactId);
                    dependency.addExclusion(exclusion);
                    changed = true;
                }
            }
            return changed;
        }
    }
}
