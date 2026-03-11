# Spring Security 6 Integration Test Checklist

## Overview

This checklist documents the manual and automated tests required to verify that Spring Security 6 endpoint configuration is working correctly, specifically addressing FORGE-559.

## Manual Testing Checklist

### Static Resource Access (FORGE-559 - Critical Issue)

- [ ] **CSS Files Load Without Authentication**
  - Open login page and verify no 403 errors in developer console
  - Check Network tab - all .css files should load with 200/304 status
  - Verify page styling renders correctly even before login

- [ ] **JavaScript Files Load Without Authentication**
  - Open login page and check Network tab
  - All .js files should return 200/304 status
  - No JavaScript errors blocking page functionality

- [ ] **Images Load Without Authentication**
  - Verify image assets display on login page
  - Check that `/images/**` pattern resources return 200/304
  - Test with various image formats (.jpg, .png, .gif, .svg)

- [ ] **Binary Servlet Resources Load (FORGE-559 - Binaries Mapping)**
  - Request `/binaries/content/gallery/image.jpg`
  - Verify resource loads with 200 status (not 403)
  - Confirm `/content/**` pattern is working correctly
  - Test that internal path translation (`/binaries/**` → `/content/**`) works

- [ ] **WebFiles Accessible Without Authentication**
  - Request resources from `/webfiles/**`
  - Verify 200 status code responses
  - Test various file types (PDFs, documents, media)

### Public Endpoint Access

- [ ] **Homepage Loads Without Authentication**
  - Navigate to `/` without logging in
  - Verify page displays with all assets loading
  - Check that no redirect to login occurs

- [ ] **News Listings Accessible Without Authentication**
  - Navigate to `/news` without logging in
  - Verify page loads successfully
  - Check that news content is visible

- [ ] **Events Listings Accessible Without Authentication**
  - Navigate to `/events` without logging in
  - Verify page loads successfully
  - Check that events content is visible

- [ ] **About Pages Accessible Without Authentication**
  - Navigate to `/about` without logging in
  - Verify page loads successfully

### Authentication Endpoints

- [ ] **Login Page Accessible by Anonymous Users**
  - Navigate to `/login.jsp` without authentication
  - Verify login form displays
  - Check that login page CSS/images/scripts load

- [ ] **Login Page Accessible by Authenticated Users**
  - Log in with valid credentials
  - Navigate to `/login.jsp`
  - Verify page loads (doesn't redirect immediately)

- [ ] **Logout Works Correctly**
  - Navigate to `/logout`
  - Verify session is invalidated
  - Verify redirect to homepage occurs
  - Verify user is logged out (can't access protected pages)

### Protected Endpoint Access

- [ ] **User Profile Requires Authentication**
  - Navigate to `/profile` without logging in
  - Verify redirect to login page occurs
  - Log in and verify `/profile` becomes accessible

- [ ] **Admin Endpoints Require Admin Role**
  - Navigate to `/admin` without logging in
  - Verify redirect to login page occurs
  - Log in with regular user account
  - Verify 403 Forbidden response (access denied)
  - Log in with admin account
  - Verify `/admin` becomes accessible

### Role-Based Access Control

- [ ] **Users with 'everybody' Role Can Access Protected Content**
  - Log in with user having 'everybody' role
  - Verify access to `/profile` is granted

- [ ] **Users with 'admin' Role Can Access Admin Areas**
  - Log in with admin user
  - Verify access to `/admin` is granted

- [ ] **Regular Users Cannot Access Admin Areas**
  - Log in with non-admin user
  - Attempt to access `/admin`
  - Verify 403 Forbidden response

### CSRF Protection

- [ ] **CSRF Token Present on Login Form**
  - Open login page
  - View page source
  - Verify CSRF token field is present in form
  - Check token name matches Spring Security configuration

- [ ] **CSRF Token Validation Works**
  - Submit login form with invalid/missing CSRF token
  - Verify request is rejected (403 or invalid token error)

### Session Management

- [ ] **Sessions Persist Across Authenticated Requests**
  - Log in with valid credentials
  - Navigate to different pages
  - Verify user remains logged in
  - Verify Session ID remains stable

- [ ] **Sessions Invalidated on Logout**
  - Log in with valid credentials
  - Perform logout
  - Use developer tools to inspect cookies
  - Verify JSESSIONID cookie is removed/invalidated

## Automated Testing Scenarios

### Security Filter Chain Verification

```
Test: Verify /binaries/** pattern allows unauthenticated access
Expected: HTTP 200 response for /binaries/content/image.jpg
Result: PASS/FAIL

Test: Verify /content/** pattern allows unauthenticated access
Expected: HTTP 200 response for /content/gallery/image.jpg
Result: PASS/FAIL

Test: Verify /css/** allows unauthenticated access
Expected: HTTP 200 response for /css/style.css
Result: PASS/FAIL

Test: Verify /script/** allows unauthenticated access
Expected: HTTP 200 response for /script/app.js
Result: PASS/FAIL

Test: Verify /images/** allows unauthenticated access
Expected: HTTP 200 response for /images/logo.png
Result: PASS/FAIL
```

### Public Endpoint Verification

```
Test: Verify homepage accessible without authentication
Expected: HTTP 200 response for /
Result: PASS/FAIL

Test: Verify news page accessible without authentication
Expected: HTTP 200 response for /news
Result: PASS/FAIL

Test: Verify events page accessible without authentication
Expected: HTTP 200 response for /events
Result: PASS/FAIL

Test: Verify about page accessible without authentication
Expected: HTTP 200 response for /about
Result: PASS/FAIL
```

### Protected Endpoint Verification

```
Test: Verify profile page redirects unauthenticated users
Expected: HTTP 302 redirect to /login.jsp
Result: PASS/FAIL

Test: Verify profile page allows authenticated users
Expected: HTTP 200 response after authentication
Result: PASS/FAIL

Test: Verify admin endpoint requires admin role
Expected: HTTP 403 for non-admin users, HTTP 200 for admin users
Result: PASS/FAIL
```

## Regression Testing

After making changes to `applicationContext-security.xml`:

1. **Run Full Manual Checklist** - Ensure no new 403 errors introduced
2. **Test Static Asset Loading** - Verify CSS, JS, images all load correctly
3. **Test Authentication Flow** - Verify login/logout still works
4. **Test Role-Based Access** - Verify role restrictions are enforced
5. **Test Session Management** - Verify sessions persist and invalidate correctly

## Browser Testing

Test across multiple browsers:
- [ ] Chrome/Edge (Chromium-based)
- [ ] Firefox
- [ ] Safari
- [ ] Mobile browsers (iOS Safari, Chrome Mobile)

## Common Issues to Verify

### FORGE-559 Specific Tests

- [ ] **Verify /content/** pattern prevents 403 errors**
  - **Issue:** Missing `/content/**` pattern causes static resources to fail
  - **Test:** Request `/binaries/content/gallery/image.jpg` and verify 200 status
  - **Expected Result:** Resource loads successfully

- [ ] **Verify realistic security policy doesn't expose missed endpoints**
  - **Issue:** Generic catch-all policy hides missing endpoint definitions
  - **Test:** Attempt to access new endpoints and verify proper 403/redirect behavior
  - **Expected Result:** Only explicitly allowed endpoints are accessible

## Performance Considerations

- [ ] **Static Resources Cached Properly**
  - Verify Cache-Control headers are set correctly
  - Ensure browser cache is utilized for repeated requests

- [ ] **Authentication Not Bottleneck**
  - Verify login performance is acceptable
  - Ensure authentication checks don't delay page rendering

## Deployment Verification

Before deploying to production:

1. [ ] Run full manual test checklist
2. [ ] Verify all static resources load correctly
3. [ ] Test with production data
4. [ ] Verify SSL/HTTPS works correctly with all resources
5. [ ] Check logs for 403 errors after first hour of operation
6. [ ] Monitor for missing endpoint patterns causing issues

## Test Result Documentation

Document all test results:

```markdown
## Test Execution: [DATE]

**Build Version:** [VERSION]
**Test Environment:** [ENVIRONMENT]
**Tester:** [NAME]

### Summary
- Total Tests: X
- Passed: X
- Failed: X
- Skipped: X

### Failed Tests
[List any failures]

### Notes
[Any observations or issues]
```

## References

- Spring Security 6 Testing Guide: https://docs.spring.io/spring-security/reference/6.0/servlet/test/index.html
- FORGE-559 Issue: Spring Security 6 missing endpoint definitions
- Demo Project: `/demo/site/webapp/src/main/webapp/WEB-INF/applicationContext-security.xml`
