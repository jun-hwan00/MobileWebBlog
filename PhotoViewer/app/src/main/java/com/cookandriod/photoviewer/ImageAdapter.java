package com.cookandriod.photoviewer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    private List<PostData> postList;
    private boolean showDetails = false;

    public ImageAdapter(List<PostData> postList) {
        this.postList = postList;
    }

    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        PostData post = postList.get(position);
        holder.imageView.setImageBitmap(post.getImage());

        if (showDetails) {
            holder.textTitle.setVisibility(View.VISIBLE);
            holder.textContent.setVisibility(View.VISIBLE);
            holder.textDate.setVisibility(View.VISIBLE);

            holder.textTitle.setText(post.getTitle());
            holder.textContent.setText(post.getText());
            holder.textDate.setText(post.getDate());
        } else {
            holder.textTitle.setVisibility(View.GONE);
            holder.textContent.setVisibility(View.GONE);
            holder.textDate.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            showDetails = !showDetails;
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textTitle;
        TextView textContent;
        TextView textDate;

        public ImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewItem);
            textTitle = itemView.findViewById(R.id.textTitle);
            textContent = itemView.findViewById(R.id.textContent);
            textDate = itemView.findViewById(R.id.textDate);
        }
    }
}