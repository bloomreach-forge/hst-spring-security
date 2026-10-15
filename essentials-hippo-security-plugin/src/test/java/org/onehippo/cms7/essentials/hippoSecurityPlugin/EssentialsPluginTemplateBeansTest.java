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
package org.onehippo.cms7.essentials.hippoSecurityPlugin;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.onehippo.forge.security.support.springsecurity.authentication.HippoAuthenticationProvider;
import org.onehippo.forge.security.support.springsecurity.authentication.HippoUserDetailsServiceImpl;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.DefaultNamespaceHandlerResolver;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerResolver;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;
import org.w3c.dom.Element;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the beans declared in the essentials plugin's bundled {@code applicationContext-security.xml}
 * template can be created against the current plugin classes.
 *
 * <p>Regression for FORGE-434: the template set a {@code roleDomainName} property that no longer exists on
 * {@link HippoUserDetailsServiceImpl}, so the site web application failed to start after installing the plugin
 * through Essentials.</p>
 */
class EssentialsPluginTemplateBeansTest {

    private static final String SECURITY_NAMESPACE = "http://www.springframework.org/schema/security";

    @Test
    void hippoBeansInTemplateCanBeCreated() {
        // siteFiles are not on the classpath — they are template files copied to the site
        // application during plugin installation. Load directly from the source tree.
        final File templateFile = new File("src/main/resources/siteFiles/applicationContext-security.xml");
        assertTrue(templateFile.exists(), "Template file not found: " + templateFile.getAbsolutePath());

        final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        final XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
        // Ignore the Spring Security namespace elements (<http>, <authentication-manager>): they need a servlet
        // environment. Only the plain <beans:bean> definitions are under test.
        reader.setNamespaceHandlerResolver(securityNamespaceIgnoringResolver());
        reader.loadBeanDefinitions(new FileSystemResource(templateFile));

        final HippoUserDetailsServiceImpl userDetailsService =
                beanFactory.getBean("hippoUserDetailsService", HippoUserDetailsServiceImpl.class);
        assertEquals("everybody", userDetailsService.getDefaultRoleName());

        final HippoAuthenticationProvider authenticationProvider =
                beanFactory.getBean("hippoAuthenticationProvider", HippoAuthenticationProvider.class);
        assertNotNull(authenticationProvider);
    }

    private static NamespaceHandlerResolver securityNamespaceIgnoringResolver() {
        final NamespaceHandlerResolver defaultResolver = new DefaultNamespaceHandlerResolver();
        final NamespaceHandler ignoringHandler = new NamespaceHandlerSupport() {
            @Override
            public void init() {
            }

            @Override
            public BeanDefinition parse(final Element element, final ParserContext parserContext) {
                return null;
            }
        };
        return namespaceUri -> SECURITY_NAMESPACE.equals(namespaceUri)
                ? ignoringHandler : defaultResolver.resolve(namespaceUri);
    }
}
