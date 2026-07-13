package com.example.inventoryapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS_SETTINGS = "settings_prefs";
    public static final String KEY_DARK_MODE = "dark_mode";
    public static final String KEY_CURRENCY = "currency";
    public static final String KEY_DEF_MIN_STOCK = "def_min_stock";

    private MaterialSwitch switchDarkMode;
    private TextInputEditText etCurrency, etLowStock;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);

        MaterialToolbar toolbar = findViewById(R.id.settings_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        switchDarkMode = findViewById(R.id.switch_dark_mode);
        etCurrency = findViewById(R.id.et_currency);
        etLowStock = findViewById(R.id.et_low_stock);
        MaterialButton btnClearData = findViewById(R.id.btn_clear_data);
        
        MaterialButton btnExport = findViewById(R.id.btn_export_json);
        MaterialButton btnImport = findViewById(R.id.btn_import_json);
        
        btnExport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, "inventory_backup.json");
            try {
                startActivityForResult(intent, 201);
            } catch (Exception e) {
                Toast.makeText(this, "No file manager found", Toast.LENGTH_SHORT).show();
            }
        });

        btnImport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            try {
                startActivityForResult(intent, 202);
            } catch (Exception e) {
                Toast.makeText(this, "No file manager found", Toast.LENGTH_SHORT).show();
            }
        });

        loadSettings();

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        btnClearData.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Erase All Data")
                    .setMessage("Are you sure? This will wipe the entire inventory database and activity log.")
                    .setPositiveButton("Erase", (d, w) -> {
                        getSharedPreferences("inv_prefs_v3", Context.MODE_PRIVATE).edit().clear().apply();
                        Toast.makeText(this, "All data erased. Please restart the app.", Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void loadSettings() {
        boolean isDark = prefs.getBoolean(KEY_DARK_MODE, false);
        switchDarkMode.setChecked(isDark);

        String currency = prefs.getString(KEY_CURRENCY, "$");
        etCurrency.setText(currency);

        int minStock = prefs.getInt(KEY_DEF_MIN_STOCK, 5);
        etLowStock.setText(String.valueOf(minStock));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            android.net.Uri uri = data.getData();
            SharedPreferences invPrefs = getSharedPreferences("inv_prefs_v3", Context.MODE_PRIVATE);
            
            if (requestCode == 201) { // Export
                try {
                    org.json.JSONObject root = new org.json.JSONObject();
                    java.util.Map<String, ?> allEntries = invPrefs.getAll();
                    for (java.util.Map.Entry<String, ?> entry : allEntries.entrySet()) {
                        if (entry.getValue() instanceof java.util.Set) {
                            org.json.JSONArray arr = new org.json.JSONArray();
                            for (String s : (java.util.Set<String>) entry.getValue()) arr.put(s);
                            root.put(entry.getKey(), arr);
                        } else {
                            root.put(entry.getKey(), entry.getValue());
                        }
                    }
                    
                    try (java.io.OutputStream os = getContentResolver().openOutputStream(uri)) {
                        if (os != null) {
                            os.write(root.toString(4).getBytes());
                            Toast.makeText(this, "Backup exported successfully", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else if (requestCode == 202) { // Import
                try {
                    java.io.InputStream is = getContentResolver().openInputStream(uri);
                    if (is != null) {
                        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        is.close();
                        
                        org.json.JSONObject root = new org.json.JSONObject(sb.toString());
                        SharedPreferences.Editor editor = invPrefs.edit();
                        editor.clear();
                        
                        java.util.Iterator<String> keys = root.keys();
                        while(keys.hasNext()) {
                            String key = keys.next();
                            Object val = root.get(key);
                            if (val instanceof org.json.JSONArray) {
                                org.json.JSONArray arr = (org.json.JSONArray) val;
                                java.util.Set<String> set = new java.util.HashSet<>();
                                for (int i = 0; i < arr.length(); i++) set.add(arr.getString(i));
                                editor.putStringSet(key, set);
                            } else if (val instanceof String) {
                                editor.putString(key, (String) val);
                            } else if (val instanceof Integer) {
                                editor.putInt(key, (Integer) val);
                            } else if (val instanceof Boolean) {
                                editor.putBoolean(key, (Boolean) val);
                            } else if (val instanceof Float) {
                                editor.putFloat(key, (Float) val);
                            } else if (val instanceof Long) {
                                editor.putLong(key, (Long) val);
                            }
                        }
                        editor.apply();
                        Toast.makeText(this, "Backup restored successfully. Please restart the app.", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Import failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Save text fields on pause
        String cur = etCurrency.getText() != null ? etCurrency.getText().toString().trim() : "$";
        int minStock = 5;
        try {
            minStock = Integer.parseInt(etLowStock.getText() != null ? etLowStock.getText().toString().trim() : "5");
        } catch (NumberFormatException ignored) {}

        prefs.edit()
             .putString(KEY_CURRENCY, cur)
             .putInt(KEY_DEF_MIN_STOCK, minStock)
             .apply();
    }
}
