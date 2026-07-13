package com.example.inventoryapp;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ProgressBar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

    public interface OnItemActionListener {
        void onEdit(int position);
        void onDelete(int position);
        void onViewDetail(int position);
        void onQuantityChanged(int position, int delta);
    }

    private List<InventoryItem> items;
    private List<InventoryItem> filteredItems;
    private Context context;
    private OnItemActionListener listener;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public InventoryAdapter(Context context, List<InventoryItem> items, OnItemActionListener listener) {
        this.context = context;
        this.items = items;
        this.filteredItems = new ArrayList<>(items);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.inventory_list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InventoryItem item = filteredItems.get(position);

        holder.nameText.setText(item.getName());
        holder.quantityText.setText(String.valueOf(item.getQuantity()) + " " + item.getUnit());

        // Category chip
        if (!item.getCategory().isEmpty()) {
            holder.categoryText.setText(item.getCategory());
            holder.categoryText.setVisibility(View.VISIBLE);
        } else {
            holder.categoryText.setVisibility(View.GONE);
        }

        // SKU
        if (!item.getSku().isEmpty()) {
            holder.skuText.setText("SKU: " + item.getSku());
            holder.skuText.setVisibility(View.VISIBLE);
        } else {
            holder.skuText.setVisibility(View.GONE);
        }

        // Expiration date + color coding
        if (!item.getExpirationDate().isEmpty()) {
            long daysUntilExpiry = getDaysUntilExpiry(item.getExpirationDate());
            if (daysUntilExpiry < 0) {
                holder.expirationText.setText("EXPIRED");
                holder.expirationText.setTextColor(Color.RED);
            } else if (daysUntilExpiry <= 7) {
                holder.expirationText.setText("Exp: " + item.getExpirationDate() + " (" + daysUntilExpiry + "d)");
                holder.expirationText.setTextColor(Color.parseColor("#FF6F00")); // amber
            } else {
                holder.expirationText.setText("Exp: " + item.getExpirationDate());
                holder.expirationText.setTextColor(Color.parseColor("#388E3C")); // green
            }
            holder.expirationText.setVisibility(View.VISIBLE);
        } else {
            holder.expirationText.setVisibility(View.GONE);
        }

        // Low stock indicator and progress bar
        int maxStock = Math.max(item.getMinStockLevel() * 2, 100);
        if (item.getMinStockLevel() == 0) maxStock = Math.max(item.getQuantity() * 2, 100);
        holder.progressBar.setMax(maxStock);
        holder.progressBar.setProgress(item.getQuantity());

        if (item.isLowStock()) {
            holder.lowStockBadge.setVisibility(View.VISIBLE);
            if (item.getQuantity() == 0) {
                holder.lowStockBadge.setBackgroundColor(Color.parseColor("#D32F2F")); // Red
                holder.cardView.setStrokeColor(Color.parseColor("#D32F2F"));
            } else {
                holder.lowStockBadge.setBackgroundColor(Color.parseColor("#FF6F00")); // Orange
                holder.cardView.setStrokeColor(Color.parseColor("#FF6F00"));
            }
        } else {
            holder.lowStockBadge.setVisibility(View.GONE);
            holder.cardView.setStrokeColor(Color.parseColor("#E0E0E0"));
        }

        // Quantity color
        if (item.getQuantity() == 0) {
            holder.quantityText.setTextColor(Color.RED);
        } else if (item.isLowStock()) {
            holder.quantityText.setTextColor(Color.parseColor("#FF6F00"));
        } else {
            holder.quantityText.setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark));
        }

        // Buttons
        holder.editButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) listener.onEdit(pos);
        });
        holder.deleteButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) listener.onDelete(pos);
        });
        holder.btnPlus.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) listener.onQuantityChanged(pos, 1);
        });
        holder.btnMinus.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) listener.onQuantityChanged(pos, -1);
        });
        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) listener.onViewDetail(pos);
        });
    }

    @Override
    public int getItemCount() {
        return filteredItems.size();
    }

    public InventoryItem getItem(int position) {
        return filteredItems.get(position);
    }

    /** Returns the actual index in the master list */
    public int getMasterIndex(int filteredPosition) {
        InventoryItem item = filteredItems.get(filteredPosition);
        return items.indexOf(item);
    }

    public void filter(String query, String category) {
        filteredItems.clear();
        for (InventoryItem item : items) {
            boolean matchesCategory = category.isEmpty() || category.equals("All") || item.getCategory().equalsIgnoreCase(category);
            
            boolean matchesQuery = true;
            if (query != null && !query.trim().isEmpty()) {
                String lowerQuery = query.toLowerCase().trim();
                matchesQuery = item.getName().toLowerCase().contains(lowerQuery)
                        || item.getSku().toLowerCase().contains(lowerQuery)
                        || item.getLocation().toLowerCase().contains(lowerQuery)
                        || item.getSupplier().toLowerCase().contains(lowerQuery);
            }

            if (matchesQuery && matchesCategory) {
                filteredItems.add(item);
            }
        }
        notifyDataSetChanged();
    }

    public void refreshAll() {
        filteredItems.clear();
        filteredItems.addAll(items);
        notifyDataSetChanged();
    }

    private long getDaysUntilExpiry(String dateStr) {
        try {
            Date expDate = sdf.parse(dateStr);
            if (expDate == null) return Long.MAX_VALUE;
            long diff = expDate.getTime() - System.currentTimeMillis();
            return TimeUnit.MILLISECONDS.toDays(diff);
        } catch (ParseException e) {
            return Long.MAX_VALUE;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView nameText, quantityText, categoryText, skuText, expirationText, lowStockBadge;
        MaterialButton editButton, deleteButton;
        android.widget.ImageButton btnPlus, btnMinus;
        ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            nameText = itemView.findViewById(R.id.item_name_text_view);
            quantityText = itemView.findViewById(R.id.item_quantity_text_view);
            categoryText = itemView.findViewById(R.id.item_category_text_view);
            skuText = itemView.findViewById(R.id.item_sku_text_view);
            expirationText = itemView.findViewById(R.id.item_expiration_text_view);
            lowStockBadge = itemView.findViewById(R.id.low_stock_badge);
            editButton = itemView.findViewById(R.id.edit_button);
            deleteButton = itemView.findViewById(R.id.remove_button);
            btnPlus = itemView.findViewById(R.id.btn_plus);
            btnMinus = itemView.findViewById(R.id.btn_minus);
            progressBar = itemView.findViewById(R.id.stock_progress_bar);
        }
    }
}
