package com.example.inventoryapp;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements InventoryAdapter.OnItemActionListener {

    // --- Views ---
    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private TextInputEditText searchBar;
    private AutoCompleteTextView categoryFilter;
    private BottomNavigationView bottomNav;

    // Panels (sections)
    private View inventoryPanel, dashboardPanel, reportsPanel;

    // Dashboard views
    private TextView tvTotalItems, tvTotalQty, tvLowStock, tvExpiringSoon, tvTotalValue;

    // Reports views
    private MaterialButton btnExportCsv, btnGenerateReport;
    private TextView tvReportOutput;

    // --- Data ---
    private List<InventoryItem> inventoryList = new ArrayList<>();
    private List<String> activityLog = new LinkedList<>();
    private static final int MAX_LOG_ENTRIES = 200;
    private static final int CREATE_FILE_REQUEST_CODE = 101;
    private static final String PREFS_NAME = "inv_prefs_v3";
    private static final String KEY_ITEMS = "items_v3";
    private static final String KEY_LOG = "activity_log";

    private String currentCategory = "All";
    private String currentSearch = "";
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        loadData();
        setupRecyclerView();
        setupSearch();
        setupCategoryFilter();
        setupBottomNav();
        setupReportButtons();

        showPanel(inventoryPanel);
        refreshDashboard();
        checkAlerts();
    }

    private void bindViews() {
        recyclerView = findViewById(R.id.inventory_recycler_view);
        searchBar = findViewById(R.id.search_bar);
        categoryFilter = findViewById(R.id.category_filter_spinner);
        bottomNav = findViewById(R.id.bottom_navigation);
        inventoryPanel = findViewById(R.id.panel_inventory);
        dashboardPanel = findViewById(R.id.panel_dashboard);
        reportsPanel = findViewById(R.id.panel_reports);
        tvTotalItems = findViewById(R.id.tv_total_items);
        tvTotalQty = findViewById(R.id.tv_total_qty);
        tvLowStock = findViewById(R.id.tv_low_stock);
        tvExpiringSoon = findViewById(R.id.tv_expiring_soon);
        tvTotalValue = findViewById(R.id.tv_total_value);

        btnExportCsv = findViewById(R.id.btn_export_csv);
        btnGenerateReport = findViewById(R.id.btn_generate_report);
        tvReportOutput = findViewById(R.id.tv_report_output);
        MaterialButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> showAddEditDialog(-1));

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.main_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            } else if (item.getItemId() == R.id.action_sort) {
                showSortDialog();
                return true;
            }
            return false;
        });
    }

    private int currentSortOption = 0; // 0=Freq, 1=A-Z, 2=Qty Low-High, 3=Qty High-Low, 4=Expiry

    private void showSortDialog() {
        String[] options = {"Smart Sort (Most Used)", "Alphabetical (A-Z)", "Quantity (Low to High)", "Quantity (High to Low)", "Expiration Date"};
        new AlertDialog.Builder(this)
            .setTitle("Sort By")
            .setSingleChoiceItems(options, currentSortOption, (dialog, which) -> {
                currentSortOption = which;
                sortInventory();
                adapter.filter(currentSearch, currentCategory);
                dialog.dismiss();
            })
            .show();
    }

    private void setupRecyclerView() {
        adapter = new InventoryAdapter(this, inventoryList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString();
                adapter.filter(currentSearch, currentCategory);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCategoryFilter() {
        updateCategoryFilter();
    }

    private void updateCategoryFilter() {
        Set<String> catSet = new HashSet<>();
        catSet.add("All");
        for (InventoryItem item : inventoryList) {
            if (!item.getCategory().isEmpty()) catSet.add(item.getCategory());
        }
        List<String> cats = new ArrayList<>(catSet);
        Collections.sort(cats);
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, cats);
        categoryFilter.setAdapter(catAdapter);
        categoryFilter.setOnItemClickListener((parent, view, pos, id) -> {
            currentCategory = cats.get(pos);
            adapter.filter(currentSearch, currentCategory);
        });
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_store) {
                showPanel(inventoryPanel);
            } else if (id == R.id.navigation_dashboard) {
                refreshDashboard();
                showPanel(dashboardPanel);
            } else if (id == R.id.navigation_issue) {
                showPanel(reportsPanel);
            }
            return true;
        });
    }

    private void setupReportButtons() {
        btnExportCsv.setOnClickListener(v -> exportToCsv());
        btnGenerateReport.setOnClickListener(v -> generateTextReport());
        findViewById(R.id.btn_view_log).setOnClickListener(v -> showActivityLog());
    }
    
    private void showActivityLog() {
        StringBuilder sb = new StringBuilder();
        if (activityLog.isEmpty()) {
            sb.append("No recent activity.");
        } else {
            for (String entry : activityLog) {
                sb.append("• ").append(entry).append("\n\n");
            }
        }
        
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        TextView tv = new TextView(this);
        tv.setText(sb.toString());
        tv.setPadding(32, 32, 32, 32);
        sv.addView(tv);
        
        new AlertDialog.Builder(this)
                .setTitle("Activity Log")
                .setView(sv)
                .setPositiveButton("Close", null)
                .show();
    }
    
    private void shareReport() {
        if (tvReportOutput.getText().toString().isEmpty() || tvReportOutput.getText().toString().startsWith("Tap")) {
            generateTextReport();
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Inventory Report");
        shareIntent.putExtra(Intent.EXTRA_TEXT, tvReportOutput.getText().toString());
        try {
            startActivity(Intent.createChooser(shareIntent, "Share Report via"));
        } catch (Exception e) {
            Toast.makeText(this, "No app can handle this action", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPanel(View panel) {
        inventoryPanel.setVisibility(View.GONE);
        dashboardPanel.setVisibility(View.GONE);
        reportsPanel.setVisibility(View.GONE);
        panel.setVisibility(View.VISIBLE);
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Override
    public void onEdit(int position) {
        int masterIndex = adapter.getMasterIndex(position);
        showAddEditDialog(masterIndex);
    }

    @Override
    public void onDelete(int position) {
        int masterIndex = adapter.getMasterIndex(position);
        InventoryItem item = inventoryList.get(masterIndex);
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Delete \"" + item.getName() + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    logActivity("Deleted: " + item.getName() + " (Qty: " + item.getQuantity() + ")");
                    inventoryList.remove(masterIndex);
                    adapter.filter(currentSearch, currentCategory);
                    saveData();
                    refreshDashboard();
                    updateCategoryFilter();
                    Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onViewDetail(int position) {
        int masterIndex = adapter.getMasterIndex(position);
        showItemDetailDialog(inventoryList.get(masterIndex));
    }

    @Override
    public void onQuantityChanged(int position, int delta) {
        int masterIndex = adapter.getMasterIndex(position);
        InventoryItem item = inventoryList.get(masterIndex);
        int newQty = item.getQuantity() + delta;
        if (newQty < 0) newQty = 0;
        
        int oldQty = item.getQuantity();
        if (newQty < oldQty) {
            item.setRetrievalFrequency(item.getRetrievalFrequency() + 1);
        }
        item.setQuantity(newQty);
        item.setLastUpdated(System.currentTimeMillis());
        
        logActivity("Quick Update: " + item.getName() + " Qty " + oldQty + " → " + newQty);
        saveData();
        adapter.notifyItemChanged(position);
        refreshDashboard();
    }

    private void showItemDetailDialog(InventoryItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(item.getName()).append("\n");
        sb.append("Quantity: ").append(item.getQuantity()).append(" ").append(item.getUnit()).append("\n");
        if (!item.getCategory().isEmpty()) sb.append("Category: ").append(item.getCategory()).append("\n");
        if (!item.getSku().isEmpty()) sb.append("SKU: ").append(item.getSku()).append("\n");
        sb.append("Min Stock Level: ").append(item.getMinStockLevel()).append("\n");
        if (!item.getExpirationDate().isEmpty()) sb.append("Expiration: ").append(item.getExpirationDate()).append("\n");
        sb.append("Retrieval Frequency: ").append(item.getRetrievalFrequency()).append("\n");
        if (!item.getNotes().isEmpty()) sb.append("Notes: ").append(item.getNotes()).append("\n");
        if (item.getPrice() > 0) sb.append("Price: $").append(String.format(Locale.getDefault(), "%.2f", item.getPrice())).append("\n");
        if (!item.getLocation().isEmpty()) sb.append("Location: ").append(item.getLocation()).append("\n");
        if (!item.getSupplier().isEmpty()) sb.append("Supplier: ").append(item.getSupplier()).append("\n");
        sb.append("\nLast Updated: ").append(android.text.format.DateUtils.getRelativeTimeSpanString(item.getLastUpdated(), System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS));

        new AlertDialog.Builder(this)
                .setTitle(item.getName())
                .setMessage(sb.toString())
                .setPositiveButton("Edit", (d, w) -> showAddEditDialog(inventoryList.indexOf(item)))
                .setNeutralButton("Share", (d, w) -> {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Item Details: " + item.getName());
                    shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
                    try {
                        startActivity(Intent.createChooser(shareIntent, "Share Item via"));
                    } catch (Exception e) {
                        Toast.makeText(this, "No app can handle this action", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showAddEditDialog(final int masterIndex) {
        boolean isEdit = masterIndex >= 0;
        InventoryItem existing = isEdit ? inventoryList.get(masterIndex) : null;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_item, null);

        TextInputEditText etName = dialogView.findViewById(R.id.et_item_name);
        TextInputEditText etQty = dialogView.findViewById(R.id.et_item_qty);
        TextInputEditText etUnit = dialogView.findViewById(R.id.et_item_unit);
        TextInputEditText etCategory = dialogView.findViewById(R.id.et_item_category);
        TextInputEditText etSku = dialogView.findViewById(R.id.et_item_sku);
        TextInputEditText etMinStock = dialogView.findViewById(R.id.et_item_min_stock);
        TextInputEditText etExpiry = dialogView.findViewById(R.id.et_item_expiry);
        TextInputEditText etNotes = dialogView.findViewById(R.id.et_item_notes);
        TextInputEditText etPrice = dialogView.findViewById(R.id.et_item_price);
        TextInputEditText etLocation = dialogView.findViewById(R.id.et_item_location);
        TextInputEditText etSupplier = dialogView.findViewById(R.id.et_item_supplier);

        etExpiry.setFocusable(false);
        etExpiry.setClickable(true);
        etExpiry.setOnClickListener(v -> showDatePicker(etExpiry));

        if (isEdit) {
            etName.setText(existing.getName());
            etQty.setText(String.valueOf(existing.getQuantity()));
            etUnit.setText(existing.getUnit());
            etCategory.setText(existing.getCategory());
            etSku.setText(existing.getSku());
            etMinStock.setText(String.valueOf(existing.getMinStockLevel()));
            etExpiry.setText(existing.getExpirationDate());
            etNotes.setText(existing.getNotes());
            etPrice.setText(existing.getPrice() > 0 ? String.valueOf(existing.getPrice()) : "");
            etLocation.setText(existing.getLocation());
            etSupplier.setText(existing.getSupplier());
        } else {
            etUnit.setText("pcs");
            etMinStock.setText(String.valueOf(getSharedPreferences(SettingsActivity.PREFS_SETTINGS, Context.MODE_PRIVATE).getInt(SettingsActivity.KEY_DEF_MIN_STOCK, 5)));
        }

        new AlertDialog.Builder(this)
                .setTitle(isEdit ? "Edit Item" : "Add New Item")
                .setView(dialogView)
                .setPositiveButton(isEdit ? "Save" : "Add", (d, w) -> {
                    String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                    String qtyStr = etQty.getText() != null ? etQty.getText().toString().trim() : "";

                    if (name.isEmpty()) {
                        Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (qtyStr.isEmpty()) {
                        Toast.makeText(this, "Quantity is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int qty = 0;
                    int minStock = 0;
                    double price = 0.0;
                    try { qty = Integer.parseInt(qtyStr); } catch (NumberFormatException ignored) {}
                    try {
                        String ms = etMinStock.getText() != null ? etMinStock.getText().toString().trim() : "0";
                        if (!ms.isEmpty()) minStock = Integer.parseInt(ms);
                    } catch (NumberFormatException ignored) {}
                    try {
                        String pr = etPrice.getText() != null ? etPrice.getText().toString().trim() : "0";
                        if (!pr.isEmpty()) price = Double.parseDouble(pr);
                    } catch (NumberFormatException ignored) {}

                    String unit = etUnit.getText() != null ? etUnit.getText().toString().trim() : "pcs";
                    if (unit.isEmpty()) unit = "pcs";
                    String category = etCategory.getText() != null ? etCategory.getText().toString().trim() : "";
                    String sku = etSku.getText() != null ? etSku.getText().toString().trim() : "";
                    String expiry = etExpiry.getText() != null ? etExpiry.getText().toString().trim() : "";
                    String notes = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";
                    String location = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";
                    String supplier = etSupplier.getText() != null ? etSupplier.getText().toString().trim() : "";

                    if (isEdit) {
                        int oldQty = existing.getQuantity();
                        int freq = existing.getRetrievalFrequency();
                        if (qty < oldQty) freq++;
                        existing.setName(name);
                        existing.setQuantity(qty);
                        existing.setUnit(unit);
                        existing.setCategory(category);
                        existing.setSku(sku);
                        existing.setMinStockLevel(minStock);
                        existing.setExpirationDate(expiry);
                        existing.setRetrievalFrequency(freq);
                        existing.setNotes(notes);
                        existing.setPrice(price);
                        existing.setLocation(location);
                        existing.setSupplier(supplier);
                        existing.setLastUpdated(System.currentTimeMillis());
                        logActivity("Edited: " + name + " Qty " + oldQty + " → " + qty);
                    } else {
                        int newId = inventoryList.isEmpty() ? 1 : inventoryList.get(inventoryList.size() - 1).getId() + 1;
                        InventoryItem newItem = new InventoryItem(newId, name, qty, unit, category, sku, minStock, expiry, 0, notes, System.currentTimeMillis(), price, location, supplier);
                        inventoryList.add(newItem);
                        logActivity("Added: " + name + " Qty " + qty);
                    }

                    sortInventory();
                    adapter.filter(currentSearch, currentCategory);
                    saveData();
                    refreshDashboard();
                    updateCategoryFilter();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDatePicker(final TextInputEditText target) {
        final Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
            target.setText(date);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    // ── SORTING ───────────────────────────────────────────────────────────────

    private void sortInventory() {
        Collections.sort(inventoryList, (a, b) -> {
            switch (currentSortOption) {
                case 1: // A-Z
                    return a.getName().compareToIgnoreCase(b.getName());
                case 2: // Qty Low-High
                    return Integer.compare(a.getQuantity(), b.getQuantity());
                case 3: // Qty High-Low
                    return Integer.compare(b.getQuantity(), a.getQuantity());
                case 4: // Expiration Date (nearest first)
                    if (a.getExpirationDate().isEmpty() && b.getExpirationDate().isEmpty()) return 0;
                    if (a.getExpirationDate().isEmpty()) return 1;
                    if (b.getExpirationDate().isEmpty()) return -1;
                    return a.getExpirationDate().compareTo(b.getExpirationDate());
                case 0: // Smart (Frequency)
                default:
                    int freqCmp = Integer.compare(b.getRetrievalFrequency(), a.getRetrievalFrequency());
                    if (freqCmp != 0) return freqCmp;
                    return a.getName().compareToIgnoreCase(b.getName());
            }
        });
    }

    // ── DASHBOARD ─────────────────────────────────────────────────────────────

    private void refreshDashboard() {
        int totalItems = inventoryList.size();
        int totalQty = 0;
        int lowStockCount = 0;
        int expiringSoonCount = 0;
        double totalValue = 0.0;
        Calendar soon = Calendar.getInstance();
        soon.add(Calendar.DAY_OF_YEAR, 7);

        for (InventoryItem item : inventoryList) {
            totalQty += item.getQuantity();
            totalValue += (item.getQuantity() * item.getPrice());
            if (item.isLowStock()) lowStockCount++;
            if (!item.getExpirationDate().isEmpty()) {
                try {
                    Date expDate = sdf.parse(item.getExpirationDate());
                    if (expDate != null && expDate.before(soon.getTime())) expiringSoonCount++;
                } catch (ParseException ignored) {}
            }
        }

        tvTotalItems.setText(String.valueOf(totalItems));
        tvTotalQty.setText(String.valueOf(totalQty));
        tvLowStock.setText(String.valueOf(lowStockCount));
        tvExpiringSoon.setText(String.valueOf(expiringSoonCount));
        
        String currency = getSharedPreferences(SettingsActivity.PREFS_SETTINGS, Context.MODE_PRIVATE)
            .getString(SettingsActivity.KEY_CURRENCY, "$");
        tvTotalValue.setText(currency + String.format(Locale.getDefault(), "%.2f", totalValue));
    }

    // ── ALERTS ────────────────────────────────────────────────────────────────

    private void checkAlerts() {
        Calendar soon = Calendar.getInstance();
        soon.add(Calendar.DAY_OF_YEAR, 7);

        List<String> lowStock = new ArrayList<>();
        List<String> expiring = new ArrayList<>();

        for (InventoryItem item : inventoryList) {
            if (item.isLowStock()) {
                lowStock.add("• " + item.getName() + " (" + item.getQuantity() + " " + item.getUnit() + ", min: " + item.getMinStockLevel() + ")");
            }
            if (!item.getExpirationDate().isEmpty()) {
                try {
                    Date expDate = sdf.parse(item.getExpirationDate());
                    if (expDate != null) {
                        long days = TimeUnit.MILLISECONDS.toDays(expDate.getTime() - System.currentTimeMillis());
                        if (days < 0) {
                            expiring.add("• " + item.getName() + " (EXPIRED on " + item.getExpirationDate() + ")");
                        } else if (days <= 7) {
                            expiring.add("• " + item.getName() + " (expires in " + days + " days)");
                        }
                    }
                } catch (ParseException ignored) {}
            }
        }

        StringBuilder alertMsg = new StringBuilder();
        if (!lowStock.isEmpty()) {
            alertMsg.append("⚠ LOW STOCK ITEMS:\n");
            for (String s : lowStock) alertMsg.append(s).append("\n");
        }
        if (!expiring.isEmpty()) {
            if (alertMsg.length() > 0) alertMsg.append("\n");
            alertMsg.append("🕐 EXPIRING SOON / EXPIRED:\n");
            for (String s : expiring) alertMsg.append(s).append("\n");
        }

        if (alertMsg.length() > 0) {
            new AlertDialog.Builder(this)
                    .setTitle("Inventory Alerts")
                    .setMessage(alertMsg.toString().trim())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    // ── REPORTS & EXPORT ──────────────────────────────────────────────────────

    private void generateTextReport() {
        if (inventoryList.isEmpty()) {
            tvReportOutput.setText("No items in inventory.");
            return;
        }

        int totalQty = 0, lowCount = 0, expiredCount = 0, expiringSoonCount = 0;
        Calendar soon = Calendar.getInstance();
        soon.add(Calendar.DAY_OF_YEAR, 7);
        StringBuilder sb = new StringBuilder();

        sb.append("═══ INVENTORY REPORT ═══\n");
        sb.append("Generated: ").append(new java.util.Date()).append("\n\n");

        for (InventoryItem item : inventoryList) {
            totalQty += item.getQuantity();
            if (item.isLowStock()) lowCount++;
            String status = "";
            if (!item.getExpirationDate().isEmpty()) {
                try {
                    Date expDate = sdf.parse(item.getExpirationDate());
                    if (expDate != null) {
                        long days = TimeUnit.MILLISECONDS.toDays(expDate.getTime() - System.currentTimeMillis());
                        if (days < 0) { status = " [EXPIRED]"; expiredCount++; }
                        else if (days <= 7) { status = " [EXPIRES IN " + days + "d]"; expiringSoonCount++; }
                    }
                } catch (ParseException ignored) {}
            }
            sb.append("• ").append(item.getName());
            if (!item.getCategory().isEmpty()) sb.append(" [").append(item.getCategory()).append("]");
            sb.append(": ").append(item.getQuantity()).append(" ").append(item.getUnit()).append(status);
            if (item.isLowStock()) sb.append(" ⚠LOW");
            sb.append("\n");
        }

        sb.append("\n─── SUMMARY ───\n");
        sb.append("Total items: ").append(inventoryList.size()).append("\n");
        sb.append("Total units in stock: ").append(totalQty).append("\n");
        sb.append("Low stock items: ").append(lowCount).append("\n");
        sb.append("Expired items: ").append(expiredCount).append("\n");
        sb.append("Expiring within 7 days: ").append(expiringSoonCount).append("\n");

        if (lowCount > 0) {
            sb.append("\n─── RESTOCK SUGGESTIONS ───\n");
            for (InventoryItem item : inventoryList) {
                if (item.isLowStock()) {
                    int needed = item.getMinStockLevel() - item.getQuantity();
                    if (needed <= 0) needed = 1; // At least restock 1 if it just hit the boundary
                    sb.append("• ").append(item.getName()).append(" -> Order ").append(needed).append(" more ").append(item.getUnit());
                    if (!item.getSupplier().isEmpty()) sb.append(" from ").append(item.getSupplier());
                    sb.append("\n");
                }
            }
        }

        if (!activityLog.isEmpty()) {
            sb.append("\n─── RECENT ACTIVITY ───\n");
            int start = Math.max(0, activityLog.size() - 20);
            for (int i = start; i < activityLog.size(); i++) {
                sb.append(activityLog.get(i)).append("\n");
            }
        }

        tvReportOutput.setText(sb.toString());
    }

    private void exportToCsv() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "inventory_report.csv");
        try {
            startActivityForResult(intent, CREATE_FILE_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "No file manager found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) writeCsvToUri(uri);
        }
    }

    private void writeCsvToUri(Uri uri) {
        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            if (os == null) return;
            StringBuilder csv = new StringBuilder();
            csv.append("ID,Name,Quantity,Unit,Category,SKU,Min Stock,Expiration Date,Retrieval Frequency,Notes,Last Updated\n");
            for (InventoryItem item : inventoryList) {
                csv.append(item.getId()).append(",")
                   .append(csvEscape(item.getName())).append(",")
                   .append(item.getQuantity()).append(",")
                   .append(csvEscape(item.getUnit())).append(",")
                   .append(csvEscape(item.getCategory())).append(",")
                   .append(csvEscape(item.getSku())).append(",")
                   .append(item.getMinStockLevel()).append(",")
                   .append(csvEscape(item.getExpirationDate())).append(",")
                   .append(item.getRetrievalFrequency()).append(",")
                   .append(csvEscape(item.getNotes())).append(",")
                   .append(new java.util.Date(item.getLastUpdated())).append("\n");
            }
            os.write(csv.toString().getBytes());
            logActivity("Exported CSV report (" + inventoryList.size() + " items)");
            saveData();
            Toast.makeText(this, "✓ Report exported successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to export: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String csvEscape(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    // ── PERSISTENCE ───────────────────────────────────────────────────────────

    private void saveData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < inventoryList.size(); i++) {
            if (i > 0) sb.append(";;");
            sb.append(inventoryList.get(i).serialize());
        }
        editor.putString(KEY_ITEMS, sb.toString());

        // Save last 200 log entries
        StringBuilder logSb = new StringBuilder();
        int start = Math.max(0, activityLog.size() - MAX_LOG_ENTRIES);
        for (int i = start; i < activityLog.size(); i++) {
            if (i > start) logSb.append("~~");
            logSb.append(activityLog.get(i));
        }
        editor.putString(KEY_LOG, logSb.toString());
        editor.apply();
    }

    private void loadData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_ITEMS, "");
        inventoryList.clear();
        if (!raw.isEmpty()) {
            String[] parts = raw.split(";;");
            for (String part : parts) {
                InventoryItem item = InventoryItem.deserialize(part);
                if (item != null) inventoryList.add(item);
            }
        }

        // Load activity log
        String logRaw = prefs.getString(KEY_LOG, "");
        activityLog.clear();
        if (!logRaw.isEmpty()) {
            String[] logEntries = logRaw.split("~~");
            for (String e : logEntries) activityLog.add(e);
        }

        sortInventory();
    }

    private void logActivity(String message) {
        String prefix = "🔹";
        if (message.startsWith("Added:")) prefix = "➕";
        else if (message.startsWith("Edited:")) prefix = "✏️";
        else if (message.startsWith("Deleted:")) prefix = "🗑️";
        else if (message.startsWith("Quick Update:")) prefix = "⚡";
        
        String entry = "[" + new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date()) + "] " + prefix + " " + message;
        activityLog.add(entry);
        if (activityLog.size() > 100) activityLog.remove(0); // keep last 100
    }
}
