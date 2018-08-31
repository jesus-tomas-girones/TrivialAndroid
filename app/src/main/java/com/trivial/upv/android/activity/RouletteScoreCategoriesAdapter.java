package com.trivial.upv.android.activity;

import android.content.Context;
import android.content.res.TypedArray;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.google.android.gms.common.images.ImageManager;
import com.trivial.upv.android.R;
import com.trivial.upv.android.helper.singleton.volley.VolleySingleton;
import com.trivial.upv.android.model.Theme;
import com.trivial.upv.android.widget.roulette.RouletteView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class RouletteScoreCategoriesAdapter extends android.support.v7.widget.RecyclerView.Adapter<RouletteScoreCategoriesAdapter.ViewHolder> {
    private final int backgroundColor;
    private String mImagePlayers[] = null;
    private Context mContext = null;
    private List[] mImagesCategories;
    private List[] mThemesCategories;
    private int mCurrentTurnPlayer;

    @NonNull
    @Override
    public RouletteScoreCategoriesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        View score = (View) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_score_players_categories_roulette, parent, false);
        RouletteScoreCategoriesAdapter.ViewHolder vh = new RouletteScoreCategoriesAdapter.ViewHolder(score);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull final RouletteScoreCategoriesAdapter.ViewHolder holder, int position) {
        if (!mImagePlayers[position].isEmpty()) {
            if (mImagePlayers[position].startsWith("content://")) {
                ImageManager imageManager = ImageManager.create(mContext);
                imageManager.loadImage(holder.mImageView, Uri.parse(mImagePlayers[position]));
            } else if (mImagePlayers[position].equals("default")) {
                holder.mImageView.setImageResource(android.R.drawable.ic_menu_help);

            } else {
                VolleySingleton.getInstance(mContext).getImageLoader().get(mImagePlayers[position], new ImageLoader.ImageListener() {
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
//        holder.mRouletteView.setText(mScore[position]);
        if (mCurrentTurnPlayer == position)
            holder.mContainer.setBackgroundColor(mContext.getResources().getColor(android.R.color.holo_green_light));
        else
            holder.mContainer.setBackgroundColor(backgroundColor);

        holder.mRouletteView.setRotationEventListener(null, false);
        holder.mRouletteView.setLine1(null);
        holder.mRouletteView.setVisibility(View.VISIBLE);
        holder.mRouletteView.setNumSectors(true, mImagesCategories[position].size(), (String[]) mImagesCategories[position].toArray(new String[0]), (Theme[]) mThemesCategories[position].toArray(new Theme[0]));

    }

    @Override
    public int getItemCount() {
        return mImagePlayers.length;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final View mContainer;
        // each data item is just a string in this case
        public RouletteView mRouletteView;
        public ImageView mImageView;

        public ViewHolder(View v) {
            super(v);

            mImageView = v.findViewById(R.id.imgCategory);
            mRouletteView = v.findViewById(R.id.rouletteView);
            mContainer = v.findViewById(R.id.container);
        }
    }


    public RouletteScoreCategoriesAdapter(Context context, int currentTurnPlayer, String[] imgPlayers, List[] imagesCategories, List[] themesCategories) {
        this.mImagePlayers = imgPlayers;
        mContext = context;
        this.mImagesCategories = imagesCategories;
        this.mThemesCategories = themesCategories;

        mCurrentTurnPlayer = currentTurnPlayer;

        //background color
        TypedArray array = context.getTheme().obtainStyledAttributes(new int[] {
                android.R.attr.colorBackground,
        });
        backgroundColor = array.getColor(0, 0xFF00FF);
        array.recycle();
    }

}
