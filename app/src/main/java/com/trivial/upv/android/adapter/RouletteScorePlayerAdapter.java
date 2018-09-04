package com.trivial.upv.android.adapter;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.google.android.gms.common.images.ImageManager;
import com.trivial.upv.android.R;
import com.trivial.upv.android.helper.singleton.volley.VolleySingleton;

public class RouletteScorePlayerAdapter extends android.support.v7.widget.RecyclerView.Adapter<RouletteScorePlayerAdapter.ViewHolder> {
    private String mScore[] = null;
    private String mImagenes[] = null;
    private Context mContext = null;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        View score = (View) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_score_player, parent, false);
        ViewHolder vh = new ViewHolder(score);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        if (!mImagenes[position].isEmpty()) {
            if (mImagenes[position].startsWith("content://")) {
                ImageManager imageManager = ImageManager.create(mContext);
                imageManager.loadImage(holder.mImageView, Uri.parse(mImagenes[position]));
            } else if (mImagenes[position].equals("default")) {
                holder.mImageView.setImageResource(android.R.drawable.ic_menu_help);

            } else {
                VolleySingleton.getInstance(mContext).getImageLoader().get(mImagenes[position], new ImageLoader.ImageListener() {
                    @Override
                    public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                        if (response != null && response.getBitmap() != null) {
                            holder.mImageView.setImageBitmap(response.getBitmap());
                            holder.mImageView.setVisibility(View.VISIBLE);
                        } else {
                            holder.mImageView.setImageBitmap(null);
                            holder.mImageView.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
            }
        } else {
            holder.mImageView.setImageBitmap(null);
            holder.mImageView.setVisibility(View.GONE);
        }
        holder.mTextView.setText(mScore[position]);
        if (!mScore[position].isEmpty()) {
            holder.mTextView.setVisibility(View.VISIBLE);
        } else {
            holder.mTextView.setVisibility(View.GONE);

        }
    }

    @Override
    public int getItemCount() {
        return mImagenes.length;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mTextView;
        public ImageView mImageView;

        public ViewHolder(View v) {
            super(v);

            mTextView = v.findViewById(R.id.txtScore);
            mImageView = v.findViewById(R.id.imgCategory);
        }
    }


    public RouletteScorePlayerAdapter(Context context, String[] score, String[] imagenes) {
        this.mScore = score;
        this.mImagenes = imagenes;
        mContext = context;
    }


}
