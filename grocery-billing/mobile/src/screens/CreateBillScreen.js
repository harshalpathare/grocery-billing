import React, { useState, useEffect } from 'react';
import { View, Text, FlatList, TouchableOpacity, StyleSheet, ActivityIndicator, Alert, Modal, TextInput } from 'react-native';
import api from '../api/axiosConfig';

export default function CreateBillScreen({ navigation }) {
  const [products, setProducts] = useState([]);
  const [customers, setCustomers] = useState([]);
  
  const [cart, setCart] = useState([]);
  const [selectedCustomerId, setSelectedCustomerId] = useState(null);
  const [walkInName, setWalkInName] = useState('');
  
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [cartVisible, setCartVisible] = useState(false);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [prodRes, custRes] = await Promise.all([
        api.get('/products'),
        api.get('/customers')
      ]);
      setProducts(Array.isArray(prodRes.data) ? prodRes.data : []);
      setCustomers(Array.isArray(custRes.data) ? custRes.data : []);
    } catch (error) {
      console.error('Error fetching data for billing:', error);
      Alert.alert('Error', 'Failed to load products and customers.');
    } finally {
      setLoading(false);
    }
  };

  const addToCart = (product) => {
    setCart(prev => {
      const existing = prev.find(item => item.productId === product.id);
      if (existing) {
        return prev.map(item => item.productId === product.id ? { ...item, quantity: item.quantity + 1 } : item);
      }
      return [...prev, { 
        productId: product.id, 
        productName: product.nameEn, 
        quantity: 1, 
        unitPrice: product.salesPrice,
        isReturn: false
      }];
    });
  };

  const removeFromCart = (productId) => {
    setCart(prev => prev.filter(item => item.productId !== productId));
  };

  const calculateTotal = () => {
    return cart.reduce((total, item) => total + (item.quantity * item.unitPrice), 0);
  };

  const handleCheckout = async () => {
    if (cart.length === 0) {
      Alert.alert('Empty Cart', 'Please add products to the bill.');
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        customerId: selectedCustomerId,
        walkInCustomerName: selectedCustomerId ? null : (walkInName || 'Walk-in'),
        isGst: false,
        paymentStatus: 'PAID',
        paymentMethod: 'CASH',
        items: cart,
        amountPaid: calculateTotal()
      };

      await api.post('/bills', payload);
      Alert.alert('Success', 'Bill generated successfully!', [
        { text: 'OK', onPress: () => {
            setCart([]);
            setCartVisible(false);
            navigation.goBack();
          } 
        }
      ]);
    } catch (error) {
      console.error('Error creating bill:', error);
      Alert.alert('Error', 'Failed to generate bill.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" color="#4299e1" />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.headerTitle}>Select Products</Text>
      <FlatList
        data={products}
        keyExtractor={(item) => item.id.toString()}
        renderItem={({ item }) => (
          <TouchableOpacity style={styles.productCard} onPress={() => addToCart(item)}>
            <View>
              <Text style={styles.productName}>{item.nameEn}</Text>
              <Text style={styles.productPrice}>₹{item.salesPrice}</Text>
            </View>
            <View style={styles.addButton}><Text style={styles.addText}>+</Text></View>
          </TouchableOpacity>
        )}
      />

      {cart.length > 0 && (
        <TouchableOpacity style={styles.cartBar} onPress={() => setCartVisible(true)}>
          <Text style={styles.cartBarText}>{cart.length} Items in Cart</Text>
          <Text style={styles.cartBarText}>Total: ₹{calculateTotal()}</Text>
        </TouchableOpacity>
      )}

      {/* Cart Checkout Modal */}
      <Modal visible={cartVisible} animationType="slide">
        <View style={styles.modalContainer}>
          <Text style={styles.modalTitle}>Checkout</Text>
          
          <Text style={styles.label}>Customer (Select or type)</Text>
          <View style={styles.customerScroll}>
             <ScrollView horizontal showsHorizontalScrollIndicator={false}>
               <TouchableOpacity 
                 style={[styles.customerChip, !selectedCustomerId && styles.customerChipActive]}
                 onPress={() => setSelectedCustomerId(null)}>
                 <Text style={[styles.chipText, !selectedCustomerId && styles.chipTextActive]}>Walk-in</Text>
               </TouchableOpacity>
               {customers.map(c => (
                 <TouchableOpacity 
                   key={c.id} 
                   style={[styles.customerChip, selectedCustomerId === c.id && styles.customerChipActive]}
                   onPress={() => setSelectedCustomerId(c.id)}>
                   <Text style={[styles.chipText, selectedCustomerId === c.id && styles.chipTextActive]}>{c.name}</Text>
                 </TouchableOpacity>
               ))}
             </ScrollView>
          </View>

          {!selectedCustomerId && (
            <TextInput 
              style={styles.input} 
              placeholder="Walk-in Customer Name" 
              value={walkInName} 
              onChangeText={setWalkInName} 
            />
          )}

          <Text style={styles.label}>Cart Items</Text>
          <FlatList
            data={cart}
            keyExtractor={(item) => item.productId.toString()}
            renderItem={({ item }) => (
              <View style={styles.cartItemRow}>
                <Text style={styles.cartItemName}>{item.productName} (x{item.quantity})</Text>
                <Text style={styles.cartItemPrice}>₹{item.unitPrice * item.quantity}</Text>
                <TouchableOpacity onPress={() => removeFromCart(item.productId)}>
                  <Text style={styles.removeText}>X</Text>
                </TouchableOpacity>
              </View>
            )}
          />

          <View style={styles.totalRow}>
            <Text style={styles.totalText}>Grand Total</Text>
            <Text style={styles.totalText}>₹{calculateTotal()}</Text>
          </View>

          <View style={styles.modalActions}>
            <TouchableOpacity style={styles.cancelBtn} onPress={() => setCartVisible(false)}>
              <Text style={styles.cancelBtnText}>Back</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.checkoutBtn} onPress={handleCheckout} disabled={submitting}>
              {submitting ? <ActivityIndicator color="#fff" /> : <Text style={styles.checkoutBtnText}>Confirm Bill</Text>}
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f7fa', padding: 16 },
  centerContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  headerTitle: { fontSize: 20, fontWeight: 'bold', color: '#2d3748', marginBottom: 12 },
  productCard: {
    backgroundColor: '#fff', padding: 16, borderRadius: 8, marginBottom: 8,
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
    shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.1, shadowRadius: 2, elevation: 2,
  },
  productName: { fontSize: 16, fontWeight: 'bold', color: '#2d3748' },
  productPrice: { fontSize: 14, color: '#48bb78', marginTop: 4, fontWeight: '600' },
  addButton: { backgroundColor: '#e2e8f0', width: 32, height: 32, borderRadius: 16, justifyContent: 'center', alignItems: 'center' },
  addText: { fontSize: 20, color: '#4a5568', fontWeight: 'bold' },
  cartBar: {
    backgroundColor: '#3182ce', padding: 16, borderRadius: 8, flexDirection: 'row', justifyContent: 'space-between',
    position: 'absolute', bottom: 16, left: 16, right: 16, elevation: 5
  },
  cartBarText: { color: '#fff', fontSize: 16, fontWeight: 'bold' },
  modalContainer: { flex: 1, padding: 20, backgroundColor: '#f5f7fa', marginTop: 40 },
  modalTitle: { fontSize: 24, fontWeight: 'bold', color: '#2d3748', marginBottom: 20 },
  label: { fontSize: 14, fontWeight: '600', color: '#4a5568', marginBottom: 8, marginTop: 12 },
  customerScroll: { marginBottom: 16 },
  customerChip: { backgroundColor: '#e2e8f0', paddingHorizontal: 16, paddingVertical: 8, borderRadius: 20, marginRight: 8 },
  customerChipActive: { backgroundColor: '#3182ce' },
  chipText: { color: '#4a5568', fontWeight: '600' },
  chipTextActive: { color: '#fff' },
  input: { backgroundColor: '#fff', padding: 12, borderRadius: 8, borderWidth: 1, borderColor: '#e2e8f0', marginBottom: 16 },
  cartItemRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: 12, borderBottomWidth: 1, borderBottomColor: '#e2e8f0' },
  cartItemName: { flex: 1, fontSize: 16, color: '#2d3748' },
  cartItemPrice: { fontSize: 16, fontWeight: '600', color: '#2d3748', marginRight: 16 },
  removeText: { color: '#e53e3e', fontSize: 16, fontWeight: 'bold', padding: 8 },
  totalRow: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 16, borderTopWidth: 2, borderTopColor: '#cbd5e0', marginTop: 16 },
  totalText: { fontSize: 20, fontWeight: 'bold', color: '#2d3748' },
  modalActions: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 24 },
  cancelBtn: { padding: 16, width: '48%', backgroundColor: '#e2e8f0', borderRadius: 8, alignItems: 'center' },
  cancelBtnText: { color: '#4a5568', fontWeight: 'bold', fontSize: 16 },
  checkoutBtn: { padding: 16, width: '48%', backgroundColor: '#48bb78', borderRadius: 8, alignItems: 'center' },
  checkoutBtnText: { color: '#fff', fontWeight: 'bold', fontSize: 16 }
});
