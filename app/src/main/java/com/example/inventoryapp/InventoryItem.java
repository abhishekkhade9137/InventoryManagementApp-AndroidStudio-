package com.example.inventoryapp;

/**
 * Represents a single inventory item with all its attributes.
 */
public class InventoryItem {
    private int id;
    private String name;
    private int quantity;
    private String unit;           // e.g., "pcs", "kg", "ltr"
    private String category;
    private String sku;
    private int minStockLevel;     // Low-stock threshold
    private String expirationDate; // Format: yyyy-MM-dd
    private int retrievalFrequency; // Number of times quantity was reduced
    private String notes;
    private long lastUpdated;      // Timestamp in millis

    // New Fields
    private double price;
    private String location;
    private String supplier;

    public InventoryItem(int id, String name, int quantity, String unit,
                         String category, String sku, int minStockLevel,
                         String expirationDate, int retrievalFrequency,
                         String notes, long lastUpdated, double price, String location, String supplier) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.category = category;
        this.sku = sku;
        this.minStockLevel = minStockLevel;
        this.expirationDate = expirationDate;
        this.retrievalFrequency = retrievalFrequency;
        this.notes = notes;
        this.lastUpdated = lastUpdated;
        this.price = price;
        this.location = location;
        this.supplier = supplier;
    }

    // Convenience constructor for legacy / quick creation
    public InventoryItem(int id, String name, int quantity, String unit,
                         String category, String sku, int minStockLevel,
                         String expirationDate, int retrievalFrequency,
                         String notes, long lastUpdated) {
        this(id, name, quantity, unit, category, sku, minStockLevel, expirationDate, retrievalFrequency, notes, lastUpdated, 0.0, "", "");
    }

    public InventoryItem(String name, int quantity, String expirationDate, int retrievalFrequency) {
        this(0, name, quantity, "pcs", "", "", 0, expirationDate, retrievalFrequency, "", System.currentTimeMillis(), 0.0, "", "");
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getUnit() { return unit != null ? unit : "pcs"; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getCategory() { return category != null ? category : ""; }
    public void setCategory(String category) { this.category = category; }

    public String getSku() { return sku != null ? sku : ""; }
    public void setSku(String sku) { this.sku = sku; }

    public int getMinStockLevel() { return minStockLevel; }
    public void setMinStockLevel(int minStockLevel) { this.minStockLevel = minStockLevel; }

    public String getExpirationDate() { return expirationDate != null ? expirationDate : ""; }
    public void setExpirationDate(String expirationDate) { this.expirationDate = expirationDate; }

    public int getRetrievalFrequency() { return retrievalFrequency; }
    public void setRetrievalFrequency(int retrievalFrequency) { this.retrievalFrequency = retrievalFrequency; }

    public String getNotes() { return notes != null ? notes : ""; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getLocation() { return location != null ? location : ""; }
    public void setLocation(String location) { this.location = location; }

    public String getSupplier() { return supplier != null ? supplier : ""; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    public boolean isLowStock() {
        return minStockLevel > 0 && quantity <= minStockLevel;
    }

    /**
     * Serialize to a pipe-delimited string for SharedPreferences storage.
     */
    public String serialize() {
        return id + "|" +
               escape(name) + "|" +
               quantity + "|" +
               escape(unit) + "|" +
               escape(category) + "|" +
               escape(sku) + "|" +
               minStockLevel + "|" +
               escape(expirationDate) + "|" +
               retrievalFrequency + "|" +
               escape(notes) + "|" +
               lastUpdated + "|" +
               price + "|" +
               escape(location) + "|" +
               escape(supplier);
    }

    /** Deserialize from a pipe-delimited string. Returns null on failure. */
    public static InventoryItem deserialize(String s) {
        try {
            String[] p = s.split("\\|", -1);
            if (p.length < 11) return null;
            int id = Integer.parseInt(p[0]);
            String name = unescape(p[1]);
            int qty = Integer.parseInt(p[2]);
            String unit = unescape(p[3]);
            String category = unescape(p[4]);
            String sku = unescape(p[5]);
            int minStock = Integer.parseInt(p[6]);
            String expiry = unescape(p[7]);
            int freq = Integer.parseInt(p[8]);
            String notes = unescape(p[9]);
            long lastUpdated = Long.parseLong(p[10]);
            
            double price = 0.0;
            String location = "";
            String supplier = "";
            
            if (p.length >= 14) {
                price = Double.parseDouble(p[11]);
                location = unescape(p[12]);
                supplier = unescape(p[13]);
            }
            
            return new InventoryItem(id, name, qty, unit, category, sku, minStock, expiry, freq, notes, lastUpdated, price, location, supplier);
        } catch (Exception e) {
            return null;
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("|", "\\p").replace(";", "\\s");
    }

    private static String unescape(String s) {
        if (s == null) return "";
        return s.replace("\\s", ";").replace("\\p", "|").replace("\\\\", "\\");
    }
}
