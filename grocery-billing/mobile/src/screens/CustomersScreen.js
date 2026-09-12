import React, { useState, useEffect } from 'react';
import { View, Text, FlatList, StyleSheet, ActivityIndicator, Alert, TouchableOpacity } from 'react-native';
import api from '../api/axiosConfig';

export default function CustomersScreen() {
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchCustomers();
  }, []);

  const fetchCustomers = async () => {
    try {
      const response = await api.get('/customers');
      setCustomers(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error('Error fetching customers:', error);
      Alert.alert('Error', 'Could not load customers.');
    } finally {
      setLoading(false);
    }
  };

  const renderCustomer = ({ item }) => (
    <View style={styles.card}>
      <Text style={styles.name}>{item.name}</Text>
      <View style={styles.row}>
        <Text style={styles.phone}>{item.phone || 'No phone'}</Text>
        <Text style={[styles.balance, { color: item.balance > 0 ? '#e53e3e' : '#38a169' }]}>
          Dues: ₹{item.balance || 0}
        </Text>
      </View>
    </View>
  );

  if (loading) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" color="#4299e1" />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Customers</Text>
      </View>
      <FlatList
        data={customers}
        keyExtractor={(item, index) => (item.id ? item.id.toString() : index.toString())}
        renderItem={renderCustomer}
        contentContainerStyle={styles.list}
        ListEmptyComponent={<Text style={styles.emptyText}>No customers found.</Text>}
      />
      <TouchableOpacity 
        style={styles.fab} 
        onPress={() => navigation.navigate('AddCustomer')}
      >
        <Text style={styles.fabIcon}>+</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f7fa' },
  centerContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  header: {
    padding: 16, backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#e2e8f0', marginTop: 40
  },
  headerTitle: { fontSize: 20, fontWeight: 'bold', color: '#2d3748' },
  list: { padding: 16 },
  card: {
    backgroundColor: '#fff', padding: 16, borderRadius: 8, marginBottom: 12,
    shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.1, shadowRadius: 2, elevation: 2,
  },
  name: { fontSize: 16, fontWeight: 'bold', color: '#2d3748', marginBottom: 8 },
  row: { flexDirection: 'row', justifyContent: 'space-between' },
  phone: { fontSize: 14, color: '#718096' },
  balance: { fontSize: 14, fontWeight: '600' },
  emptyText: { textAlign: 'center', color: '#a0aec0', marginTop: 24 },
  fab: {
    position: 'absolute',
    width: 60,
    height: 60,
    alignItems: 'center',
    justifyContent: 'center',
    right: 20,
    bottom: 20,
    backgroundColor: '#4299e1',
    borderRadius: 30,
    elevation: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 3,
  },
  fabIcon: {
    fontSize: 30,
    color: '#fff',
    lineHeight: 34
  }
});
