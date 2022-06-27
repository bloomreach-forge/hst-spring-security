/*
 *  Copyright 2011-2020 Hippo.
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

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.hst.security.TransientUser;
import org.hippoecm.hst.security.impl.RepositoryAuthenticationProvider;
import org.hippoecm.hst.site.HstServices;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.security.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import javax.jcr.RepositoryException;
import java.util.*;
import java.util.stream.Collectors;

public class HippoUserDetailsServiceImpl implements HippoUserDetailsService {

    static final Logger log = LoggerFactory.getLogger(HippoUserDetailsServiceImpl.class);

    private static final Set<String> userPropsToExclude = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("hipposys:active", "hipposys:password",
                    "hipposys:passwordlastmodified", "hipposys:securityprovider")));

    private String defaultRoleName;

    public String getDefaultRoleName() {
        return defaultRoleName;
    }

    private String rolePrefix = "ROLE_";

    public String getRolePrefix() {
        return rolePrefix;
    }

    public void setRolePrefix(String rolePrefix) {
        this.rolePrefix = rolePrefix;
    }

    public void setDefaultRoleName(String defaultRoleName) {
        this.defaultRoleName = defaultRoleName;
    }

    private RepositoryAuthenticationProvider getRepositoryAuthenticationProvider() {
        return HstServices.getComponentManager().getComponent(RepositoryAuthenticationProvider.class);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return loadUserByUsernameAndPassword(username, null);
    }

    @Override
    public UserDetails loadUserByUsernameAndPassword(String username, String password)
            throws UsernameNotFoundException {
        User user = null;
        final RepositoryAuthenticationProvider rap = getRepositoryAuthenticationProvider();
        final TransientUser transientUser = rap.authenticate(username, password != null ? password.toCharArray() : ArrayUtils.EMPTY_CHAR_ARRAY);
        if (transientUser == null) {
            throw new UsernameNotFoundException("Could not authenticate user " + username + " against the repository");
        }
        final String repoUserName = transientUser.getName();
        final SecurityService securityService = HippoServiceRegistry.getService(SecurityService.class);
        if (securityService != null) {
            try {
                final org.onehippo.repository.security.User repoUser = securityService.getUser(repoUserName);
                if (repoUser == null) {
                    throw new UsernameNotFoundException("Could not find user " + repoUserName + " in the repository");
                }
                boolean accountNonExpired = true;
                boolean credentialsNonExpired = true;
                boolean accountNonLocked = true;
                Collection<? extends GrantedAuthority> authorities = getGrantedAuthoritiesOfUser(repoUser);
                user = new HippoUser(username, password != null ? password : repoUser.getProperty("hipposys:password"), repoUser.isActive(), accountNonExpired,
                        credentialsNonExpired, accountNonLocked, authorities, getUserProperties(repoUser));
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
        return user;
    }

    protected Collection<? extends GrantedAuthority> getGrantedAuthoritiesOfUser(org.onehippo.repository.security.User repoUser) {
        final SecurityService securityService = HippoServiceRegistry.getService(SecurityService.class);
        if (securityService != null) {
            final HashSet<String> userRoleNames = new HashSet<>(repoUser.getUserRoles());
            repoUser.getMemberships().forEach(groupId -> {
                try {
                    userRoleNames.addAll(securityService.getGroup(groupId).getUserRoles());
                } catch (RepositoryException e) {
                    log.error(e.getMessage());
                }
            });
            userRoleNames.add(defaultRoleName);
            return userRoleNames.stream().map(userRole -> new SimpleGrantedAuthority(rolePrefix + userRole)).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    protected Map<String, String> getUserProperties(final org.onehippo.repository.security.User repoUser) {
        Map<String, String> userProps = new HashMap<>();
        final SecurityService securityService = HippoServiceRegistry.getService(SecurityService.class);
        if (securityService != null) {
            for (String propName : repoUser.getPropertyNames()) {
                if (!userPropsToExclude.contains(propName)) {
                    userProps.put(propName, repoUser.getProperty(propName));
                }
            }
        }
        return userProps;
    }
}
