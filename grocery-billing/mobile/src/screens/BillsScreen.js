import React, { useState, useEffect } from 'react';
import { View, Text, FlatList, StyleSheet, ActivityIndicator, Alert, TouchableOpacity } from 'react-native';
import api from '../api/axiosConfig';

export default function BillsScreen() {
  const [bills, setBills] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchBills();
  }, []);

  const fetchBills = async () => {
    try {
      const response = await api.get('/bills');
      setBills(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error('Error fetching bills:', error);
      Alert.alert('Error', 'Could not load bills.');
    } finally {
      setLoading(false);
    }
  };

  const renderBill = ({ item }) => (
    <View style={styles.card}>
      <View style={styles.row}>
        <Text style={styles.billNo}>{item.billNo}</Text>
        <Text style={styles.date}>{item.billDate}</Text>
      </View>
      <Text style={styles.customerName}>{item.customerName || 'Walk-in Customer'}</Text>
      <View style={styles.row}>
        <Text style={[styles.status, { color: item.status === 'PAID' ? '#38a169' : '#e53e3e' }]}>
          {item.status}
        </Text>
        <Text style={styles.amount}>₹{item.netTotal}</Text>
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
        <Text style={styles.headerTitle}>Recent Bills</Text>
      </View>
      <FlatList
        data={bills}
        keyExtractor={(item, index) => (item.id ? item.id.toString() : index.toString())}
        renderItem={renderBill}
        contentContainerStyle={styles.list}
        ListEmptyComponent={<Text style={styles.emptyText}>No bills found.</Text>}
      />
      <TouchableOpacity 
        style={styles.fab} 
        onPress={() => navigation.navigate('CreateBill')}
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
  row: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 8 },
  billNo: { fontSize: 14, fontWeight: 'bold', color: '#4299e1' },
  date: { fontSize: 12, color: '#718096' },
  customerName: { fontSize: 16, fontWeight: '600', color: '#2d3748', marginBottom: 12 },
  status: { fontSize: 12, fontWeight: 'bold' },
  amount: { fontSize: 16, fontWeight: 'bold', color: '#2d3748' },
  emptyText: { textAlign: 'center', color: '#a0aec0', marginTop: 40 },
  fab: {
    position: 'absolute',
    width: 60,
    height: 60,
    alignItems: 'center',
    justifyContent: 'center',
    right: 20,
    bottom: 20,
    backgroundColor: '#3182ce',
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
