import glob

def check_page(path):
    with open(path, encoding='utf-8') as f:
        content = f.read()
    
    has_sidebar_id     = 'id="sidebar"' in content
    has_main_id        = 'id="mainContent"' in content
    has_toggle_btn     = 'id="sidebarToggle"' in content
    toggle_count       = content.count('id="sidebarToggle"')
    sidebar_count      = content.count('id="sidebar"')
    main_count         = content.count('id="mainContent"')
    has_appjs          = 'app.js' in content
    
    # Check for inline DOMContentLoaded that might conflict
    inline_dcl         = content.count('DOMContentLoaded')
    
    # Check for errors that might throw before app.js runs
    # Find line number of sidebarToggle button
    lines = content.split('\n')
    btn_line = next((i+1 for i, l in enumerate(lines) if 'id="sidebarToggle"' in l), None)
    appjs_line = next((i+1 for i, l in enumerate(lines) if 'app.js' in l), None)
    
    print(f"\n{'='*60}")
    print(f"FILE: {path}")
    print(f"  has #sidebar:        {has_sidebar_id} (count: {sidebar_count})")
    print(f"  has #mainContent:    {has_main_id} (count: {main_count})")
    print(f"  has #sidebarToggle:  {has_toggle_btn} (count: {toggle_count})")
    print(f"  has app.js:          {has_appjs}")
    print(f"  DOMContentLoaded x:  {inline_dcl}")
    print(f"  #sidebarToggle line: {btn_line}")
    print(f"  app.js line:         {appjs_line}")

for f in sorted(glob.glob('src/main/resources/templates/**/*.html', recursive=True)):
    # Skip reports, auth login page, super admin pages that may not have sidebar
    check_page(f)
