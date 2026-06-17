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

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link HippoUser} value object.
 */
class HippoUserTest {

    private static final Collection<GrantedAuthority> NO_AUTHORITIES = Collections.emptyList();

    // --- Constructor with props ---

    @Test
    void constructor_withProps_storesAllProperties() {
        Map<String, String> props = new HashMap<>();
        props.put("hipposys:firstname", "Alice");
        props.put("hipposys:lastname", "Smith");
        props.put("hipposys:email", "alice@example.com");

        HippoUser user = new HippoUser("alice", "secret", NO_AUTHORITIES, props);

        assertEquals("Alice", user.getFirstName());
        assertEquals("Smith", user.getLastName());
        assertEquals("alice@example.com", user.getEmail());
    }

    @Test
    void constructor_withNullProps_doesNotThrow() {
        HippoUser user = new HippoUser("bob", "pass", NO_AUTHORITIES, null);

        assertNull(user.getFirstName());
        assertNull(user.getLastName());
        assertNull(user.getEmail());
    }

    @Test
    void constructor_withEmptyProps_returnsNullForAllGetters() {
        HippoUser user = new HippoUser("bob", "pass", NO_AUTHORITIES, Collections.emptyMap());

        assertNull(user.getFirstName());
        assertNull(user.getLastName());
        assertNull(user.getEmail());
    }

    // --- getProperty ---

    @Test
    void getProperty_withKnownPropName_returnsValue() {
        Map<String, String> props = Map.of("custom:attr", "value123");
        HippoUser user = new HippoUser("carol", "pass", NO_AUTHORITIES, props);

        assertEquals("value123", user.getProperty("custom:attr"));
    }

    @Test
    void getProperty_withUnknownPropName_returnsNull() {
        HippoUser user = new HippoUser("carol", "pass", NO_AUTHORITIES, Collections.emptyMap());

        assertNull(user.getProperty("nonexistent:prop"));
    }

    // --- Full-args constructor ---

    @Test
    void fullArgsConstructor_setsUsernameAndAuthorities() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_admin"));
        Map<String, String> props = Map.of("hipposys:email", "dave@example.com");

        HippoUser user = new HippoUser("dave", "pass", true, true, true, true, authorities, props);

        assertEquals("dave", user.getUsername());
        assertEquals(1, user.getAuthorities().size());
        assertEquals("dave@example.com", user.getEmail());
    }

    @Test
    void fullArgsConstructor_withNullProps_doesNotThrow() {
        HippoUser user = new HippoUser("eve", "pass", true, true, true, true, NO_AUTHORITIES, null);

        assertNull(user.getFirstName());
        assertNull(user.getEmail());
    }

    // --- Props map isolation (copy-on-construct) ---

    @Test
    void constructor_mutatingOriginalMap_doesNotAffectStoredProps() {
        Map<String, String> props = new HashMap<>();
        props.put("hipposys:firstname", "Frank");
        HippoUser user = new HippoUser("frank", "pass", NO_AUTHORITIES, props);

        props.put("hipposys:firstname", "Modified");

        assertEquals("Frank", user.getFirstName(), "HippoUser must copy the props map, not hold a reference");
    }

    // --- getUsername / getPassword delegates to parent ---

    @Test
    void getUsername_returnsConstructorValue() {
        HippoUser user = new HippoUser("grace", "pw", NO_AUTHORITIES, null);
        assertEquals("grace", user.getUsername());
    }
}
