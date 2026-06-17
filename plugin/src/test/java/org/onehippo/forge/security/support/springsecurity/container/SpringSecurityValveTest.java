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
package org.onehippo.forge.security.support.springsecurity.container;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import javax.security.auth.Subject;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SpringSecurityValve#invoke(ValveContext)}.
 *
 * <p>Each test exercises one of the guard clauses that cause the valve to skip
 * Subject creation and delegate to the next valve in the chain.</p>
 */
class SpringSecurityValveTest {

    private SpringSecurityValve valve;

    @BeforeEach
    void setUp() {
        valve = new SpringSecurityValve();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- Getter/Setter ---

    @Test
    void storeSubjectRepositoryCredentials_defaultIsTrue() {
        assertTrue(valve.isStoreSubjectRepositoryCredentials());
    }

    @Test
    void setStoreSubjectRepositoryCredentials_false_getterReflectsChange() {
        valve.setStoreSubjectRepositoryCredentials(false);
        assertFalse(valve.isStoreSubjectRepositoryCredentials());
    }

    @Test
    void setStoreSubjectRepositoryCredentials_true_getterReflectsChange() {
        valve.setStoreSubjectRepositoryCredentials(false);
        valve.setStoreSubjectRepositoryCredentials(true);
        assertTrue(valve.isStoreSubjectRepositoryCredentials());
    }

    // --- invoke: no user principal → skip ---

    @Test
    void invoke_noUserPrincipal_delegatesToNextValve() throws ContainerException {
        ValveContext ctx = mock(ValveContext.class);
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(ctx.getServletRequest()).thenReturn(req);
        when(req.getUserPrincipal()).thenReturn(null);

        valve.invoke(ctx);

        verify(ctx).invokeNext();
    }

    // --- invoke: subject already in session → skip ---

    @Test
    void invoke_subjectAlreadyInSession_delegatesToNextValve() throws ContainerException {
        ValveContext ctx = mock(ValveContext.class);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        Subject existingSubject = new Subject();

        when(ctx.getServletRequest()).thenReturn(req);
        when(req.getUserPrincipal()).thenReturn(mock(Principal.class));
        when(req.getSession(false)).thenReturn(session);
        when(session.getAttribute(ContainerConstants.SUBJECT_ATTR_NAME)).thenReturn(existingSubject);

        valve.invoke(ctx);

        verify(ctx).invokeNext();
    }

    // --- invoke: no security context → skip ---

    @Test
    void invoke_nullSecurityContext_delegatesToNextValve() throws ContainerException {
        // SecurityContextHolder.getContext() never returns null in practice, but
        // clearing it produces an empty context with null authentication — test that path.
        ValveContext ctx = mock(ValveContext.class);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);

        when(ctx.getServletRequest()).thenReturn(req);
        when(req.getUserPrincipal()).thenReturn(mock(Principal.class));
        when(req.getSession(false)).thenReturn(session);
        when(session.getAttribute(ContainerConstants.SUBJECT_ATTR_NAME)).thenReturn(null);

        // SecurityContext is cleared; getAuthentication() returns null.
        SecurityContextHolder.clearContext();

        valve.invoke(ctx);

        verify(ctx).invokeNext();
    }

    // --- invoke: no authentication in context → skip ---

    @Test
    void invoke_nullAuthentication_delegatesToNextValve() throws ContainerException {
        ValveContext ctx = mock(ValveContext.class);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        SecurityContext secCtx = mock(SecurityContext.class);

        when(ctx.getServletRequest()).thenReturn(req);
        when(req.getUserPrincipal()).thenReturn(mock(Principal.class));
        when(req.getSession(false)).thenReturn(session);
        when(session.getAttribute(ContainerConstants.SUBJECT_ATTR_NAME)).thenReturn(null);
        when(secCtx.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(secCtx);

        valve.invoke(ctx);

        verify(ctx).invokeNext();
    }

    // --- invoke: principal not a UserDetails → skip ---

    @Test
    void invoke_principalNotUserDetails_delegatesToNextValve() throws ContainerException {
        ValveContext ctx = mock(ValveContext.class);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        SecurityContext secCtx = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        // Principal is a plain String, not UserDetails
        when(auth.getPrincipal()).thenReturn("just-a-string");

        when(ctx.getServletRequest()).thenReturn(req);
        when(req.getUserPrincipal()).thenReturn(mock(Principal.class));
        when(req.getSession(false)).thenReturn(session);
        when(session.getAttribute(ContainerConstants.SUBJECT_ATTR_NAME)).thenReturn(null);
        when(secCtx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secCtx);

        valve.invoke(ctx);

        verify(ctx).invokeNext();
    }

    // --- invoke: valid UserDetails with password → Subject stored in session ---

    @Test
    void invoke_validUserDetailsWithPassword_storesSubjectAndDelegatesToNext() throws ContainerException {
        ValveContext ctx = mock(ValveContext.class);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpSession existingSession = mock(HttpSession.class);
        HttpSession newSession = mock(HttpSession.class);
        SecurityContext secCtx = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);
        Principal userPrincipal = mock(Principal.class);

        when(userPrincipal.getName()).thenReturn("alice");
        when(userDetails.getUsername()).thenReturn("alice");
        when(userDetails.getPassword()).thenReturn("secret");
        when(userDetails.getAuthorities()).thenReturn(java.util.Collections.emptyList());
        when(auth.getPrincipal()).thenReturn(userDetails);

        when(ctx.getServletRequest()).thenReturn(req);
        when(req.getUserPrincipal()).thenReturn(userPrincipal);
        when(req.getSession(false)).thenReturn(existingSession);
        when(existingSession.getAttribute(ContainerConstants.SUBJECT_ATTR_NAME)).thenReturn(null);
        when(req.getSession(true)).thenReturn(newSession);
        when(secCtx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secCtx);

        valve.invoke(ctx);

        verify(newSession).setAttribute(
                org.mockito.ArgumentMatchers.eq(ContainerConstants.SUBJECT_ATTR_NAME),
                org.mockito.ArgumentMatchers.any(Subject.class));
        verify(ctx).invokeNext();
    }

    // --- invoke: valid UserDetails with null password → dummy password used ---

    @Test
    void invoke_validUserDetailsWithNullPassword_storesSubjectWithDummyCredentials() throws ContainerException {
        ValveContext ctx = mock(ValveContext.class);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpSession existingSession = mock(HttpSession.class);
        HttpSession newSession = mock(HttpSession.class);
        SecurityContext secCtx = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);
        Principal userPrincipal = mock(Principal.class);

        when(userPrincipal.getName()).thenReturn("bob");
        when(userDetails.getUsername()).thenReturn("bob");
        when(userDetails.getPassword()).thenReturn(null); // erased password
        when(userDetails.getAuthorities()).thenReturn(java.util.Collections.emptyList());
        when(auth.getPrincipal()).thenReturn(userDetails);

        when(ctx.getServletRequest()).thenReturn(req);
        when(req.getUserPrincipal()).thenReturn(userPrincipal);
        when(req.getSession(false)).thenReturn(existingSession);
        when(existingSession.getAttribute(ContainerConstants.SUBJECT_ATTR_NAME)).thenReturn(null);
        when(req.getSession(true)).thenReturn(newSession);
        when(secCtx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secCtx);

        valve.invoke(ctx);

        verify(newSession).setAttribute(
                org.mockito.ArgumentMatchers.eq(ContainerConstants.SUBJECT_ATTR_NAME),
                org.mockito.ArgumentMatchers.any(Subject.class));
        verify(ctx).invokeNext();
    }

    // --- invoke: storeSubjectRepositoryCredentials=false → no JCR creds added ---

    @Test
    void invoke_storeCredentialsFalse_subjectStillCreatedAndDelegated() throws ContainerException {
        valve.setStoreSubjectRepositoryCredentials(false);

        ValveContext ctx = mock(ValveContext.class);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpSession existingSession = mock(HttpSession.class);
        HttpSession newSession = mock(HttpSession.class);
        SecurityContext secCtx = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);
        Principal userPrincipal = mock(Principal.class);

        when(userPrincipal.getName()).thenReturn("carol");
        when(userDetails.getUsername()).thenReturn("carol");
        when(userDetails.getPassword()).thenReturn("pw");
        when(userDetails.getAuthorities()).thenReturn(java.util.Collections.emptyList());
        when(auth.getPrincipal()).thenReturn(userDetails);

        when(ctx.getServletRequest()).thenReturn(req);
        when(req.getUserPrincipal()).thenReturn(userPrincipal);
        when(req.getSession(false)).thenReturn(existingSession);
        when(existingSession.getAttribute(ContainerConstants.SUBJECT_ATTR_NAME)).thenReturn(null);
        when(req.getSession(true)).thenReturn(newSession);
        when(secCtx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secCtx);

        valve.invoke(ctx);

        verify(newSession).setAttribute(
                org.mockito.ArgumentMatchers.eq(ContainerConstants.SUBJECT_ATTR_NAME),
                org.mockito.ArgumentMatchers.any(Subject.class));
        verify(ctx).invokeNext();
    }
}
