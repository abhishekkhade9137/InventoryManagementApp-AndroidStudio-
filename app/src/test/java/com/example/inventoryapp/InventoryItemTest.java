package com.example.inventoryapp;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for InventoryItem data model.
 */
public class InventoryItemTest {

    @Test
    public void testFullConstructor() {
        InventoryItem item = new InventoryItem(1, "Apples", 50, "kg", "Produce", "APL-001", 10, "2026-12-31", 3, "Organic", 0L, 2.50, "Aisle 1", "FarmCo");
        assertEquals(1, item.getId());
        assertEquals("Apples", item.getName());
        assertEquals(50, item.getQuantity());
        assertEquals("kg", item.getUnit());
        assertEquals("Produce", item.getCategory());
        assertEquals("APL-001", item.getSku());
        assertEquals(10, item.getMinStockLevel());
        assertEquals("2026-12-31", item.getExpirationDate());
        assertEquals(3, item.getRetrievalFrequency());
        assertEquals("Organic", item.getNotes());
        assertEquals(2.50, item.getPrice(), 0.001);
        assertEquals("Aisle 1", item.getLocation());
        assertEquals("FarmCo", item.getSupplier());
    }

    @Test
    public void testSerializeDeserialize_roundTrip() {
        InventoryItem original = new InventoryItem(42, "Banana|Split", 10, "pcs", "Fruit;Category", "BAN|42", 2, "2027-01-15", 5, "Notes here", 1234567890L, 1.99, "Back Room", "Supplier Inc");
        String serialized = original.serialize();
        InventoryItem restored = InventoryItem.deserialize(serialized);

        assertNotNull(restored);
        assertEquals(42, restored.getId());
        assertEquals("Banana|Split", restored.getName());
        assertEquals(10, restored.getQuantity());
        assertEquals(1.99, restored.getPrice(), 0.001);
        assertEquals("Back Room", restored.getLocation());
        assertEquals("Supplier Inc", restored.getSupplier());
    }
}
