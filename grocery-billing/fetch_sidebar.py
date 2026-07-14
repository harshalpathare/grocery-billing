import urllib.request
url = 'http://localhost:8080/reports'
try:
    req = urllib.request.Request(url)
    with urllib.request.urlopen(req) as response:
        html = response.read().decode('utf-8')
        idx1 = html.find('sidebar-nav')
        idx2 = html.find('sidebar-bottom')
        print(html[idx1:idx2])
except Exception as e:
    print('Error:', e)
