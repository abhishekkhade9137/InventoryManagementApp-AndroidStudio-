package com.example.quartermasterpackage;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText itemNameEditText;
    private EditText itemQuantityEditText;
    private Button addItemButton;
    private ListView inventoryListView;

    private List<InventoryItem> inventoryList;
    private InventoryAdapter inventoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        itemNameEditText = findViewById(R.id.item_name_edit_text);
        itemQuantityEditText = findViewById(R.id.item_quantity_edit_text);
        addItemButton = findViewById(R.id.add_item_button);
        inventoryListView = findViewById(R.id.inventory_list_view);

        inventoryList = new ArrayList<>();
        inventoryAdapter = new InventoryAdapter(this, inventoryList);
        inventoryListView.setAdapter(inventoryAdapter);

        loadInventoryData();

        addItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItem();
            }
        });

        inventoryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                editItem(position);
            }
        });

        inventoryListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                removeItem(position);
                return true;
            }
        });
    }

    private void addItem() {
        String itemName = itemNameEditText.getText().toString();
        String itemQuantityString = itemQuantityEditText.getText().toString();

        if (itemName.isEmpty() || itemQuantityString.isEmpty()) {
            Toast.makeText(MainActivity.this, "Please enter item name and quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        int itemQuantity = Integer.parseInt(itemQuantityString);

        InventoryItem newItem = new InventoryItem(itemName, itemQuantity);
        inventoryList.add(newItem);
        inventoryAdapter.notifyDataSetChanged();

        itemNameEditText.getText().clear();
        itemQuantityEditText.getText().clear();
        itemNameEditText.requestFocus();

        saveInventoryData();
    }

    void editItem(final int position) {
        final InventoryItem item = inventoryList.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Item");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(item.getQuantity()));
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    int newQuantity = Integer.parseInt(input.getText().toString());
                    item.setQuantity(newQuantity);
                    inventoryAdapter.notifyDataSetChanged();
                    saveInventoryData();
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "Invalid quantity", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    void removeItem(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remove Item");
        builder.setMessage("Are you sure you want to remove this item?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                inventoryList.remove(position);
                inventoryAdapter.notifyDataSetChanged();
                saveInventoryData();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void saveInventoryData() {
        SharedPreferences sharedPreferences = getSharedPreferences("inventory_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < inventoryList.size(); i++) {
            InventoryItem item = inventoryList.get(i);
            sb.append(item.getName()).append(",").append(item.getQuantity());
            if (i < inventoryList.size() - 1) {
                sb.append(";");
            }
        }
        String inventoryListString = sb.toString();

        editor.putString("inventory_list", inventoryListString);
        editor.apply();
    }

    private void loadInventoryData() {
        SharedPreferences sharedPreferences = getSharedPreferences("inventory_prefs", Context.MODE_PRIVATE);
        String inventoryListString = sharedPreferences.getString("inventory_list", null);

        if (inventoryListString != null) {
            inventoryList.clear();
            String[] items = inventoryListString.split(";");
            for (String item : items) {
                String[] parts = item.split(",");
                if (parts.length == 2) {
                    String name = parts[0];
                    int quantity = Integer.parseInt(parts[1]);
                    inventoryList.add(new InventoryItem(name, quantity));
                }
            }
            inventoryAdapter.notifyDataSetChanged();
        }
    }


}