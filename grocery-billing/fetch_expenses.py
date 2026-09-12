import urllib.request
import urllib.parse
import http.cookiejar
import re

jar = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))

# Get login page
login_req = urllib.request.Request('http://localhost:8080/login')
login_res = opener.open(login_req)
login_html = login_res.read().decode('utf-8')

# Extract CSRF token
csrf_match = re.search(r'name="_csrf" value="([^"]+)"', login_html)
if not csrf_match:
    print("Could not find CSRF token")
    exit(1)
csrf = csrf_match.group(1)

# Login
login_data = urllib.parse.urlencode({'username': 'admin', 'password': 'admin', '_csrf': csrf}).encode('utf-8')
login_req2 = urllib.request.Request('http://localhost:8080/login', data=login_data)
opener.open(login_req2)

# Get expenses page
exp_req = urllib.request.Request('http://localhost:8080/expenses')
exp_res = opener.open(exp_req)
exp_html = exp_res.read().decode('utf-8')

print("--- Script tags ---")
for match in re.finditer(r'<script.*?</script>', exp_html, re.DOTALL):
    print(match.group(0)[:150].replace('\n', ' '))
