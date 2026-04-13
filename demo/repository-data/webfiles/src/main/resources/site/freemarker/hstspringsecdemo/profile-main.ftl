<#include "../include/imports.ftl">
<div>
  <h1>Profile</h1>
  <#if hstRequest.userPrincipal??>
    <p>Welcome, <strong>${hstRequest.userPrincipal.name?html}</strong>.</p>
    <p>You are authenticated and have the <code>everybody</code> role required to view this page.</p>
  <#else>
    <p>You should not see this page without authentication.</p>
  </#if>
  <p>Spring Security pattern: <code>/profile/**</code> &rarr; <code>hasRole('everybody')</code></p>
</div>
