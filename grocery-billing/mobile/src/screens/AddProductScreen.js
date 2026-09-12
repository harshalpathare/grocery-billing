import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, Alert, ScrollView, ActivityIndicator } from 'react-native';
import api from '../api/axiosConfig';

export default function AddProductScreen({ navigation }) {
  const [nameEn, setNameEn] = useState('');
  const [nameHi, setNameHi] = useState('');
  const [sku, setSku] = useState('');
  const [salesPrice, setSalesPrice] = useState('');
  const [costPrice, setCostPrice] = useState('');
  const [stock, setStock] = useState('');
  const [unit, setUnit] = useState('piece');
  const [loading, setLoading] = useState(false);

  const handleSave = async () => {
    if (!nameEn.trim() || !salesPrice.trim()) {
      Alert.alert('Validation Error', 'English Name and Sales Price are required.');
      return;
    }

    setLoading(true);
    try {
      await api.post('/products', {
        nameEn,
        nameHi,
        sku,
        salesPrice: parseFloat(salesPrice),
        costPrice: costPrice ? parseFloat(costPrice) : null,
        stock: stock ? parseFloat(stock) : 0,
        unit,
        gstPercent: 0
      });
      Alert.alert('Success', 'Product added successfully!', [
        { text: 'OK', onPress: () => navigation.goBack() }
      ]);
    } catch (error) {
      console.error('Error adding product:', error);
      Alert.alert('Error', 'Failed to add product.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <ScrollView style={styles.container}>
      <Text style={styles.label}>Product Name (English) *</Text>
      <TextInput style={styles.input} value={nameEn} onChangeText={setNameEn} placeholder="e.g. Sugar 1kg" />

      <Text style={styles.label}>Product Name (Hindi)</Text>
      <TextInput style={styles.input} value={nameHi} onChangeText={setNameHi} placeholder="e.g. चीनी 1kg" />

      <Text style={styles.label}>SKU / Barcode</Text>
      <TextInput style={styles.input} value={sku} onChangeText={setSku} placeholder="Optional SKU" />

      <View style={styles.row}>
        <View style={styles.half}>
          <Text style={styles.label}>Sales Price (₹) *</Text>
          <TextInput style={styles.input} value={salesPrice} onChangeText={setSalesPrice} placeholder="0.00" keyboardType="numeric" />
        </View>
        <View style={styles.half}>
          <Text style={styles.label}>Cost Price (₹)</Text>
          <TextInput style={styles.input} value={costPrice} onChangeText={setCostPrice} placeholder="0.00" keyboardType="numeric" />
        </View>
      </View>

      <View style={styles.row}>
        <View style={styles.half}>
          <Text style={styles.label}>Opening Stock</Text>
          <TextInput style={styles.input} value={stock} onChangeText={setStock} placeholder="0" keyboardType="numeric" />
        </View>
        <View style={styles.half}>
          <Text style={styles.label}>Unit</Text>
          <TextInput style={styles.input} value={unit} onChangeText={setUnit} placeholder="kg, piece, liter" />
        </View>
      </View>

      <TouchableOpacity style={styles.saveButton} onPress={handleSave} disabled={loading}>
        {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.saveButtonText}>Save Product</Text>}
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f7fa', padding: 16 },
  label: { fontSize: 14, color: '#4a5568', marginBottom: 4, fontWeight: '600' },
  input: {
    backgroundColor: '#fff', borderWidth: 1, borderColor: '#e2e8f0', borderRadius: 8,
    padding: 12, marginBottom: 16, fontSize: 16, color: '#2d3748'
  },
  row: { flexDirection: 'row', justifyContent: 'space-between' },
  half: { width: '48%' },
  saveButton: {
    backgroundColor: '#48bb78', padding: 16, borderRadius: 8, alignItems: 'center', marginTop: 8, marginBottom: 40
  },
  saveButtonText: { color: '#fff', fontSize: 16, fontWeight: 'bold' }
});
