/*
 *  Copyright 2011 Hippo.
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

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Interface extending <code>UserDetailsService</code>
 * for integrating with Hippo Repository user/group store.
 */
public interface HippoUserDetailsService extends UserDetailsService {

    /**
     * Load <code>UserDetails</code> by username and password.
     * 
     * @param username username
     * @param password password
     * @return UserDetails instance
     * @throws UsernameNotFoundException if user is not found
     */
    UserDetails loadUserByUsernameAndPassword(String username, String password) throws UsernameNotFoundException;

}
