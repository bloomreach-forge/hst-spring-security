/*
 * Copyright 2022 Bloomreach Inc. (https://www.bloomreach.com)
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

package org.onehippo.cms7.essentials.hippoSecurityPlugin.rest;

import javax.inject.Inject;
import javax.jcr.Session;

import org.onehippo.cms7.essentials.sdk.api.service.ContentTypeService;
import org.onehippo.cms7.essentials.sdk.api.service.JcrService;
import org.onehippo.cms7.essentials.sdk.api.service.PlaceholderService;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.onehippo.cms7.essentials.sdk.api.service.SettingsService;
import org.onehippo.cms7.essentials.sdk.api.service.TemplateQueryService;
import org.springframework.stereotype.Component;

@Component
public class PluginContext {

    private final JcrService jcrService;
    private final PlaceholderService placeholderService;
    private final ProjectService projectService;
    private final ContentTypeService contentTypeService;
    private final TemplateQueryService templateQueryService;
    private final SettingsService settingsService;


    @Inject
    public PluginContext(final JcrService jcrService, final PlaceholderService placeholderService, final ProjectService projectService, final ContentTypeService contentTypeService, final TemplateQueryService templateQueryService, final SettingsService settingsService) {
        this.jcrService = jcrService;
        this.placeholderService = placeholderService;
        this.projectService = projectService;
        this.contentTypeService = contentTypeService;
        this.templateQueryService = templateQueryService;
        this.settingsService = settingsService;
    }

    public JcrService getJcrService() {
        return jcrService;
    }

    public PlaceholderService getPlaceholderService() {
        return placeholderService;
    }

    public ProjectService getProjectService() {
        return projectService;
    }

    public ContentTypeService getContentTypeService() {
        return contentTypeService;
    }

    public TemplateQueryService getTemplateQueryService() {
        return templateQueryService;
    }

    public SettingsService getSettingsService() {
        return settingsService;
    }

    public Session createSession() {
        return jcrService.createSession();
    }

    public String getNamespace() {
        return settingsService.getSettings().getProjectNamespace();
    }
}
