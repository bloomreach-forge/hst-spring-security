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

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.site.HstServices;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * <code>AuthenticationProvider</code> implementation which authenticates users
 * against Hippo Repository user/group store.
 */
public class HippoAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

    private Repository systemRepository;

    private HippoUserDetailsService hippoUserDetailsService;

    public void setSystemRepository(Repository systemRepository) {
        this.systemRepository = systemRepository;
    }

    public Repository getSystemRepository() {
        if (systemRepository == null) {
            systemRepository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        }

        return systemRepository;
    }

    public void setHippoUserDetailsService(HippoUserDetailsService hippoUserDetailsService) {
        this.hippoUserDetailsService = hippoUserDetailsService;
    }

    protected HippoUserDetailsService getHippoUserDetailsService() {
        if (hippoUserDetailsService == null) {
            hippoUserDetailsService = new HippoUserDetailsServiceImpl();
            ((HippoUserDetailsServiceImpl) hippoUserDetailsService).setDefaultRoleName("everybody");
        }

        return hippoUserDetailsService;
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails,
            UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        if (authentication.getCredentials() == null) {
            logger.debug("Authentication failed: no credentials provided");

            throw new BadCredentialsException(messages.getMessage(
                    "AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"), null);
        }
    }

    @Override
    protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication)
            throws AuthenticationException {

        Repository sysRepo = getSystemRepository();

        if (sysRepo == null) {
            throw new ProviderNotFoundException("Hippo Repository is not available now.");
        }

        Session session = null;
        String password = authentication.getCredentials().toString();

        try {
            session = sysRepo.login(new SimpleCredentials(username, password.toCharArray()));
        } catch (LoginException e) {
            throw new BadCredentialsException(e.getMessage());
        } catch (RepositoryException e) {
            throw new ProviderNotFoundException(e.getMessage());
        } finally {
            try {
                if (session != null) {
                    session.logout();
                }
            } catch (Exception ignore) {
            }
        }

        UserDetails loadedUser;

        try {
            loadedUser = getHippoUserDetailsService().loadUserByUsernameAndPassword(username, password);
        } catch (Exception e) {
            throw new AuthenticationServiceException(e.getMessage(), e);
        }

        if (loadedUser == null) {
            throw new AuthenticationServiceException(
                    "UserDetailsService returned null, which is an interface contract violation");
        }

        return loadedUser;
    }

}
