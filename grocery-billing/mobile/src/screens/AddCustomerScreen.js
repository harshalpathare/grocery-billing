import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, Alert, ScrollView, ActivityIndicator } from 'react-native';
import api from '../api/axiosConfig';

export default function AddCustomerScreen({ navigation }) {
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [address, setAddress] = useState('');
  const [gstin, setGstin] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSave = async () => {
    if (!name.trim() || !phone.trim()) {
      Alert.alert('Validation Error', 'Name and Phone are required.');
      return;
    }

    setLoading(true);
    try {
      await api.post('/customers', {
        name,
        phone,
        email,
        address,
        gstin
      });
      Alert.alert('Success', 'Customer added successfully!', [
        { text: 'OK', onPress: () => navigation.goBack() }
      ]);
    } catch (error) {
      console.error('Error adding customer:', error);
      Alert.alert('Error', 'Failed to add customer. Check if phone number already exists.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <ScrollView style={styles.container}>
      <Text style={styles.label}>Customer Name *</Text>
      <TextInput style={styles.input} value={name} onChangeText={setName} placeholder="Enter name" />

      <Text style={styles.label}>Phone Number *</Text>
      <TextInput style={styles.input} value={phone} onChangeText={setPhone} placeholder="Enter 10-digit phone number" keyboardType="phone-pad" maxLength={10} />

      <Text style={styles.label}>Email Address</Text>
      <TextInput style={styles.input} value={email} onChangeText={setEmail} placeholder="Optional" keyboardType="email-address" />

      <Text style={styles.label}>Address</Text>
      <TextInput style={styles.input} value={address} onChangeText={setAddress} placeholder="Optional" />

      <Text style={styles.label}>GSTIN</Text>
      <TextInput style={styles.input} value={gstin} onChangeText={setGstin} placeholder="Optional" autoCapitalize="characters" />

      <TouchableOpacity style={styles.saveButton} onPress={handleSave} disabled={loading}>
        {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.saveButtonText}>Save Customer</Text>}
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
  saveButton: {
    backgroundColor: '#4299e1', padding: 16, borderRadius: 8, alignItems: 'center', marginTop: 8, marginBottom: 40
  },
  saveButtonText: { color: '#fff', fontSize: 16, fontWeight: 'bold' }
});
