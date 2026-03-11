# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**HST Spring Security Support** is a Bloomreach (formerly Hippo) CMS plugin that provides seamless integration between Spring Security and HST-2 (Hippo Site Toolkit). The project integrates Spring Security's powerful authentication and access-control framework with Hippo Repository user/group stores.

**Current Version:** 5.0.0 (supports Hippo 16.0.0)
**Repository:** Part of bloomreach-forge
**Main Branch:** develop

### Two Main Modules

1. **plugin** (`plugin/`) - Core Spring Security integration components
   - `HippoAuthenticationProvider` - Spring Security AuthenticationProvider that validates credentials against the Hippo Repository
   - `HippoUserDetailsService` - Loads user details and authorities from the repository
   - `SpringSecurityValve` - HST-2 request valve that converts Spring Security Authentication into HST-2 Subjects
   - Supporting classes: `HippoUser`, `HippoUserDetailsServiceImpl`

2. **essentials-hippo-security-plugin** (`essentials-hippo-security-plugin/`) - Bloomreach Essentials plugin for automated setup

## Common Build and Test Commands

### Build and Package
```bash
# Clean build entire project (both modules)
mvn clean install

# Build without running tests
mvn clean install -DskipTests

# Build specific module
mvn clean install -pl plugin

# Package JAR for specific module
mvn clean package -pl plugin -DskipTests
```

### Documentation
```bash
# Generate documentation locally (outputs to target/site/)
mvn clean site

# Generate GitHub Pages documentation (outputs to docs/)
mvn -Pgithub.pages clean site
```

### Testing
```bash
# Run all tests
mvn clean test

# Run tests in specific module
mvn clean test -pl plugin

# Run specific test class
mvn test -Dtest=ClassName

# Run specific test method
mvn test -Dtest=ClassName#methodName
```

### Repository and Distribution
```bash
# Deploy to Bloomreach Maven repository (requires credentials)
mvn clean deploy
```

## Architecture and Integration Points

### Key Architecture Concepts

**Spring Security Integration Flow:**
1. Spring Security authenticates the user (via form login, HTTP Basic, LDAP, OIDC, etc.)
2. `HippoAuthenticationProvider` validates credentials against Hippo Repository using JCR login
3. `HippoUserDetailsService` loads user authorities/roles from the repository
4. `SpringSecurityValve` (HST-2 valve) converts Spring Security `Authentication` into HST-2 `Subject`
5. The `Subject` is stored in the HTTP session and used by subsequent HST-2 request processing

**Key Classes and Responsibilities:**

- **HippoAuthenticationProvider** (`plugin/src/main/java/.../authentication/HippoAuthenticationProvider.java`)
  - Extends `AbstractUserDetailsAuthenticationProvider`
  - Performs initial credential validation against Hippo Repository via JCR
  - Delegates to `HippoUserDetailsService` for loading full user details
  - Handles repository connection, login exception translation

- **HippoUserDetailsService** (`plugin/src/main/java/.../authentication/HippoUserDetailsService.java` - interface)
  - Extends Spring's `UserDetailsService`
  - Implemented by `HippoUserDetailsServiceImpl`
  - Loads user authorities/roles from the repository based on username and password

- **SpringSecurityValve** (`plugin/src/main/java/.../container/SpringSecurityValve.java`)
  - Extends `AbstractOrderableValve` (HST-2 valve)
  - Reads Spring Security's `SecurityContext` from thread-local storage
  - Converts `UserDetails` and `GrantedAuthority` objects to HST-2 `Subject` with principals and credentials
  - Stores converted `Subject` in HTTP session for use by HST-2 security processing
  - Handles both cases: with and without stored repository credentials

- **HippoUser** (`plugin/src/main/java/.../authentication/HippoUser.java`)
  - Implements Spring Security's `UserDetails` interface
  - Represents an authenticated user with authorities loaded from repository

### Critical Design Decisions

1. **JCR Credential Validation**: Credentials are validated by attempting a JCR login with `SimpleCredentials`. This ensures the user actually exists in the repository.

2. **Subject Repository Credentials**: The `SpringSecurityValve` has a configurable flag `storeSubjectRepositoryCredentials` (default: true) to decide whether to store JCR credentials in the Subject. If the password is cleared by Spring Security, a dummy password is used.

3. **Valve Ordering**: The valve must run in the HST-2 valve chain and should execute after Spring Security has authenticated the user but before HST-2 security processing.

4. **No Password Storage in Spring Security**: Modern Spring Security versions don't retain passwords in memory after authentication. The valve must handle this by using a dummy password for Subject credentials.

## Dependency Information

**Parent POM:** `org.onehippo.cms7:hippo-cms7-project` (version 16.0.0)

**Key Dependencies (managed in parent):**
- Spring Security 6.3.0 (in plugin)
- Spring Framework 6.1.x
- Jakarta Servlet API (replaces javax.servlet)
- Hippo Repository API and HST-2 APIs (JCR-based)

**Important:** Avoid newer Jakarta dependencies that break compatibility with Hippo 16. Dependencies use `scope="provided"` since they're provided by the Hippo container.

## File Structure

```
plugin/
  src/main/java/org/onehippo/forge/security/support/springsecurity/
    authentication/
      HippoAuthenticationProvider.java     - Spring Security authentication provider
      HippoUserDetailsService.java         - Interface for user detail loading
      HippoUserDetailsServiceImpl.java      - Default implementation
      HippoUser.java                       - UserDetails implementation
    container/
      SpringSecurityValve.java             - HST-2 valve for Subject creation

essentials-hippo-security-plugin/
  src/main/java/...                       - Essentials plugin code
  src/main/resources/                     - Plugin configuration and templates

demo/
  SECURITY_CONFIGURATION.md               - Comprehensive Spring Security 6 guide
  site/webapp/src/main/webapp/WEB-INF/
    applicationContext-security.xml       - Spring Security configuration with:
                                            * Static resource patterns
                                            * Public endpoints (news, events, etc.)
                                            * Protected endpoints (admin, profile)
                                            * Role-based access control
  site/components/src/main/java/...      - Demo components (News, Events)
```

## Demo Project Security Architecture

The demo project demonstrates realistic Spring Security 6 configuration:

**Public Content:**
- Homepage (`/`)
- News listings and articles (`/news**`)
- Events listings (`/events**`)
- About pages (`/about**`)

**Protected Content:**
- User profile pages (`/profile**`) - requires authentication
- Admin sections (`/admin**`) - requires admin role

**Static Resources:**
- CSS, images, scripts - bypass authentication
- Binaries (PDFs, media) - bypass authentication via `/binaries/**` and `/content/**` patterns

This realistic structure helps developers understand how to properly configure Spring Security 6 for real-world scenarios.

## Spring Security 6 Critical Changes

**Every endpoint must be explicitly defined.** Spring Security 6 breaks from earlier lenient behavior:
- Missing endpoint patterns result in 403 Forbidden responses
- This includes static resources (CSS, JS, images, binaries, etc.)
- The `/binaries/**` servlet internally translates to `/content/**` - both patterns must be explicitly allowed

**Key Configuration Pattern:**
```xml
<!-- Static resources -->
<http pattern="/binaries/**" security="none"/>
<http pattern="/content/**" security="none"/>

<!-- Public endpoints -->
<intercept-url pattern="/public**" access="permitAll()" />

<!-- Protected endpoints -->
<intercept-url pattern="/admin**" access="hasRole('admin')" />

<!-- Catch-all -->
<intercept-url pattern="/**" access="hasRole('everybody')" />
```

See `demo/SECURITY_CONFIGURATION.md` for comprehensive endpoint configuration guide including common issues and best practices.

## Important Notes

- The project uses Maven as build tool (Apache Maven 3.6.3+)
- Java 17+ is required (based on environment)
- All repositories are `provided` scope - they come from the Hippo container at runtime
- The plugin integrates Spring Security (which handles multiple auth mechanisms) with HST-2's Subject-based security model
- Documentation can be generated locally or published to GitHub Pages
- The essentials plugin automates setup for new HST-2 projects
- Spring Security 6 endpoint pattern configuration is critical - see Demo section below for details
