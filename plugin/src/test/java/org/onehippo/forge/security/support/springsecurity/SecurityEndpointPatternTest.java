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
package org.onehippo.forge.security.support.springsecurity;

import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies Ant path patterns used in Spring Security XML configuration behave correctly.
 *
 * <p>Spring Security uses {@link AntPathMatcher} for {@code <http pattern="...">} and
 * {@code <intercept-url pattern="...">} elements. Two common mistakes are caught here:</p>
 * <ol>
 *   <li><b>Regex anchors</b> — {@code /$} is not a valid Ant pattern; it silently fails to
 *       match {@code /}, causing unauthenticated users to hit the catch-all and be redirected
 *       to login. Use {@code /} instead. (Regression: FORGE-559)</li>
 *   <li><b>{@code **} vs {@code /**}</b> — {@code /news**} does not match {@code /news/article}
 *       because {@code **} embedded in a path token does not cross the {@code /} separator.
 *       Use {@code /news/**} to match all sub-paths. (Regression: FORGE-559)</li>
 * </ol>
 */
class SecurityEndpointPatternTest {

    private final AntPathMatcher matcher = new AntPathMatcher();

    // --- Root path regression (FORGE-559): "/$" vs "/" ---

    @Test
    void rootPattern_matchesHomepage() {
        assertTrue(matcher.match("/", "/"));
    }

    @Test
    void dollarSuffixPattern_doesNotMatchRoot() {
        // "/$" looks plausible but is not a valid Ant pattern and must never be used.
        assertFalse(matcher.match("/$", "/"),
                "\"/$\" is not a valid Ant pattern; use \"/\" instead");
    }

    // --- "**" suffix does NOT cross path separators (FORGE-559) ---

    @Test
    void doubleStarSuffix_doesNotMatchSubPaths() {
        // "/news**" only matches "/news" and paths like "/newsfoo" (no slash).
        // It does NOT match "/news/article.html" — use "/news/**" instead.
        assertFalse(matcher.match("/news**", "/news/article.html"),
                "\"/news**\" must not be relied on to match sub-paths; use \"/news/**\"");
    }

    // --- Correct "/**" patterns match both the root path and sub-paths ---

    @Test
    void newsSlashDoubleStarPattern_matchesNewsListAndDetailPages() {
        assertTrue(matcher.match("/news/**", "/news"));
        assertTrue(matcher.match("/news/**", "/news/"));
        assertTrue(matcher.match("/news/**", "/news/some-article.html"));
        assertTrue(matcher.match("/news/**", "/news/2024/spring-article.html"));
    }

    @Test
    void eventsSlashDoubleStarPattern_matchesEventsPages() {
        assertTrue(matcher.match("/events/**", "/events"));
        assertTrue(matcher.match("/events/**", "/events/"));
        assertTrue(matcher.match("/events/**", "/events/some-event.html"));
    }

    @Test
    void aboutSlashDoubleStarPattern_matchesAboutPages() {
        assertTrue(matcher.match("/about/**", "/about"));
        assertTrue(matcher.match("/about/**", "/about/team"));
    }

    @Test
    void profileSlashDoubleStarPattern_matchesProfilePages() {
        assertTrue(matcher.match("/profile/**", "/profile"));
        assertTrue(matcher.match("/profile/**", "/profile/settings"));
    }

    @Test
    void adminSlashDoubleStarPattern_matchesAdminPages() {
        assertTrue(matcher.match("/admin/**", "/admin"));
        assertTrue(matcher.match("/admin/**", "/admin/users"));
    }

    // --- Static resource bypass patterns ---

    @Test
    void binariesPattern_matchesBinaryServletPaths() {
        assertTrue(matcher.match("/binaries/**", "/binaries/content/gallery/image.jpg"));
        assertTrue(matcher.match("/binaries/**", "/binaries/content/documents/report.pdf"));
    }

    @Test
    void contentPattern_matchesInternalBinaryTranslationPaths() {
        // The /binaries servlet internally rewrites to /content/**.
        // Both patterns must be present in the security config (FORGE-559).
        assertTrue(matcher.match("/content/**", "/content/gallery/image.jpg"));
        assertTrue(matcher.match("/content/**", "/content/documents/report.pdf"));
    }

    @Test
    void webfilesPattern_matchesWebFileRequests() {
        assertTrue(matcher.match("/webfiles/**", "/webfiles/site/css/bootstrap.css"));
        assertTrue(matcher.match("/webfiles/**", "/webfiles/site/js/app.js"));
    }

    // --- Protected patterns do not accidentally match public paths ---

    @Test
    void profilePattern_doesNotMatchPublicPaths() {
        assertFalse(matcher.match("/profile/**", "/"));
        assertFalse(matcher.match("/profile/**", "/news"));
        assertFalse(matcher.match("/profile/**", "/about"));
    }

    @Test
    void adminPattern_doesNotMatchPublicPaths() {
        assertFalse(matcher.match("/admin/**", "/"));
        assertFalse(matcher.match("/admin/**", "/news"));
        assertFalse(matcher.match("/admin/**", "/about"));
    }
}
