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
