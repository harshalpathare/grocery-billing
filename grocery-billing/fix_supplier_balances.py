import mysql.connector

try:
    conn = mysql.connector.connect(
        host='localhost',
        user='root',
        password='Hp@9511896490',
        database='bohara_db'
    )
    cursor = conn.cursor(dictionary=True)

    # Get all suppliers
    cursor.execute('SELECT id, name FROM suppliers')
    suppliers = cursor.fetchall()

    for supplier in suppliers:
        supplier_id = supplier['id']
        
        # Recalculate totalPayable and totalPaid based on purchase orders
        cursor.execute('''
            SELECT 
                SUM(CASE WHEN type = 'RETURN' THEN -IFNULL(total_amount, 0) ELSE IFNULL(total_amount, 0) END) as calc_payable,
                SUM(IFNULL(amount_paid, 0)) as calc_paid
            FROM purchase_orders 
            WHERE supplier_id = %s
        ''', (supplier_id,))
        
        result = cursor.fetchone()
        
        calc_payable = result['calc_payable'] or 0
        calc_paid = result['calc_paid'] or 0
        calc_balance = calc_payable - calc_paid
        
        print(f"Supplier: {supplier['name']}")
        print(f"  Calculated Payable: {calc_payable}")
        print(f"  Calculated Paid: {calc_paid}")
        print(f"  Calculated Balance: {calc_balance}")
        
        # Update the supplier record
        cursor.execute('''
            UPDATE suppliers 
            SET total_payable = %s, total_paid = %s, balance = %s 
            WHERE id = %s
        ''', (calc_payable, calc_paid, calc_balance, supplier_id))
        
    conn.commit()
    print("Successfully updated all supplier balances.")
    
except Exception as e:
    print(f"Error: {e}")
finally:
    if 'conn' in locals() and conn.is_connected():
        cursor.close()
        conn.close()
