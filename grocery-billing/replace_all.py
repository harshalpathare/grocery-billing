import glob
import re
import sys

replacement = """<div class="sidebar-brand">
    <img th:if="${globalLogoUrl != null and !globalLogoUrl.isEmpty()}" th:src="${globalLogoUrl}" alt="Logo" style="width: 32px; height: 32px; border-radius: 8px; object-fit: cover;">
    <i th:unless="${globalLogoUrl != null and !globalLogoUrl.isEmpty()}" th:class="'bi ' + ${globalAppIcon}"></i>
    <span th:text="${globalAppName}" th:style="'color: ' + ${globalAppNameColor} + ';'">Grocery Bill</span>
</div>"""

for f in glob.glob("src/main/resources/templates/**/*.html", recursive=True):
    with open(f, "r", encoding="utf-8") as file:
        content = file.read()
    
    # Replace the broken sidebar-brand (which has empty strings)
    new_content = re.sub(r'<div class="sidebar-brand">\s*<img th:if="" th:src="".*?</div>', replacement, content, flags=re.DOTALL)
    
    # Just in case some have the original sidebar-brand
    new_content = re.sub(r'<div class="sidebar-brand"><i class="bi bi-shop"></i>.*?</div>', replacement, new_content, flags=re.DOTALL)
    
    if content != new_content:
        with open(f, "w", encoding="utf-8") as file:
            file.write(new_content)
        print(f"Updated {f}")
