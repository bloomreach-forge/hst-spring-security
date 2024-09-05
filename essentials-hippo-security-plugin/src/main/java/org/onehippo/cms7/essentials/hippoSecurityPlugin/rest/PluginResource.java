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

import javax.jcr.Session;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import org.onehippo.cms7.essentials.sdk.api.model.rest.UserFeedback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("hippoSecurityPlugin")
public class PluginResource {

    private static final Logger log = LoggerFactory.getLogger(PluginResource.class);

    final PluginContext context;

    public PluginResource(final PluginContext context) {
        this.context = context;
    }

    @GET
    @Path("/")
    public PluginData initialize(@Context ServletContext servletContext, @Context HttpServletResponse response) throws Exception {
        log.info("Successfully executed myPlugin GET endpoint");
        final Session session = context.createSession();
        try {
            final PluginData pluginData = new PluginData();
            if (session == null) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                log.error("No session found");
                return pluginData;
            }
            pluginData.setNamespace(context.getNamespace());
            pluginData.setExampleProperty("My myPlugin GET data");
            return pluginData;
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }


    @POST
    @Path("/")
    public UserFeedback create(final PluginData data, @Context ServletContext servletContext) {
        log.info("data: {}", data);
        try {
            return new UserFeedback().addSuccess("Successfully executed myPlugin POST endpoint");
        } catch (Exception e) {
            log.error("Error creating plugin", e);
            return new UserFeedback().addError("Error executing myPlugin POST endpoint:" + e.getMessage());
        }
    }
}
