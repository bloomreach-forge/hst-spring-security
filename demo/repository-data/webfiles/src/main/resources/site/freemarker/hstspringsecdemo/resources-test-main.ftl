<#include "../include/imports.ftl">
<div>
  <h1>Static Resources Test</h1>
  <p>
    This page is <code>permitAll()</code> — no authentication required.
    Open the browser <strong>Network tab</strong> before loading to confirm every resource returns <strong>200</strong>.
  </p>

  <hr/>

  <h2>1. /webfiles/** — CSS</h2>
  <p>Bootstrap CSS is served via <code>/webfiles/**</code>. If the button below is blue and rounded, CSS loaded correctly.</p>
  <p><button class="btn btn-primary">Bootstrap button — styled by /webfiles/site/css/bootstrap.css</button></p>
  <#assign cssUrl><@hst.webfile path="/css/bootstrap.css"/></#assign>
  <p>Direct URL: <code><a href="${cssUrl}" target="_blank">${cssUrl}</a></code></p>

  <hr/>

  <h2>2. /webfiles/** — JavaScript</h2>
  <p>jQuery is served via <code>/webfiles/**</code>.</p>
  <#assign jsUrl><@hst.webfile path="/js/jquery-3.4.1.min.js"/></#assign>
  <p>Direct URL: <code><a href="${jsUrl}" target="_blank">${jsUrl}</a></code></p>
  <p id="jquery-status"><em>checking jQuery…</em></p>
  <script>
    document.getElementById('jquery-status').innerHTML = (typeof jQuery !== 'undefined')
      ? '<strong style="color:green">&#10003; jQuery ' + jQuery.fn.jquery + ' loaded — /webfiles/** working</strong>'
      : '<strong style="color:red">&#10007; jQuery NOT loaded — check /webfiles/** security="none"</strong>';
  </script>

  <hr/>

  <h2>3. /binaries/** and /content/** — Gallery Images</h2>
  <p>
    These images are served by the <code>/binaries</code> servlet, which internally forwards each request to
    <code>/content/**</code>. <strong>Both</strong> <code>/binaries/**</code> and <code>/content/**</code>
    must have <code>security="none"</code> for them to load.
    A broken image (or 403 in the Network tab) means one of the two patterns is missing.
  </p>
  <div style="margin:8px 0">
    <img src="${hstRequest.contextPath}/binaries/content/gallery/hstspringsecdemo/samples/viognier-grapes-188185_640.jpg/viognier-grapes-188185_640.jpg/hippogallery:original"
         alt="viognier grapes" style="max-height:150px; margin:4px; border:1px solid #ccc;"
         onerror="this.style.border='3px solid red'; this.alt='FAILED — check /binaries/** and /content/**'"/>
    <img src="${hstRequest.contextPath}/binaries/content/gallery/hstspringsecdemo/samples/animal-2883_640.jpg/animal-2883_640.jpg/hippogallery:original"
         alt="animal" style="max-height:150px; margin:4px; border:1px solid #ccc;"
         onerror="this.style.border='3px solid red'; this.alt='FAILED — check /binaries/** and /content/**'"/>
    <img src="${hstRequest.contextPath}/binaries/content/gallery/hstspringsecdemo/samples/snail-193611_640.jpg/snail-193611_640.jpg/hippogallery:original"
         alt="snail" style="max-height:150px; margin:4px; border:1px solid #ccc;"
         onerror="this.style.border='3px solid red'; this.alt='FAILED — check /binaries/** and /content/**'"/>
  </div>
  <p>
    Direct URL (open in a new tab, expect 200):<br/>
    <#assign binaryUrl>${hstRequest.contextPath}/binaries/content/gallery/hstspringsecdemo/samples/viognier-grapes-188185_640.jpg/viognier-grapes-188185_640.jpg/hippogallery:original</#assign>
    <code><a href="${binaryUrl}" target="_blank">${binaryUrl}</a></code>
  </p>
  <p>
    <em>The Spring Security <code>/content/**</code> pattern is not directly visible in the browser — it covers the
    internal forward the binaries servlet makes after receiving the <code>/binaries/**</code> request.
    Images loading above confirms both patterns are active.</em>
  </p>

  <hr/>

  <h2>4. /images/** — reference pattern only</h2>
  <p>
    The <code>/images/**</code> (and <code>/css/**</code>, <code>/script/**</code>) patterns exclude static files
    served directly from a webapp's static folders. In this demo all static assets go through the webfiles servlet
    (<code>/webfiles/**</code>), so these three patterns are not exercised here. They are included in the
    configuration as reference exclusions for real-world brXM sites.
  </p>
</div>
