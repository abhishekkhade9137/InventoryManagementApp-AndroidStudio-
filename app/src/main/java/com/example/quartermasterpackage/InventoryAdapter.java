package com.example.quartermasterpackage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

public class InventoryAdapter extends ArrayAdapter<InventoryItem> {

    public InventoryAdapter(Context context, List<InventoryItem> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.inventory_list_item, parent, false);
        }

        InventoryItem currentItem = getItem(position);

        TextView itemNameTextView = listItemView.findViewById(R.id.item_name_text_view);
        itemNameTextView.setText(currentItem.getName());

        TextView itemQuantityTextView = listItemView.findViewById(R.id.item_quantity_text_view);
        itemQuantityTextView.setText(String.valueOf(currentItem.getQuantity()));

        Button editButton = listItemView.findViewById(R.id.edit_button);
        Button removeButton = listItemView.findViewById(R.id.remove_button);

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call editItem method in MainActivity
                ((MainActivity) getContext()).editItem(position);
            }
        });

        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call removeItem method in MainActivity
                ((MainActivity) getContext()).removeItem(position);
            }
        });

        return listItemView;
    }
}