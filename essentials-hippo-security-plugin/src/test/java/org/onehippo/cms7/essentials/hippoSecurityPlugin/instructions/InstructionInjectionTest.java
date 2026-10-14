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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.service.MavenRepositoryService;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.onehippo.cms7.essentials.sdk.api.service.WebXmlService;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that Essentials SDK services are injected into the custom installation instructions.
 *
 * <p>Regression for FORGE-434: Spring 7 (shipped with brXM 17) only recognises {@code jakarta.inject.Inject}.
 * Instructions annotated with {@code javax.inject.Inject} still compile, because {@code javax.inject} is on the
 * classpath transitively, but their services stay {@code null} and the plugin installation fails.</p>
 */
class InstructionInjectionTest {

    private static DefaultListableBeanFactory beanFactory;

    @BeforeAll
    static void setUpBeanFactory() {
        beanFactory = new DefaultListableBeanFactory();
        final AutowiredAnnotationBeanPostProcessor processor = new AutowiredAnnotationBeanPostProcessor();
        processor.setBeanFactory(beanFactory);
        beanFactory.addBeanPostProcessor(processor);
        beanFactory.registerSingleton("projectService", stub(ProjectService.class));
        beanFactory.registerSingleton("mavenRepositoryService", stub(MavenRepositoryService.class));
        beanFactory.registerSingleton("webXmlService", stub(WebXmlService.class));
    }

    @ParameterizedTest
    @ValueSource(classes = {
            AddVersionsInstruction.class,
            DependencyManagementInstruction.class,
            FilterMappingsInstruction.class,
            ForgeRepositoryInstruction.class
    })
    void essentialsServicesAreInjected(final Class<? extends Instruction> instructionClass) throws Exception {
        final Instruction instruction = instructionClass.getDeclaredConstructor().newInstance();

        // Same call the Essentials ExecuteInstruction makes before running a custom instruction
        beanFactory.autowireBean(instruction);

        int serviceFields = 0;
        for (Field field : instructionClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || !isEssentialsService(field.getType())) {
                continue;
            }
            serviceFields++;
            field.setAccessible(true);
            assertNotNull(field.get(instruction), instructionClass.getSimpleName() + "." + field.getName()
                    + " was not injected; use jakarta.inject.Inject instead of javax.inject.Inject");
        }
        assertTrue(serviceFields > 0, instructionClass.getSimpleName() + " has no Essentials service fields");
    }

    private static boolean isEssentialsService(final Class<?> type) {
        return type.getPackageName().equals(ProjectService.class.getPackageName());
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(final Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) ->
                switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> type.getSimpleName() + " stub";
                    default -> null;
                });
    }
}
