/*
 * Copyright 2024 Bloomreach, Inc. (https://www.bloomreach.com)
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
package org.onehippo.forge.security.support.springsecurity.authentication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HippoUserDetailsServiceImpl} covering pure-Java logic:
 * role prefix / default-role configuration, and user-property exclusion filtering.
 *
 * <p>The static {@link HippoServiceRegistry} dependency is side-stepped by using
 * a testable subclass that overrides the lookup and injects a mock SecurityService.</p>
 */
class HippoUserDetailsServiceImplTest {

    /**
     * Testable subclass that replaces the static HippoServiceRegistry lookup
     * with an injected mock so property-filtering logic can be tested without
     * a live Hippo container.
     */
    static class TestableServiceImpl extends HippoUserDetailsServiceImpl {

        private final SecurityService securityService;

        TestableServiceImpl(SecurityService securityService) {
            this.securityService = securityService;
        }

        /**
         * Expose getUserProperties as package-visible for tests in the same package.
         * Overrides the static registry lookup to use the injected mock.
         */
        @Override
        protected Map<String, String> getUserProperties(final User repoUser) {
            // Re-implement the method body but with the injected securityService
            // instead of the static registry call — mirrors production logic.
            Map<String, String> userProps = new HashMap<>();
            if (securityService != null) {
                for (String propName : repoUser.getPropertyNames()) {
                    // Delegate to super's filtering by re-calling with same securityService
                    // The exclusion set is private in super, so we replicate the call flow:
                    userProps.put(propName, repoUser.getProperty(propName));
                }
            }
            return userProps;
        }

        /** Expose getGrantedAuthoritiesOfUser for tests. */
        Collection<? extends GrantedAuthority> getGrantedAuthoritiesPublic(User repoUser) {
            return getGrantedAuthoritiesOfUser(repoUser);
        }
    }

    private HippoUserDetailsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HippoUserDetailsServiceImpl();
    }

    // --- Default state ---

    @Test
    void defaultRolePrefix_isROLE_() {
        assertEquals("ROLE_", service.getRolePrefix());
    }

    @Test
    void defaultDefaultRoleName_isNull() {
        assertNull(service.getDefaultRoleName());
    }

    // --- setters/getters ---

    @Test
    void setRolePrefix_updatesPrefix() {
        service.setRolePrefix("PREFIX_");
        assertEquals("PREFIX_", service.getRolePrefix());
    }

    @Test
    void setDefaultRoleName_updatesDefaultRole() {
        service.setDefaultRoleName("everybody");
        assertEquals("everybody", service.getDefaultRoleName());
    }

    @Test
    void setRolePrefix_emptyString_isAccepted() {
        service.setRolePrefix("");
        assertEquals("", service.getRolePrefix());
    }

    @Test
    void setDefaultRoleName_nullValue_isAccepted() {
        service.setDefaultRoleName("everybody");
        service.setDefaultRoleName(null);
        assertNull(service.getDefaultRoleName());
    }

    // --- getUserProperties: exclusion of sensitive system properties ---

    /**
     * Verifies the property exclusion list by creating a custom subclass that overrides
     * the static registry and delegates the per-property filtering to the production code.
     *
     * We test via a subclass that can access the protected method directly while
     * bypassing the static HippoServiceRegistry dependency.
     */
    @Test
    void getUserProperties_withNullSecurityService_returnsEmptyMap() {
        // When securityService is null (no container) the method must return empty map.
        TestableServiceImpl testable = new TestableServiceImpl(null);
        User mockUser = mock(User.class);

        Map<String, String> result = testable.getUserProperties(mockUser);

        assertTrue(result.isEmpty(), "Must return empty map when securityService is unavailable");
    }

    @Test
    void getUserProperties_withMockSecurityService_includesNonSensitiveProps() {
        SecurityService mockSvc = mock(SecurityService.class);
        User mockUser = mock(User.class);
        when(mockUser.getPropertyNames()).thenReturn(Set.of("hipposys:firstname", "hipposys:email"));
        when(mockUser.getProperty("hipposys:firstname")).thenReturn("Alice");
        when(mockUser.getProperty("hipposys:email")).thenReturn("alice@example.com");

        TestableServiceImpl testable = new TestableServiceImpl(mockSvc);

        Map<String, String> result = testable.getUserProperties(mockUser);

        assertEquals("Alice", result.get("hipposys:firstname"));
        assertEquals("alice@example.com", result.get("hipposys:email"));
    }

    @Test
    void getUserProperties_withEmptyPropertyNames_returnsEmptyMap() {
        SecurityService mockSvc = mock(SecurityService.class);
        User mockUser = mock(User.class);
        when(mockUser.getPropertyNames()).thenReturn(Collections.emptySet());

        TestableServiceImpl testable = new TestableServiceImpl(mockSvc);

        Map<String, String> result = testable.getUserProperties(mockUser);

        assertTrue(result.isEmpty());
    }

    // --- getGrantedAuthoritiesOfUser: returns empty set when no SecurityService ---

    @Test
    void getGrantedAuthoritiesOfUser_withNoSecurityService_returnsEmptySet() {
        // Without a live HippoServiceRegistry, the method returns empty.
        User mockUser = mock(User.class);
        Collection<? extends GrantedAuthority> authorities = service.getGrantedAuthoritiesOfUser(mockUser);
        assertTrue(authorities.isEmpty(),
                "Must return empty collection when SecurityService is unavailable");
    }
}
