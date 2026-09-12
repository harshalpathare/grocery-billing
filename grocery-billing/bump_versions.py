import glob
import re

for f in glob.glob("src/main/resources/templates/**/*.html", recursive=True):
    with open(f, "r", encoding="utf-8") as file:
        content = file.read()
    
    new_content = re.sub(r'style\.css\?v=\d+', 'style.css?v=15', content)
    new_content = re.sub(r'app\.js\?v=\d+', 'app.js?v=29', new_content)
    
    if content != new_content:
        with open(f, "w", encoding="utf-8") as file:
            file.write(new_content)
        print(f"Bumped versions in {f}")
