# Spring Security 6 Endpoint Configuration Guide

## Overview

This document explains the Spring Security 6 configuration for the HST Spring Security demo project and addresses important changes introduced in Spring Security 6.

## Key Changes in Spring Security 6

In Spring Security 6, **every endpoint must be explicitly defined** in your security configuration. Unlike earlier versions that were lenient about undefined endpoints, Spring Security 6 will reject any endpoint not explicitly allowed, returning a 403 (Forbidden) response.

This includes:
- Static resources (CSS, JavaScript, images)
- Binary files (PDFs, documents, media)
- Any API endpoints
- Content delivery paths

### Impact of Missing Endpoint Definitions

If an endpoint is not explicitly configured, Spring Security will:
1. Reject HTTP requests to that endpoint with a 403 Forbidden response
2. Potentially break critical functionality like asset loading or file downloads
3. Make the application appear broken to end users

## Demo Configuration

The demo project's security configuration is defined in:
```
demo/site/webapp/src/main/webapp/WEB-INF/applicationContext-security.xml
```

### Static Resource Patterns

**These patterns are defined with `security="none"` to bypass authentication:**

```xml
<http pattern="/css/**" security="none"/>
<http pattern="/images/**" security="none"/>
<http pattern="/script/**" security="none"/>
<http pattern="/binaries/**" security="none"/>
<http pattern="/content/**" security="none"/>
<http pattern="/webfiles/**" security="none" />
```

#### Important: /binaries and /content Pattern

The `/binaries` endpoint is a Hippo servlet that internally translates requests:
- Request: `/binaries/content/gallery/johnstonesupplycms/logo.jpg`
- Internal path: `/content/gallery/johnstonesupplycms/logo.jpg`

Therefore, **both patterns must be explicitly allowed**. If you forget the `/content/**` pattern, static resources served through the binaries servlet will fail with 403 errors.

### Channel Manager Patterns

These patterns allow Hippo's Channel Manager integration to bypass authentication:

```xml
<http pattern="/_rp/**" security="none"/>
<http pattern="/_cmsrest/**" security="none"/>
<http pattern="/_cmsinternal/**" security="none"/>
<http pattern="/_cmssessioncontext/**" security="none"/>
```

### Endpoint-Specific Security Rules

The demo implements a realistic security model with mixed public and protected content:

#### Public Endpoints (No Authentication Required)

```xml
<!-- Home page - publicly accessible -->
<intercept-url pattern="/$" access="permitAll()" />

<!-- Browse content - publicly accessible -->
<intercept-url pattern="/news**" access="permitAll()" />
<intercept-url pattern="/events**" access="permitAll()" />
<intercept-url pattern="/about**" access="permitAll()" />
```

These patterns allow visitors to browse site content without logging in.

#### Authentication Endpoints

```xml
<intercept-url pattern="/login.jsp*" access="isAnonymous() or hasRole('everybody')" />
<intercept-url pattern="/logout*" access="isAnonymous() or hasRole('everybody')" />
```

Allow both anonymous users (to access the login page) and authenticated users to use authentication endpoints.

#### Protected Endpoints (Authentication Required)

```xml
<!-- Admin section - requires 'admin' role -->
<intercept-url pattern="/admin**" access="hasRole('admin')" />

<!-- User profile - requires 'everybody' role -->
<intercept-url pattern="/profile**" access="hasRole('everybody')" />
```

#### Default Catch-All

```xml
<!-- Default: Allow authenticated users with 'everybody' role -->
<intercept-url pattern="/**" access="hasRole('everybody')" />
```

Any endpoint not explicitly matched by previous rules defaults to requiring the 'everybody' role.

## Customizing the Configuration

### Adding a New Public Endpoint

To add a public page (e.g., blog):

```xml
<intercept-url pattern="/blog**" access="permitAll()" />
```

Insert this **before** the catch-all `/**` pattern.

### Adding a Protected Endpoint

To add a page requiring authentication:

```xml
<intercept-url pattern="/reports**" access="hasRole('everybody')" />
```

### Role-Based Access Control

Define role-specific access:

```xml
<!-- Only users with 'admin' role can access settings -->
<intercept-url pattern="/settings**" access="hasRole('admin')" />

<!-- Only users with 'editor' role can create content -->
<intercept-url pattern="/editor**" access="hasRole('editor')" />
```

### Complex Access Rules

Combine multiple conditions using Spring EL:

```xml
<!-- Allow admins OR content editors -->
<intercept-url pattern="/dashboard**" access="hasRole('admin') or hasRole('editor')" />

<!-- Allow authenticated users except during maintenance windows -->
<intercept-url pattern="/data-api**" access="hasRole('everybody') and @serviceStatusProvider.isOperational()" />
```

## Testing the Configuration

To verify that your security configuration is working:

1. **Verify Static Resources Load:** Open developer console (F12) and check that CSS, images, and scripts load without 403 errors
2. **Test Public Pages:** Browse to public endpoints without logging in
3. **Test Protected Pages:** Attempt to access protected pages without authentication (should redirect to login)
4. **Test After Login:** Verify authenticated users can access protected pages
5. **Test Role-Based Access:** Use different user accounts with different roles to verify role-based restrictions

## Common Issues

### Issue: Static Resources Return 403 Forbidden

**Cause:** Missing endpoint pattern definition

**Solution:** Add the missing pattern to the static resources section:
```xml
<http pattern="/your-path/**" security="none"/>
```

### Issue: Binaries Servlet Returns 403

**Cause:** Missing `/content/**` pattern (see binaries servlet mapping explanation above)

**Solution:** Ensure both patterns are defined:
```xml
<http pattern="/binaries/**" security="none"/>
<http pattern="/content/**" security="none"/>
```

### Issue: All Pages Return 403 After Login

**Cause:** Catch-all pattern uses `denyAll()` or overly restrictive roles

**Solution:** Change the catch-all to allow authenticated users:
```xml
<intercept-url pattern="/**" access="hasRole('everybody')" />
```

### Issue: Login Page Returns 403

**Cause:** Login page pattern allows only one of `isAnonymous()` or `hasRole()`

**Solution:** Use the OR operator to allow both:
```xml
<intercept-url pattern="/login.jsp*" access="isAnonymous() or hasRole('everybody')" />
```

## Best Practices

1. **Explicit Over Implicit:** Always explicitly define patterns rather than relying on defaults
2. **Order Matters:** Patterns are evaluated in order; more specific patterns should come before generic ones
3. **Test After Changes:** Always test static resources and authentication flows after configuration changes
4. **Document Changes:** Add comments explaining why specific patterns are included
5. **Use permitAll() for Public Content:** Be explicit about public endpoints rather than using blanket allows
6. **Avoid Overlapping Patterns:** Ensure patterns don't overlap in ways that create ambiguous rules
7. **Monitor Logs:** Enable DEBUG logging for Spring Security to troubleshoot configuration issues

## References

- [Spring Security 6 Migration Guide](https://docs.spring.io/spring-security/reference/6.0/index.html)
- [Servlet Endpoint Configuration](https://docs.spring.io/spring-security/reference/6.0/servlet/configuration/index.html)
- [Expression-Based Access Control](https://docs.spring.io/spring-security/reference/6.0/servlet/authorization/expression-based.html)
