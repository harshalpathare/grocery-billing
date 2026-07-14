import os, re

files_to_process = [
    ('src/main/resources/templates/product/list.html', '/products/bulk-delete'),
    ('src/main/resources/templates/bill/list.html', '/bills/bulk-delete'),
    ('src/main/resources/templates/customer/list.html', '/customers/bulk-delete'),
    ('src/main/resources/templates/supplier/list.html', '/suppliers/bulk-delete'),
    ('src/main/resources/templates/purchase/list.html', '/purchases/bulk-delete'),
    ('src/main/resources/templates/expenses/list.html', '/expenses/bulk-delete')
]

js_template = """
<script>
    document.addEventListener('DOMContentLoaded', function() {
        const selectAll = document.getElementById('selectAll');
        const rowCheckboxes = document.querySelectorAll('.rowCheckbox');
        const bulkActions = document.getElementById('bulkActions');
        const selectedCount = document.getElementById('selectedCount');
        const bulkDeleteForm = document.getElementById('bulkDeleteForm');

        function updateBulkActions() {
            const checked = document.querySelectorAll('.rowCheckbox:checked');
            selectedCount.textContent = checked.length;
            if (checked.length > 0) {
                bulkActions.classList.remove('d-none');
            } else {
                bulkActions.classList.add('d-none');
            }
        }

        if (selectAll) {
            selectAll.addEventListener('change', function() {
                rowCheckboxes.forEach(cb => cb.checked = this.checked);
                updateBulkActions();
            });
        }

        rowCheckboxes.forEach(cb => {
            cb.addEventListener('change', updateBulkActions);
        });

        window.confirmBulkDelete = function() {
            if (confirm('Are you sure you want to delete all selected items? This cannot be undone.')) {
                bulkDeleteForm.querySelectorAll('.bulk-id').forEach(el => el.remove());
                document.querySelectorAll('.rowCheckbox:checked').forEach(cb => {
                    const input = document.createElement('input');
                    input.type = 'hidden';
                    input.name = 'ids';
                    input.value = cb.value;
                    input.className = 'bulk-id';
                    bulkDeleteForm.appendChild(input);
                });
                bulkDeleteForm.submit();
            }
        }
    });
</script>
"""

for filepath, endpoint in files_to_process:
    if not os.path.exists(filepath):
        print(f"Skipping {filepath} - not found")
        continue
        
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    if 'selectAll' in content and 'bulkDeleteForm' in content:
        print(f"Skipping {filepath} - already has bulk delete")
        continue

    # 1. Insert Bulk Actions div before table-responsive
    bulk_action_html = f'''
        <div id="bulkActions" class="d-none mb-3">
            <form id="bulkDeleteForm" method="post" action="{endpoint}">
                <input type="hidden" th:name="${{_csrf.parameterName}}" th:value="${{_csrf.token}}">
                <button type="button" class="btn btn-danger btn-sm" onclick="confirmBulkDelete()">
                    <i class="bi bi-trash-fill me-1"></i> Delete Selected (<span id="selectedCount">0</span>)
                </button>
            </form>
        </div>
'''
    content = re.sub(r'(<div[^>]*class="[^"]*table-responsive[^"]*"[^>]*>)', bulk_action_html + r'\1', content)

    # 2. Insert <th> in thead > tr
    content = re.sub(r'(<thead>\s*<tr>\s*)', r'\1<th style="width: 40px"><input type="checkbox" id="selectAll" class="form-check-input"></th>\n', content)

    # 3. Find the loop variable to inject <td>
    # Looks like <tr th:each="product, stat : ${products}"> or <tr th:each="bill : ${bills}">
    match = re.search(r'<tr[^>]*th:each="([a-zA-Z0-9_]+)', content)
    if match:
        var_name = match.group(1)
        td_html = f'<td><input type="checkbox" class="form-check-input rowCheckbox" th:value="${{{var_name}.id}}"></td>\n'
        content = re.sub(r'(<tr[^>]*th:each="[^"]*"[^>]*>\s*)', r'\1' + td_html, content)
    else:
        print(f"Could not find loop variable in {filepath}")

    # 4. Insert JS before </body>
    content = re.sub(r'(</body>)', js_template + r'\1', content)

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    
    print(f"Updated {filepath}")
