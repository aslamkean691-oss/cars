package com.example.cars.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cars.R;
import com.example.cars.model.Branch;

import java.util.ArrayList;
import java.util.List;

public class BranchAdapter extends RecyclerView.Adapter<BranchAdapter.Holder> {

    private final List<Branch> items = new ArrayList<>();

    public void setItems(List<Branch> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_branch, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Branch b = items.get(position);
        String title = b.getBranchName() != null && !b.getBranchName().isEmpty()
                ? b.getBranchName()
                : "סניף";
        h.title.setText(title);
        h.address.setText(b.getAddress() != null ? b.getAddress() : "");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView address;

        Holder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvBranchTitle);
            address = itemView.findViewById(R.id.tvBranchAddress);
        }
    }
}
