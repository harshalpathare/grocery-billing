package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.*;
import com.example.grocery_billing.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ActionExecutorService {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final BillRepository billRepository;
    private final BillItemRepository billItemRepository;
    private final TransactionRepository transactionRepository;
    private final SupplierRepository supplierRepository;
    private final ObjectMapper objectMapper;
    private final CustomerService customerService;
    private final ProductService productService;
    private final SupplierService supplierService;
    private final ReportService reportService;
    private final PurchaseOrderService purchaseOrderService;

    // ─────────────────────────────────────────────
    // EXECUTE ACTIONS FROM AI RESPONSE
    // ─────────────────────────────────────────────
    public Map<String, Object> executeAction(String aiResponse) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", aiResponse);
        result.put("action_executed", false);

        try {
            // Try to parse structured action from AI response
            Map<String, Object> action = parseActionFromResponse(aiResponse);
            if (action != null && !action.isEmpty()) {
                result = performAction(action);
                result.put("action_executed", true);
            }
        } catch (Exception e) {
            log.error("Error executing action: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
        }

        return result;
    }

    // ─────────────────────────────────────────────
    // PARSE ACTION FROM AI RESPONSE
    // Look for JSON action block in response
    // ─────────────────────────────────────────────
    private Map<String, Object> parseActionFromResponse(String response) {
        try {
            int start = response.indexOf("{");
            int end = response.lastIndexOf("}");
            
            if (start != -1 && end != -1 && end > start) {
                String jsonStr = response.substring(start, end + 1);
                Map<String, Object> parsed = objectMapper.readValue(jsonStr, Map.class);
                
                // If it successfully parsed, see if it has an "action" key
                if (parsed.containsKey("action")) {
                    return parsed;
                }
                
                // Fallback for LLM hallucinations where it uses empty string key: {"": "list_customers"}
                if (parsed.containsKey("")) {
                    String val = parsed.get("").toString();
                    if (val.contains("_")) {
                        parsed.put("action", val);
                        return parsed;
                    }
                }
            }
            
            return null;

        } catch (Exception e) {
            log.debug("No structured action found in response (Parse failed)");
            return null;
        }
    }

    // ─────────────────────────────────────────────
    // PERFORM ACTUAL DATABASE OPERATION
    // ─────────────────────────────────────────────
    private Map<String, Object> performAction(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        String actionType = (String) action.get("action");
        if (actionType == null) {
            result.put("success", false);
            result.put("message", "No action specified");
            return result;
        }

        try {
            switch (actionType.toLowerCase()) {
                // ═══════════════════════════════════════
                // CUSTOMER OPERATIONS
                // ═══════════════════════════════════════
                case "clear_udhari":
                    result = clearUdhari(action);
                    break;
                case "update_customer_balance":
                    result = updateCustomerBalance(action);
                    break;
                case "pay_udhari":
                    result = payUdhari(action);
                    break;
                case "create_customer":
                    result = createCustomer(action);
                    break;
                case "delete_customer":
                    result = deleteCustomer(action);
                    break;
                case "list_customers":
                    result = listCustomers(action);
                    break;
                case "list_customers_with_udhari":
                    result = listCustomersWithUdhari(action);
                    break;

                // ═══════════════════════════════════════
                // PRODUCT OPERATIONS
                // ═══════════════════════════════════════
                case "create_product":
                    result = createProduct(action);
                    break;
                case "delete_product":
                    result = deleteProduct(action);
                    break;
                case "update_stock":
                    result = updateProductStock(action);
                    break;
                case "update_product_price":
                    result = updateProductPrice(action);
                    break;
                case "list_products":
                    result = listProducts(action);
                    break;
                case "get_low_stock":
                    result = getLowStockProducts(action);
                    break;
                case "get_top_products":
                    result = getTopProducts(action);
                    break;

                // ═══════════════════════════════════════
                // SUPPLIER OPERATIONS
                // ═══════════════════════════════════════
                case "create_supplier":
                    result = createSupplier(action);
                    break;
                case "delete_supplier":
                    result = deleteSupplier(action);
                    break;
                case "record_supplier_payment":
                    result = recordSupplierPayment(action);
                    break;
                case "list_suppliers":
                    result = listSuppliers(action);
                    break;
                case "draft_purchase_order":
                    result = draftPurchaseOrder(action);
                    break;

                // ═══════════════════════════════════════
                // REPORTING & ANALYTICS
                // ═══════════════════════════════════════
                case "get_customer":
                case "get_customer_info":
                case "get_customer_details":
                    result = getCustomerInfo(action);
                    break;
                case "get_dashboard_summary":
                    result = getDashboardSummary(action);
                    break;
                case "get_sales_report":
                    result = getSalesReport(action);
                    break;
                case "get_profit_report":
                    result = getProfitReport(action);
                    break;
                case "get_credit_report":
                    result = getCreditReport(action);
                    break;
                case "get_inventory_status":
                    result = getInventoryStatus(action);
                    break;
                case "get_top_customers":
                    result = getTopCustomers(action);
                    break;
                case "require_confirmation":
                    result = requireConfirmation(action);
                    break;
                case "cancel_action":
                    result = cancelAction(action);
                    break;
                default:
                    result.put("success", false);
                    result.put("message", "Unknown action: " + actionType);
            }
        } catch (Exception e) {
            log.error("Error performing action: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
        }

        return result;
    }

    // ─────────────────────────────────────────────
    // ACTION: CLEAR UDHARI (SET BALANCE TO 0)
    // ─────────────────────────────────────────────
    private Map<String, Object> clearUdhari(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        String customerName = (String) action.get("customer_name");
        Long customerId = action.get("customer_id") != null
                ? Long.parseLong(action.get("customer_id").toString())
                : null;

        Customer customer = null;
        if (customerId != null) {
            customer = customerRepository.findById(customerId).orElse(null);
        } else if (customerName != null && !customerName.isEmpty()) {
            customer = findCustomerByNameFuzzy(customerName);
        }

        if (customer == null) {
            result.put("success", false);
            result.put("message", "Customer not found");
            return result;
        }

        BigDecimal previousBalance = customer.getBalance();
        customer.setBalance(BigDecimal.ZERO);
        customerRepository.save(customer);

        result.put("success", true);
        result.put("message", "Udhari cleared successfully");
        result.put("customer_id", customer.getId());
        result.put("customer_name", customer.getName());
        result.put("previous_balance", previousBalance);
        result.put("new_balance", BigDecimal.ZERO);

        log.info("Cleared udhari for {}: {} → 0",
                customer.getName(), previousBalance);

        return result;
    }

    // ─────────────────────────────────────────────
    // ACTION: UPDATE CUSTOMER BALANCE
    // ─────────────────────────────────────────────
    private Map<String, Object> updateCustomerBalance(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        String customerName = (String) action.get("customer_name");
        Long customerId = action.get("customer_id") != null
                ? Long.parseLong(action.get("customer_id").toString())
                : null;

        BigDecimal newBalance = action.get("balance") != null
                ? new BigDecimal(action.get("balance").toString())
                : null;

        if (newBalance == null) {
            result.put("success", false);
            result.put("message", "Balance amount not specified");
            return result;
        }

        Customer customer = null;
        if (customerId != null) {
            customer = customerRepository.findById(customerId).orElse(null);
        } else if (customerName != null && !customerName.isEmpty()) {
            customer = findCustomerByNameFuzzy(customerName);
        }

        if (customer == null) {
            result.put("success", false);
            result.put("message", "Customer not found");
            return result;
        }

        BigDecimal previousBalance = customer.getBalance();
        customer.setBalance(newBalance);
        customerRepository.save(customer);

        result.put("success", true);
        result.put("message", "Customer balance updated");
        result.put("customer_id", customer.getId());
        result.put("customer_name", customer.getName());
        result.put("previous_balance", previousBalance);
        result.put("new_balance", newBalance);

        log.info("Updated balance for {}: {} → {}",
                customer.getName(), previousBalance, newBalance);

        return result;
    }

    // ─────────────────────────────────────────────
    // ACTION: PAY UDHARI (REDUCE BALANCE)
    // ─────────────────────────────────────────────
    private Map<String, Object> payUdhari(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        String customerName = (String) action.get("customer_name");
        Long customerId = action.get("customer_id") != null
                ? Long.parseLong(action.get("customer_id").toString())
                : null;

        BigDecimal payAmount = action.get("amount") != null
                ? new BigDecimal(action.get("amount").toString())
                : null;

        if (payAmount == null) {
            result.put("success", false);
            result.put("message", "Payment amount not specified");
            return result;
        }

        Customer customer = null;
        if (customerId != null) {
            customer = customerRepository.findById(customerId).orElse(null);
        } else if (customerName != null && !customerName.isEmpty()) {
            customer = findCustomerByNameFuzzy(customerName);
        }

        if (customer == null) {
            result.put("success", false);
            result.put("message", "Customer not found");
            return result;
        }

        BigDecimal previousBalance = customer.getBalance();
        BigDecimal newBalance = previousBalance.subtract(payAmount);

        if (newBalance.signum() < 0) {
            result.put("success", false);
            result.put("message", "Payment exceeds pending balance");
            result.put("pending_balance", previousBalance);
            return result;
        }

        customer.setBalance(newBalance);
        customerRepository.save(customer);

        result.put("success", true);
        result.put("message", "Payment recorded");
        result.put("customer_id", customer.getId());
        result.put("customer_name", customer.getName());
        result.put("amount_paid", payAmount);
        result.put("previous_balance", previousBalance);
        result.put("new_balance", newBalance);

        log.info("Payment recorded for {}: -{}, balance {} → {}",
                customer.getName(), payAmount, previousBalance, newBalance);

        return result;
    }

    // ─────────────────────────────────────────────
    // ACTION: GET CUSTOMER INFO
    // ─────────────────────────────────────────────
    private Map<String, Object> getCustomerInfo(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        String customerName = (String) action.get("customer_name");
        Long customerId = action.get("customer_id") != null
                ? Long.parseLong(action.get("customer_id").toString())
                : null;

        Customer customer = null;
        if (customerId != null) {
            customer = customerRepository.findById(customerId).orElse(null);
        } else if (customerName != null && !customerName.isEmpty()) {
            customer = findCustomerByNameFuzzy(customerName);
        }

        if (customer == null) {
            result.put("success", false);
            result.put("message", "Customer not found");
            return result;
        }

        result.put("success", true);
        result.put("customer_id", customer.getId());
        result.put("customer_name", customer.getName());
        result.put("phone", customer.getPhone());
        result.put("balance", customer.getBalance());
        result.put("total_paid", customer.getTotalPaid());
        result.put("address", customer.getAddress());

        return result;
    }

    // ─────────────────────────────────────────────
    // HELPER: FUZZY CUSTOMER LOOKUP
    // ─────────────────────────────────────────────
    private Customer findCustomerByNameFuzzy(String searchName) {
        if (searchName == null || searchName.isEmpty()) {
            return null;
        }

        String search = searchName.toLowerCase().trim();

        // 1. Try exact match
        Customer exact = customerRepository.findAll().stream()
                .filter(c -> c.getName().equalsIgnoreCase(search))
                .findFirst()
                .orElse(null);
        if (exact != null) return exact;

        // 2. Try contains match (partial)
        Customer partial = customerRepository.findAll().stream()
                .filter(c -> c.getName().toLowerCase().contains(search) ||
                             search.contains(c.getName().toLowerCase().split(" ")[0]))
                .findFirst()
                .orElse(null);
        if (partial != null) return partial;

        // 3. Try first word match
        String[] words = search.split(" ");
        if (words.length > 0) {
            Customer firstWord = customerRepository.findAll().stream()
                    .filter(c -> c.getName().toLowerCase().startsWith(words[0]))
                    .findFirst()
                    .orElse(null);
            if (firstWord != null) return firstWord;
        }

        return null;
    }

    // ─────────────────────────────────────────────
    // ACTION: UPDATE PRODUCT STOCK
    // ─────────────────────────────────────────────
    private Map<String, Object> updateProductStock(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        String productName = (String) action.get("product_name");
        Long productId = action.get("product_id") != null
                ? Long.parseLong(action.get("product_id").toString())
                : null;

        Integer newStock = action.get("stock_qty") != null
                ? Integer.parseInt(action.get("stock_qty").toString())
                : null;

        String operation = (String) action.getOrDefault("operation", "set"); // set, add, reduce

        if (newStock == null && operation.equals("set")) {
            result.put("success", false);
            result.put("message", "Stock quantity not specified");
            return result;
        }

        Product product = null;
        if (productId != null) {
            product = productRepository.findById(productId).orElse(null);
        } else if (productName != null && !productName.isEmpty()) {
            product = productRepository.findAll().stream()
                    .filter(p -> p.getNameEn().equalsIgnoreCase(productName))
                    .findFirst()
                    .orElse(null);
        }

        if (product == null) {
            result.put("success", false);
            result.put("message", "Product not found");
            return result;
        }

        int previousStock = product.getStockQty();
        int finalStock = newStock;

        switch (operation.toLowerCase()) {
            case "add":
                finalStock = previousStock + (newStock != null ? newStock : 0);
                break;
            case "reduce":
                finalStock = previousStock - (newStock != null ? newStock : 0);
                if (finalStock < 0) {
                    result.put("success", false);
                    result.put("message", "Insufficient stock. Current: " + previousStock);
                    return result;
                }
                break;
            case "set":
            default:
                finalStock = newStock;
        }

        product.setStockQty(finalStock);
        productRepository.save(product);

        result.put("success", true);
        result.put("message", "Stock updated successfully");
        result.put("product_id", product.getId());
        result.put("product_name", product.getNameEn());
        result.put("previous_stock", previousStock);
        result.put("new_stock", finalStock);
        result.put("operation", operation);

        log.info("Updated stock for {}: {} → {} ({})",
                product.getNameEn(), previousStock, finalStock, operation);

        return result;
    }

    // ─────────────────────────────────────────────
    // ACTION: RECORD SUPPLIER PAYMENT
    // ─────────────────────────────────────────────
    private Map<String, Object> recordSupplierPayment(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        String supplierName = (String) action.get("supplier_name");
        Long supplierId = action.get("supplier_id") != null
                ? Long.parseLong(action.get("supplier_id").toString())
                : null;

        BigDecimal amount = action.get("amount") != null
                ? new BigDecimal(action.get("amount").toString())
                : null;

        String description = (String) action.getOrDefault("description", "Supplier payment");

        if (amount == null || amount.signum() <= 0) {
            result.put("success", false);
            result.put("message", "Valid payment amount required");
            return result;
        }

        Supplier supplier = null;
        if (supplierId != null) {
            supplier = supplierRepository.findById(supplierId).orElse(null);
        } else if (supplierName != null && !supplierName.isEmpty()) {
            supplier = supplierRepository.findAll().stream()
                    .filter(s -> s.getName().equalsIgnoreCase(supplierName))
                    .findFirst()
                    .orElse(null);
        }

        if (supplier == null) {
            result.put("success", false);
            result.put("message", "Supplier not found");
            return result;
        }

        // Record transaction
        result.put("success", true);
        result.put("message", "Payment recorded for supplier: " + supplierName);
        result.put("supplier_id", supplier.getId());
        result.put("supplier_name", supplier.getName());
        result.put("amount_paid", amount);
        result.put("payment_date", LocalDate.now());
        result.put("description", description);

        log.info("Payment recorded for supplier {}: ₹{}",
                supplier.getName(), amount);

        return result;
    }

    // ─────────────────────────────────────────────
    // ACTION: DRAFT PURCHASE ORDER
    // ─────────────────────────────────────────────
    private Map<String, Object> draftPurchaseOrder(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        String supplierName = (String) action.get("supplier_name");
        
        if (supplierName == null || supplierName.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Supplier name is required to draft a Purchase Order.");
            return result;
        }

        // 1. Find Supplier
        Supplier supplier = supplierRepository.findAll().stream()
                .filter(s -> s.getName().toLowerCase().contains(supplierName.toLowerCase()))
                .findFirst()
                .orElse(null);

        if (supplier == null) {
            result.put("success", false);
            result.put("message", "Supplier '" + supplierName + "' not found.");
            return result;
        }

        // 2. Find Low Stock Products (assuming < 15 is low)
        List<Product> lowStockProducts = productRepository.findAll().stream()
                .filter(p -> p.getActive() != null && p.getActive() && p.getStockQty() != null && p.getStockQty() < 15)
                .toList();

        if (lowStockProducts.isEmpty()) {
            result.put("success", false);
            result.put("message", "No low stock products found! Shop is fully stocked.");
            return result;
        }

        // 3. Draft the PO (Save directly without updating stock yet, just a draft!)
        String poNumber = purchaseOrderService.generatePoNumber();
        PurchaseOrder draftPo = PurchaseOrder.builder()
                .poNumber(poNumber)
                .supplier(supplier)
                .orderDate(LocalDate.now())
                .paymentStatus(PurchaseOrder.PaymentStatus.PENDING)
                .subtotal(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .amountPaid(BigDecimal.ZERO)
                .notes("AI Auto-Drafted Purchase Order")
                .build();
                
        // Initialize the items list before adding
        draftPo.setItems(new ArrayList<>());

        for (Product product : lowStockProducts) {
            BigDecimal costPrice = product.getCostPrice() != null ? product.getCostPrice() : product.getPrice().multiply(new BigDecimal("0.7")); // Guess cost price if null
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setProduct(product);
            item.setProductNameSnapshot(product.getNameEn());
            item.setQuantity(new BigDecimal("50")); // Order 50 items automatically
            item.setUnitCost(costPrice);
            item.calculateTotal();
            draftPo.addItem(item);
        }

        draftPo.calculateTotals();
        
        // Use the saveDirectly method so it doesn't instantly add stock and modify supplier balances
        PurchaseOrder savedDraft = purchaseOrderService.saveDirectly(draftPo);

        result.put("success", true);
        result.put("message", "Successfully drafted Purchase Order <a href='/purchases/" + savedDraft.getId() + "' target='_blank'>" + poNumber + "</a> with " + lowStockProducts.size() + " items automatically.");
        result.put("po_number", poNumber);
        result.put("supplier_name", supplier.getName());
        result.put("item_count", lowStockProducts.size());
        
        log.info("AI drafted PO {} for {}", poNumber, supplier.getName());

        return result;
    }

    // ─────────────────────────────────────────────
    // ACTION: REQUIRE CONFIRMATION
    // For risky operations (deletion, large amounts, etc)
    // ─────────────────────────────────────────────
    private Map<String, Object> requireConfirmation(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        String operationType = (String) action.get("operation_type");
        String message = (String) action.get("message");
        Map<String, Object> pendingAction = (Map<String, Object>) action.get("pending_action");

        if (operationType == null || message == null) {
            result.put("success", false);
            result.put("message", "Missing operation details");
            return result;
        }

        result.put("success", true);
        result.put("pending_confirmation", true);
        result.put("confirmation_required", true);
        result.put("operation_type", operationType);
        result.put("message", message);
        result.put("pending_action", pendingAction);

        log.info("Confirmation required for: {}", operationType);

        return result;
    }

    // ═════════════════════════════════════════════════
    // CUSTOMER OPERATIONS
    // ═════════════════════════════════════════════════

    private Map<String, Object> createCustomer(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        String name = (String) action.get("name");
        String phone = (String) action.get("phone");
        String address = (String) action.get("address");

        if (name == null || name.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Customer name is required");
            return result;
        }

        if (phone != null) {
            phone = phone.replaceAll("\\D+", "");
            if (phone.length() > 10) {
                phone = phone.substring(phone.length() - 10);
            }
        } else {
            phone = "";
        }

        if (!phone.matches("^[6-9]\\d{9}$")) {
            result.put("success", false);
            result.put("message", "Invalid 10-digit Indian mobile number format. Got: " + phone);
            return result;
        }

        if (customerRepository.findByPhone(phone).isPresent()) {
            result.put("success", false);
            result.put("message", "A customer with this phone number (" + phone + ") already exists in our system.");
            return result;
        }

        Customer customer = new Customer();
        customer.setName(name.trim());
        customer.setPhone(phone);
        customer.setAddress(address != null ? address.trim() : "");
        customer.setActive(true);
        customer.setBalance(BigDecimal.ZERO);

        Customer saved = customerRepository.save(customer);

        result.put("success", true);
        result.put("message", "Customer created successfully");
        result.put("customer_id", saved.getId());
        result.put("customer_name", saved.getName());

        log.info("Customer created: {}", name);
        return result;
    }

    private Map<String, Object> deleteCustomer(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        String customerName = (String) action.get("customer_name");
        Long customerId = action.get("customer_id") != null
                ? Long.parseLong(action.get("customer_id").toString())
                : null;

        Customer customer = null;
        if (customerId != null) {
            customer = customerRepository.findById(customerId).orElse(null);
        } else if (customerName != null && !customerName.isEmpty()) {
            customer = findCustomerByNameFuzzy(customerName);
        }

        if (customer == null) {
            result.put("success", false);
            result.put("message", "Customer not found");
            return result;
        }

        customer.setActive(false);
        customerRepository.save(customer);

        result.put("success", true);
        result.put("message", "Customer deleted");
        result.put("customer_id", customer.getId());
        result.put("customer_name", customer.getName());

        log.info("Customer deleted: {}", customer.getName());
        return result;
    }

    private Map<String, Object> listCustomers(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();
        List<Customer> customers = customerService.getAllActiveCustomers();

        List<Map<String, Object>> customerList = customers.stream()
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", c.getId());
                    m.put("name", c.getName());
                    m.put("phone", c.getPhone());
                    m.put("balance", c.getBalance());
                    m.put("address", c.getAddress());
                    return m;
                })
                .limit(10)
                .toList();

        result.put("success", true);
        result.put("customers", customerList);
        result.put("total_count", customers.size());
        result.put("shown_count", customerList.size());

        return result;
    }

    private Map<String, Object> listCustomersWithUdhari(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();
        List<Customer> customersWithBalance = customerRepository
                .findByBalanceGreaterThanAndActiveTrue(BigDecimal.ZERO);

        List<Map<String, Object>> customerList = customersWithBalance.stream()
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", c.getId());
                    m.put("name", c.getName());
                    m.put("phone", c.getPhone());
                    m.put("pending_udhari", c.getBalance());
                    m.put("address", c.getAddress());
                    return m;
                })
                .limit(20)
                .toList();

        result.put("success", true);
        result.put("customers_with_udhari", customerList);
        result.put("total_count", customersWithBalance.size());
        result.put("shown_count", customerList.size());

        return result;
    }

    private Map<String, Object> createProduct(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        String nameEn = (String) action.get("name_en");
        String nameHi = (String) action.get("name_hi");
        String nameMr = (String) action.get("name_mr");

        Object priceObj = action.get("price");
        Object costObj = action.get("cost_price");
        Object stockObj = action.get("stock_qty");
        String category = (String) action.get("category");

        if (nameEn == null || nameEn.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Product name in English is required");
            return result;
        }

        Product product = new Product();
        product.setNameEn(nameEn.trim());
        product.setNameHi(nameHi != null ? nameHi.trim() : "");
        product.setNameMr(nameMr != null ? nameMr.trim() : "");
        product.setCategory(category != null ? category.trim() : "General");
        product.setActive(true);

        if (priceObj != null) {
            product.setPrice(new BigDecimal(priceObj.toString()));
        }
        if (costObj != null) {
            product.setCostPrice(new BigDecimal(costObj.toString()));
        }
        if (stockObj != null) {
            product.setStockQty(Integer.parseInt(stockObj.toString()));
        }

        Product saved = productRepository.save(product);

        result.put("success", true);
        result.put("message", "Product created successfully");
        result.put("product_id", saved.getId());
        result.put("product_name", saved.getNameEn());

        log.info("Product created: {}", nameEn);
        return result;
    }

    private Map<String, Object> deleteProduct(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        String productName = (String) action.get("product_name");
        Long productId = action.get("product_id") != null
                ? Long.parseLong(action.get("product_id").toString())
                : null;

        Product product = null;
        if (productId != null) {
            product = productRepository.findById(productId).orElse(null);
        } else if (productName != null && !productName.isEmpty()) {
            product = productRepository.findAll().stream()
                    .filter(p -> p.getNameEn().equalsIgnoreCase(productName))
                    .findFirst()
                    .orElse(null);
        }

        if (product == null) {
            result.put("success", false);
            result.put("message", "Product not found");
            return result;
        }

        product.setActive(false);
        productRepository.save(product);

        result.put("success", true);
        result.put("message", "Product deleted");
        result.put("product_id", product.getId());
        result.put("product_name", product.getNameEn());

        log.info("Product deleted: {}", product.getNameEn());
        return result;
    }

    private Map<String, Object> updateProductPrice(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        String productName = (String) action.get("product_name");
        Long productId = action.get("product_id") != null
                ? Long.parseLong(action.get("product_id").toString())
                : null;

        BigDecimal newPrice = action.get("price") != null
                ? new BigDecimal(action.get("price").toString())
                : null;

        if (newPrice == null || newPrice.signum() <= 0) {
            result.put("success", false);
            result.put("message", "Valid price is required");
            return result;
        }

        Product product = null;
        if (productId != null) {
            product = productRepository.findById(productId).orElse(null);
        } else if (productName != null && !productName.isEmpty()) {
            product = productRepository.findAll().stream()
                    .filter(p -> p.getNameEn().equalsIgnoreCase(productName))
                    .findFirst()
                    .orElse(null);
        }

        if (product == null) {
            result.put("success", false);
            result.put("message", "Product not found");
            return result;
        }

        BigDecimal oldPrice = product.getPrice();
        product.setPrice(newPrice);
        productRepository.save(product);

        result.put("success", true);
        result.put("message", "Product price updated");
        result.put("product_id", product.getId());
        result.put("product_name", product.getNameEn());
        result.put("old_price", oldPrice);
        result.put("new_price", newPrice);

        log.info("Product price updated: {} ₹{} → ₹{}", product.getNameEn(), oldPrice, newPrice);
        return result;
    }

    private Map<String, Object> listProducts(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();
        List<Product> products = productService.getAllActiveProducts();

        List<Map<String, Object>> productList = products.stream()
                .map(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", p.getId());
                    m.put("name", p.getNameEn());
                    m.put("price", p.getPrice());
                    m.put("stock", p.getStockQty());
                    m.put("category", p.getCategory());
                    return m;
                })
                .limit(10)
                .toList();

        result.put("success", true);
        result.put("products", productList);
        result.put("total_count", products.size());
        result.put("shown_count", productList.size());

        return result;
    }

    private Map<String, Object> getLowStockProducts(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        Integer threshold = action.get("threshold") != null
                ? Integer.parseInt(action.get("threshold").toString())
                : 10;

        List<Product> lowStock = productRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .filter(p -> p.getStockQty() < threshold)
                .limit(15)
                .toList();

        List<Map<String, Object>> items = lowStock.stream()
                .map(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", p.getId());
                    m.put("name", p.getNameEn());
                    m.put("stock", p.getStockQty());
                    m.put("threshold", threshold);
                    return m;
                })
                .toList();

        result.put("success", true);
        result.put("low_stock_products", items);
        result.put("count", items.size());
        result.put("threshold", threshold);

        return result;
    }

    private Map<String, Object> getTopProducts(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        int limit = action.get("limit") != null
                ? Integer.parseInt(action.get("limit").toString())
                : 5;

        // Get products by bill count
        Map<Long, Integer> productCount = new HashMap<>();

        billRepository.findAll().forEach(bill -> {
            bill.getBillItems().forEach(item -> {
                Long pid = item.getProduct().getId();
                productCount.merge(pid, 1, Integer::sum);
            });
        });

        List<Map<String, Object>> topProducts = productCount.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(e -> {
                    Product p = productRepository.findById(e.getKey()).orElse(null);
                    Map<String, Object> m = new HashMap<>();
                    if (p != null) {
                        m.put("product_id", p.getId());
                        m.put("product_name", p.getNameEn());
                        m.put("sold_count", e.getValue());
                        m.put("price", p.getPrice());
                    }
                    return m;
                })
                .toList();

        result.put("success", true);
        result.put("top_products", topProducts);
        result.put("count", topProducts.size());

        return result;
    }

    // ═════════════════════════════════════════════════
    // SUPPLIER OPERATIONS
    // ═════════════════════════════════════════════════

    private Map<String, Object> createSupplier(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        String name = (String) action.get("name");
        String phone = (String) action.get("phone");
        String address = (String) action.get("address");
        String gst = (String) action.get("gst");

        if (name == null || name.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Supplier name is required");
            return result;
        }

        Supplier supplier = new Supplier();
        supplier.setName(name.trim());
        supplier.setPhone(phone != null ? phone.trim() : "");
        supplier.setAddress(address != null ? address.trim() : "");
        supplier.setGstin(gst != null ? gst.trim() : "");
        supplier.setActive(true);

        Supplier saved = supplierRepository.save(supplier);

        result.put("success", true);
        result.put("message", "Supplier created successfully");
        result.put("supplier_id", saved.getId());
        result.put("supplier_name", saved.getName());

        log.info("Supplier created: {}", name);
        return result;
    }

    private Map<String, Object> deleteSupplier(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        String supplierName = (String) action.get("supplier_name");
        Long supplierId = action.get("supplier_id") != null
                ? Long.parseLong(action.get("supplier_id").toString())
                : null;

        Supplier supplier = null;
        if (supplierId != null) {
            supplier = supplierRepository.findById(supplierId).orElse(null);
        } else if (supplierName != null && !supplierName.isEmpty()) {
            supplier = supplierRepository.findAll().stream()
                    .filter(s -> s.getName().equalsIgnoreCase(supplierName))
                    .findFirst()
                    .orElse(null);
        }

        if (supplier == null) {
            result.put("success", false);
            result.put("message", "Supplier not found");
            return result;
        }

        supplier.setActive(false);
        supplierRepository.save(supplier);

        result.put("success", true);
        result.put("message", "Supplier deleted");
        result.put("supplier_id", supplier.getId());
        result.put("supplier_name", supplier.getName());

        log.info("Supplier deleted: {}", supplier.getName());
        return result;
    }

    private Map<String, Object> listSuppliers(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();
        List<Supplier> suppliers = supplierService.getAllActive();

        List<Map<String, Object>> supplierList = suppliers.stream()
                .map(s -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", s.getId());
                    m.put("name", s.getName());
                    m.put("phone", s.getPhone());
                    m.put("address", s.getAddress());
                    m.put("gst", s.getGstin());
                    return m;
                })
                .limit(10)
                .toList();

        result.put("success", true);
        result.put("suppliers", supplierList);
        result.put("total_count", suppliers.size());
        result.put("shown_count", supplierList.size());

        return result;
    }

    // ═════════════════════════════════════════════════
    // REPORTING & ANALYTICS
    // ═════════════════════════════════════════════════

    private Map<String, Object> getDashboardSummary(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        LocalDate today = LocalDate.now();
        BigDecimal todaySales = billRepository.getTotalSalesByDate(today);
        BigDecimal totalPending = customerRepository.getTotalPendingBalance();
        long totalBills = billRepository.count();
        long totalCustomers = customerRepository.findByActiveTrueOrderByNameAsc().size();
        long totalProducts = productRepository.findByActiveTrueOrderByNameEnAsc().size();

        result.put("success", true);
        result.put("today_sales", todaySales != null ? todaySales : BigDecimal.ZERO);
        result.put("total_pending_balance", totalPending != null ? totalPending : BigDecimal.ZERO);
        result.put("total_bills", totalBills);
        result.put("total_customers", totalCustomers);
        result.put("total_products", totalProducts);
        result.put("date", today);

        return result;
    }

    private Map<String, Object> getSalesReport(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        LocalDate date = LocalDate.now();
        if (action.get("date") != null) {
            date = LocalDate.parse(action.get("date").toString());
        }

        BigDecimal sales = billRepository.getTotalSalesByDate(date);
        long billCount = billRepository.countByBillDate(date);

        result.put("success", true);
        result.put("date", date);
        result.put("total_sales", sales != null ? sales : BigDecimal.ZERO);
        result.put("bill_count", billCount);

        return result;
    }

    private Map<String, Object> getProfitReport(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        result.put("success", true);
        result.put("message", "Profit calculated based on product cost and selling price");
        result.put("note", "Detailed profit metrics available in Reports section");

        return result;
    }

    private Map<String, Object> getCreditReport(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        List<Customer> customersWithBalance = customerRepository
                .findByBalanceGreaterThanAndActiveTrue(BigDecimal.ZERO);

        BigDecimal totalPending = customersWithBalance.stream()
                .map(Customer::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> customerList = customersWithBalance.stream()
                .limit(10)
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("customer_name", c.getName());
                    m.put("pending_amount", c.getBalance());
                    m.put("phone", c.getPhone());
                    return m;
                })
                .toList();

        result.put("success", true);
        result.put("total_pending", totalPending);
        result.put("customer_count", customersWithBalance.size());
        result.put("customers_with_credit", customerList);

        return result;
    }

    private Map<String, Object> getInventoryStatus(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        List<Product> products = productService.getAllActiveProducts();

        long totalItems = products.stream()
                .mapToLong(Product::getStockQty)
                .sum();

        long lowStockCount = products.stream()
                .filter(p -> p.getStockQty() < 10)
                .count();

        result.put("success", true);
        result.put("total_products", products.size());
        result.put("total_stock_qty", totalItems);
        result.put("low_stock_count", lowStockCount);
        result.put("status", lowStockCount > 0 ? "⚠️ Low stock items found" : "✅ Inventory normal");

        return result;
    }

    private Map<String, Object> getTopCustomers(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        int limit = action.get("limit") != null
                ? Integer.parseInt(action.get("limit").toString())
                : 5;

        // Get customers by bill count and total spent
        Map<Long, Integer> customerBills = new HashMap<>();
        Map<Long, BigDecimal> customerAmount = new HashMap<>();

        billRepository.findAll().forEach(bill -> {
            if (bill.getCustomer() != null) {
                Long cid = bill.getCustomer().getId();
                customerBills.merge(cid, 1, Integer::sum);
                customerAmount.merge(cid,
                        bill.getTotalAmount() != null ? bill.getTotalAmount() : BigDecimal.ZERO,
                        BigDecimal::add);
            }
        });

        List<Map<String, Object>> topCustomers = customerBills.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(e -> {
                    Customer c = customerRepository.findById(e.getKey()).orElse(null);
                    Map<String, Object> m = new HashMap<>();
                    if (c != null) {
                        m.put("customer_id", c.getId());
                        m.put("customer_name", c.getName());
                        m.put("bill_count", e.getValue());
                        m.put("total_spent", customerAmount.getOrDefault(e.getKey(), BigDecimal.ZERO));
                    }
                    return m;
                })
                .toList();

        result.put("success", true);
        result.put("top_customers", topCustomers);
        result.put("count", topCustomers.size());

        return result;
    }

    // ─────────────────────────────────────────────
    // ACTION: CANCEL ACTION
    // ─────────────────────────────────────────────
    private Map<String, Object> cancelAction(Map<String, Object> action) {
        Map<String, Object> result = new HashMap<>();

        String message = (String) action.getOrDefault("message", "Action cancelled");

        result.put("success", true);
        result.put("cancelled", true);
        result.put("message", message);

        log.info("Action cancelled: {}", message);

        return result;
    }
}
