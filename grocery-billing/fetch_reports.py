import urllib.request
url = 'http://localhost:8080/reports'
try:
    req = urllib.request.Request(url)
    with urllib.request.urlopen(req) as response:
        html = response.read().decode('utf-8')
        if 'nav-link active' in html:
            print('Found active nav-link!')
        else:
            print('No active nav-link!')
        if 'bi-pie-chart-fill' in html:
            print('Found Reports link icon!')
        else:
            print('No Reports link icon!')
except Exception as e:
    print('Error:', e)
