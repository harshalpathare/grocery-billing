package com.example.grocery_billing.service;

import com.example.grocery_billing.config.ShopContext;
import com.example.grocery_billing.entity.CashFlow;
import com.example.grocery_billing.entity.Expense;
import com.example.grocery_billing.entity.Shop;
import com.example.grocery_billing.repository.CashFlowRepository;
import com.example.grocery_billing.repository.ExpenseRepository;
import com.example.grocery_billing.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ShopRepository shopRepository;
    private final CashFlowRepository cashFlowRepository;

    private Long shopId() {
        return ShopContext.getShopId();
    }

    private Shop currentShop() {
        Long id = shopId();
        if (id == null) throw new RuntimeException("No shop in context");
        return shopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop not found"));
    }

    public List<String> getExpenseCategories() {
        return Arrays.asList("Rent", "Salary", "Electricity", "Fuel", "Internet", "Repairs", "Miscellaneous");
    }

    public List<Expense> getAllExpenses() {
        return expenseRepository.findByShopIdOrderByExpenseDateDescCreatedAtDesc(shopId());
    }

    public List<Expense> getExpensesBetween(LocalDate start, LocalDate end) {
        return expenseRepository.findByShopIdAndExpenseDateBetweenOrderByExpenseDateDesc(shopId(), start, end);
    }

    public List<Expense> getExpensesByCategoryAndDate(String category, LocalDate start, LocalDate end) {
        if (category == null || category.isEmpty() || "All".equalsIgnoreCase(category)) {
            return getExpensesBetween(start, end);
        }
        return expenseRepository.findByShopIdAndCategoryAndExpenseDateBetweenOrderByExpenseDateDesc(shopId(), category, start, end);
    }

    public List<Object[]> getCategoryWiseReport(LocalDate start, LocalDate end) {
        return expenseRepository.getCategoryWiseTotal(shopId(), start, end);
    }

    @Transactional
    public Expense saveExpense(Expense expense) {
        if (expense.getShop() == null) {
            expense.setShop(currentShop());
        }
        if (expense.getExpenseDate() == null) {
            expense.setExpenseDate(LocalDate.now());
        }
        
        boolean isNew = (expense.getId() == null);
        Expense saved = expenseRepository.save(expense);
        
        // Auto cash flow log
        if (isNew) {
            CashFlow cf = new CashFlow();
            cf.setShop(currentShop());
            cf.setTransactionDate(saved.getExpenseDate());
            cf.setType(CashFlow.TransactionType.OUT);
            cf.setAmount(saved.getAmount());
            cf.setCategory("Expense: " + saved.getCategory());
            cf.setDescription(saved.getNote() != null ? saved.getNote() : "Expense");
            cashFlowRepository.save(cf);
        }
        
        return saved;
    }

    @Transactional
    public void deleteExpense(Long id) {
        expenseRepository.deleteById(id);
    }
}
