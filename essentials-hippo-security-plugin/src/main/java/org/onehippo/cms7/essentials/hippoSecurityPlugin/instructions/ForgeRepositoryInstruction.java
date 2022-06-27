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

import java.util.Map;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.model.rest.MavenRepository;
import org.onehippo.cms7.essentials.sdk.api.service.MavenModelService;
import org.onehippo.cms7.essentials.sdk.api.service.MavenRepositoryService;

public class ForgeRepositoryInstruction implements Instruction {

    private static final MavenRepository FORGE_REPOSITORY = new MavenRepository();
    static {
        FORGE_REPOSITORY.setId("hippo-forge");
        FORGE_REPOSITORY.setName("Hippo Bloomreach Forge Repository");
        FORGE_REPOSITORY.setUrl("https://maven.onehippo.com/maven2-forge/");
        final MavenRepository.Policy releasePolicy = new MavenRepository.Policy();
        releasePolicy.setUpdatePolicy("never");
        releasePolicy.setChecksumPolicy("fail");
        FORGE_REPOSITORY.setReleasePolicy(releasePolicy);

    }

    @Inject
    private MavenRepositoryService repositoryService;


    @Override
    public Status execute(final Map<String, Object> parameters) {
        if (!repositoryService.addRepository(Module.PROJECT, FORGE_REPOSITORY)) {
            return Status.FAILED;
        }
        return Status.SUCCESS;
    }

    @Override
    public void populateChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(Type.EXECUTE, "Add the Forge repository to the project root POM.");
    }
}
