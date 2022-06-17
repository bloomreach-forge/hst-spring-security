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
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.onehippo.cms7.essentials.plugin.sdk.utils.MavenModelUtils;
import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.model.rest.MavenRepository;
import org.onehippo.cms7.essentials.sdk.api.service.MavenModelService;
import org.onehippo.cms7.essentials.sdk.api.service.MavenRepositoryService;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddVersionsInstruction implements Instruction {

    private static final Logger log = LoggerFactory.getLogger(AddVersionsInstruction.class);

    @Inject
    private ProjectService projectService;

    @Override
    public Status execute(final Map<String, Object> parameters) {
        final File pom = projectService.getPomPathForModule(Module.PROJECT).toFile();
        final Model pomModel = MavenModelUtils.readPom(pom);
        if (pomModel == null) {
            return Status.FAILED;
        }
        java.util.Properties javaProperties = new Properties();
        try (final InputStream resourceAsStream = getClass().getResourceAsStream("/version.properties")) {
            javaProperties.load(resourceAsStream);
            final Properties properties = pomModel.getProperties();
            properties.setProperty("hippo.forge.hst-springsec.version", javaProperties.getProperty("hippo.forge.hst-springsec.version"));
            properties.setProperty("spring.security.version", javaProperties.getProperty("spring.security.version"));

        } catch (IOException e) {
            log.error("Error executing versions instruction:", e);
        }

        MavenModelUtils.writePom(pomModel, pom);
        return Status.SUCCESS;
    }

    @Override
    public void populateChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(Type.EXECUTE, "Add spring and plugin versions");
    }
}
