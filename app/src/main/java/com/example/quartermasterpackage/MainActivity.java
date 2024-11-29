package com.example.quartermasterpackage;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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

        addItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });
    }

    // Method to handle edit action
    public void editItem(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Quantity");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(inventoryList.get(position).getQuantity()));
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int newQuantity = Integer.parseInt(input.getText().toString());
                inventoryList.get(position).setQuantity(newQuantity);
                inventoryAdapter.notifyDataSetChanged();
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

    // Method to handle remove action
    public void removeItem(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remove Item");
        builder.setMessage("Are you sure you want to remove this item?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                inventoryList.remove(position);
                inventoryAdapter.notifyDataSetChanged();
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
}