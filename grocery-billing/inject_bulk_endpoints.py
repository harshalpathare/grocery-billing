import os
import re

configs = [
    {
        'file': 'src/main/java/com/example/grocery_billing/controller/ProductController.java',
        'content': '''
    @PostMapping("/bulk-delete")
    public String bulkDelete(@RequestParam("ids") java.util.List<Long> ids, org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            for (Long id : ids) {
                productService.deleteProduct(id);
            }
            ra.addFlashAttribute("successMessage", "Selected products deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error deleting products: " + e.getMessage());
        }
        return "redirect:/products";
    }
'''
    },
    {
        'file': 'src/main/java/com/example/grocery_billing/controller/BillController.java',
        'content': '''
    @PostMapping("/bulk-delete")
    public String bulkDelete(@RequestParam("ids") java.util.List<Long> ids, org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            for (Long id : ids) {
                billService.deleteBill(id);
            }
            ra.addFlashAttribute("successMessage", "Selected bills deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error deleting bills: " + e.getMessage());
        }
        return "redirect:/bills";
    }
'''
    },
    {
        'file': 'src/main/java/com/example/grocery_billing/controller/CustomerController.java',
        'content': '''
    @PostMapping("/bulk-delete")
    public String bulkDelete(@RequestParam("ids") java.util.List<Long> ids, org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            for (Long id : ids) {
                customerService.deleteCustomer(id);
            }
            ra.addFlashAttribute("successMessage", "Selected customers deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error deleting customers: " + e.getMessage());
        }
        return "redirect:/customers";
    }
'''
    },
    {
        'file': 'src/main/java/com/example/grocery_billing/controller/SupplierController.java',
        'content': '''
    @PostMapping("/bulk-delete")
    public String bulkDelete(@RequestParam("ids") java.util.List<Long> ids, org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            for (Long id : ids) {
                supplierService.delete(id);
            }
            ra.addFlashAttribute("successMessage", "Selected suppliers deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error deleting suppliers: " + e.getMessage());
        }
        return "redirect:/suppliers";
    }
'''
    },
    {
        'file': 'src/main/java/com/example/grocery_billing/controller/PurchaseOrderController.java',
        'content': '''
    @PostMapping("/bulk-delete")
    public String bulkDelete(@RequestParam("ids") java.util.List<Long> ids, org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            for (Long id : ids) {
                purchaseOrderService.delete(id);
            }
            ra.addFlashAttribute("successMessage", "Selected purchases deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error deleting purchases: " + e.getMessage());
        }
        return "redirect:/purchases";
    }
'''
    },
    {
        'file': 'src/main/java/com/example/grocery_billing/controller/ExpenseController.java',
        'content': '''
    @PostMapping("/bulk-delete")
    public String bulkDelete(@RequestParam("ids") java.util.List<Long> ids, org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            for (Long id : ids) {
                expenseService.deleteExpense(id);
            }
            ra.addFlashAttribute("successMessage", "Selected expenses deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error deleting expenses: " + e.getMessage());
        }
        return "redirect:/expenses";
    }
'''
    }
]

for cfg in configs:
    filepath = cfg['file']
    if not os.path.exists(filepath):
        print(f"Skipping {filepath} - not found")
        continue

    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    if '@PostMapping("/bulk-delete")' in content:
        print(f"Skipping {filepath} - already has endpoint")
        continue

    # Insert before the last closing brace
    last_brace_index = content.rfind('}')
    if last_brace_index != -1:
        new_content = content[:last_brace_index] + cfg['content'] + '\n}\n'
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated {filepath}")
    else:
        print(f"Could not find closing brace in {filepath}")
