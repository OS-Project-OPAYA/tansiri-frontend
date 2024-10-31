package com.capstone.tansiri;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.capstone.tansiri.map.entity.Favorite;

import java.util.List;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder> {

    private List<Favorite> favoriteList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Favorite favorite);
    }

    public FavoriteAdapter(List<Favorite> favoriteList, OnItemClickListener listener) {
        this.favoriteList = favoriteList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        Favorite favorite = favoriteList.get(position);
        holder.endNameTextView.setText(favorite.getEndName());

        holder.btnDelete.setOnClickListener(v -> {
            // 삭제 버튼 클릭 시 이벤트 처리
            listener.onItemClick(favorite); // 클릭한 즐겨찾기 항목 전달
        });
    }

    @Override
    public int getItemCount() {
        return favoriteList.size();
    }

    public static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        TextView endNameTextView;
        Button btnDelete;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            endNameTextView = itemView.findViewById(R.id.endNameTextView);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
