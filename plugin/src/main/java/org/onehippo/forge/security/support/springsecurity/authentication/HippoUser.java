/*
 *  Copyright 2017 Hippo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.forge.security.support.springsecurity.authentication;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class HippoUser extends User {

    private static final long serialVersionUID = 1L;

    private final Map<String, String> properties = new HashMap<>();

    public HippoUser(String username, String password, Collection<? extends GrantedAuthority> authorities, Map<String, String> props) {
        super(username, password, authorities);

        if (props != null) {
            this.properties.putAll(props);
        }
    }

    public HippoUser(String username, String password, boolean enabled, boolean accountNonExpired,
                     boolean credentialsNonExpired, boolean accountNonLocked,
                     Collection<? extends GrantedAuthority> authorities, Map<String, String> props) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);

        if (props != null) {
            this.properties.putAll(props);
        }
    }

    public String getFirstName() {
        return (String) getProperty("hipposys:firstname");
    }

    public String getLastName() {
        return (String) getProperty("hipposys:lastname");
    }

    public String getEmail() {
        return (String) getProperty("hipposys:email");
    }

    /**
     * Return extra Hippo user property such as <code>hipposys:firstname</code>, <code>hipposys:lastname </code>
     * and <code>hipposys:email</code>.
     *
     * @param propName extra property name of Hippo user
     * @return extra property value of Hippo user.
     */
    public Object getProperty(final String propName) {
        return properties.get(propName);
    }
}
