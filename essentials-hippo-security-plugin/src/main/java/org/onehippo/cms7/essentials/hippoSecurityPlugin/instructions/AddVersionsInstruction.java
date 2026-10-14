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
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;

import jakarta.inject.Inject;

import org.apache.maven.model.Model;
import org.onehippo.cms7.essentials.plugin.sdk.utils.MavenModelUtils;
import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddVersionsInstruction implements Instruction {

    private static final Logger log = LoggerFactory.getLogger(AddVersionsInstruction.class);

    // spring-security.version is not added: it is managed by the brXM parent pom, in line with its Spring Framework
    static final String HST_SPRINGSEC_VERSION = "hst-springsec.version";

    @Inject
    private ProjectService projectService;

    @Override
    public Status execute(final Map<String, Object> parameters) {
        final File pom = projectService.getPomPathForModule(Module.PROJECT).toFile();
        final Model pomModel = MavenModelUtils.readPom(pom);
        if (pomModel == null) {
            return Status.FAILED;
        }
        final Properties versions = new Properties();
        try (final InputStream resourceAsStream = getClass().getResourceAsStream("/version.properties")) {
            versions.load(resourceAsStream);
        } catch (IOException e) {
            log.error("Error executing versions instruction:", e);
            return Status.FAILED;
        }
        pomModel.getProperties().setProperty(HST_SPRINGSEC_VERSION, versions.getProperty(HST_SPRINGSEC_VERSION));

        return MavenModelUtils.writePom(pomModel, pom) ? Status.SUCCESS : Status.FAILED;
    }

    @Override
    public void populateChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(Type.EXECUTE, "Add the HST Spring Security Support version to the project POM.");
    }
}
