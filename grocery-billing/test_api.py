import urllib.request
import urllib.parse
import json

base_url = 'http://localhost:8080'

# 1. Login to get JWT token
login_data = json.dumps({
    'username': 'superadmin',
    'password': 'super@admin123'
}).encode('utf-8')

login_req = urllib.request.Request(
    f"{base_url}/api/v1/auth/login",
    data=login_data,
    headers={'Content-Type': 'application/json'}
)

try:
    with urllib.request.urlopen(login_req) as response:
        resp_data = json.loads(response.read().decode('utf-8'))
        token = resp_data.get('token')
        print(f"[+] Login successful, obtained JWT token: {token[:15]}...")

        # 2. Access protected API endpoint
        api_req = urllib.request.Request(
            f"{base_url}/api/v1/products",
            headers={
                'Authorization': f'Bearer {token}',
                'Accept': 'application/json'
            }
        )
        
        with urllib.request.urlopen(api_req) as api_resp:
            products = json.loads(api_resp.read().decode('utf-8'))
            print(f"[+] Successfully fetched products via API. Count: {len(products)}")
            if len(products) > 0:
                print(f"[+] First product: {products[0].get('name')} (ID: {products[0].get('id')})")
            
except urllib.error.HTTPError as e:
    print(f"[-] HTTP Error: {e.code} {e.reason}")
    print(e.read().decode('utf-8'))
except Exception as e:
    print(f"[-] Error: {e}")
