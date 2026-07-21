<#include "../include/imports.ftl">
<div>
  <h1>Admin</h1>
  <#if hstRequest.userPrincipal??>
    <p>Welcome, <strong>${hstRequest.userPrincipal.name?html}</strong>.</p>
    <p>You are authenticated with the <code>admin</code> role required to view this page.</p>
  <#else>
    <p>You should not see this page without admin role.</p>
  </#if>
  <p>Spring Security pattern: <code>/admin/**</code> &rarr; <code>hasRole('xm.default-user.system-admin')</code></p>
</div>
