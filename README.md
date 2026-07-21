# HST Spring Security Support

[Spring Security](http://projects.spring.io/spring-security/) is a powerful and highly customizable authentication
and access-control framework.

**HST Spring Security Support** project provides seamless integration with Spring Security for HST-2 based applications.

Mainly, **HST Spring Security Support** project provides the following:

- An Authentication Provider which allows authentication against users and groups stored in the Hippo Repository.
  See the Javadoc of [```org.onehippo.forge.security.support.springsecurity.authentication.HippoAuthenticationProvider```](src/main/java/org/onehippo/forge/security/support/springsecurity/authentication/HippoAuthenticationProvider.java).
  for details.
- A Spring Security Valve in order to translate Spring Security Authentication to an HST-2 aware Subject.
  See the Javadoc of [```org.onehippo.forge.security.support.springsecurity.container.SpringSecurityValve```](src/main/java/org/onehippo/forge/security/support/springsecurity/container/SpringSecurityValve.java) for details.

Because Spring Security provides a lot of out-of-box security integration solutions such as HTTP Basic/Digest authentication, LDAP, Form-based, Open ID, JA-SIG CAS authentication, you can take advantage of those with HST-2! 

## Demo Project

- Demo project is available in [demo](demo/) folder. Follow [Running Demo Application](https://bloomreach-forge.github.io/hst-spring-security/runningdemo.html) page.

# Documentation (Local)

The documentation can generated locally by this command:

 > mvn clean site

The output is in the ```target/site/``` directory by default. You can open ```target/site/index.html``` in a browser.

# Documentation (GitHub Pages)

Documentation is available at [bloomreach-forge.github.io/hst-spring-security](https://bloomreach-forge.github.io/hst-spring-security).

You can generate the GitHub pages only from ```master``` branch by this command:

 > mvn -Pgithub.pages clean site

The output is in the ```docs/``` directory by default. You can open ```docs/index.html``` in a browser.

You can push it and GitHub Pages will be served for the site automatically.

## Spring Security 6 Configuration Guide

### Critical Change: Explicit Endpoint Definitions Required

As of spring Security 6 it requires **every endpoint to be explicitly defined**. Unlike earlier versions, undefined endpoints return 403 Forbidden — this includes static resources (CSS, JS, images) and binary servlet paths.

### Static Resources and the `/binaries` Servlet

The `/binaries` servlet internally translates requests to `/content/**` paths:

```
/binaries/content/gallery/logo.jpg  →  /content/gallery/logo.jpg
```

Both patterns must be explicitly excluded from security:

```xml
<http pattern="/binaries/**" security="none"/>
<http pattern="/content/**" security="none"/>
```

Forgetting `/content/**` causes 403 errors on all binary/media assets.

Full set of static resource exclusions for a typical brXM site:

```xml
<http pattern="/css/**"             security="none"/>
<http pattern="/images/**"          security="none"/>
<http pattern="/script/**"          security="none"/>
<http pattern="/binaries/**"        security="none"/>
<http pattern="/content/**"         security="none"/>
<http pattern="/webfiles/**"        security="none"/>
<http pattern="/_rp/**"             security="none"/>
<http pattern="/_cmsrest/**"        security="none"/>
<http pattern="/_cmsinternal/**"    security="none"/>
<http pattern="/_cmssessioncontext/**" security="none"/>
```

### Endpoint Security Rules

Patterns are evaluated top-to-bottom; more specific rules must come before the catch-all.

**Pattern pitfalls to avoid:**
- Use `/` for the homepage — not `/$` (`$` is not an Ant wildcard and silently fails to match)
- Use `/**` for sub-path wildcards — not `**` suffix (e.g. `/news/**` not `/news**`; `**` embedded in a token does not cross path separators)

```xml
<!-- Public content -->
<intercept-url pattern="/"         access="permitAll()" />
<intercept-url pattern="/news/**"  access="permitAll()" />
<intercept-url pattern="/events/**" access="permitAll()" />
<intercept-url pattern="/about/**" access="permitAll()" />

<!-- Auth pages — allow both anonymous and authenticated -->
<intercept-url pattern="/login.jsp*" access="isAnonymous() or hasRole('everybody')" />
<intercept-url pattern="/logout*"    access="isAnonymous() or hasRole('everybody')" />

<!-- Protected content -->
<intercept-url pattern="/admin/**"   access="hasRole('xm.default-user.system-admin')" />
<intercept-url pattern="/profile/**" access="hasRole('everybody')" />

<!-- Catch-all -->
<intercept-url pattern="/**" access="hasRole('everybody')" />
```

### Common Issues

| Symptom | Cause | Fix |
|---|---|---|
| CSS/images return 403 | Missing static resource pattern | Add `<http pattern="/your-path/**" security="none"/>` |
| Binaries/media return 403 | Missing `/content/**` pattern | Add both `/binaries/**` and `/content/**` exclusions |
| All pages 403 after login | Overly restrictive catch-all | Use `hasRole('everybody')` not `denyAll()` on `/**` |
| Login page returns 403 | Pattern allows only one auth state | Use `isAnonymous() or hasRole('everybody')` |

### Testing the Demo

Start the demo project (`mvn -Pdevelopment verify` in `demo/`) then verify the following with the browser Network tab open.

**Static resources — use the dedicated test page:**

Navigate to `http://localhost:8080/site/resources-test` without logging in.
The page is `permitAll()` and exercises every bypass pattern:

| Pattern | What the test page shows |
|---|---|
| `/webfiles/**` CSS | A Bootstrap-styled button — blue = CSS loaded |
| `/webfiles/**` JS | Inline script prints the jQuery version if loaded |
| `/binaries/**` + `/content/**` | Three gallery images rendered via `<img>` tags; broken image = missing pattern |
| `/images/**` | Explained inline — reference pattern, not directly exercised in this demo |

Direct URLs to verify in the Network tab:

| Resource | URL |
|---|---|
| Bootstrap CSS | `http://localhost:8080/site/webfiles/site/css/bootstrap.css` |
| jQuery | `http://localhost:8080/site/webfiles/site/js/jquery-3.4.1.min.js` |
| Gallery image (binaries) | `http://localhost:8080/site/binaries/content/gallery/hstspringsecdemo/samples/viognier-grapes-188185_640.jpg/viognier-grapes-188185_640.jpg/hippogallery:original` |

**Public pages (accessible without login):**

- `http://localhost:8080/site/` — homepage
- `http://localhost:8080/site/news` — news listing
- `http://localhost:8080/site/events` — events listing
- `http://localhost:8080/site/about` — about page

**Protected pages (redirect to login when unauthenticated):**

- `http://localhost:8080/site/profile` — requires `everybody` role; log in as `editor` / `editor`
- `http://localhost:8080/site/admin` — requires `xm.default-user.system-admin` role; log in as `siteadmin` / `siteadmin`

> **Demo users:** `editor/editor` (profile access), `siteadmin/siteadmin` (admin access). Do not use the CMS `admin` user — it has Hippo-internal system roles, not the `admin` Spring Security role.

**CSRF protection** — enabled by default in Spring Security 4+. The demo logout form in `base-top-menu.ftl` includes `${_csrf.token}` in a hidden field. To verify it works: attempt a POST to `/logout` without the token and confirm you receive a 403.

**Session management** — log in, access `/profile`, then log out. Confirm the session is invalidated and a subsequent request to `/profile` redirects to `/login.jsp`.

### Best Practices

1. **Order matters** — specific patterns before generic ones
2. **Explicit over implicit** — always define patterns; never rely on defaults
3. **Test static resources** after every config change (check browser Network tab for 403s)
4. **Enable DEBUG logging** for `org.springframework.security` to diagnose issues
