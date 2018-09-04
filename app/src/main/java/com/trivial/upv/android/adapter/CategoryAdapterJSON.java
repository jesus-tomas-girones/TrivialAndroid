/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.trivial.upv.android.adapter;

import android.app.Activity;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.trivial.upv.android.R;
import com.trivial.upv.android.databinding.ItemCategoryBinding;
import com.trivial.upv.android.helper.ApiLevelHelper;
import com.trivial.upv.android.helper.singleton.volley.VolleySingleton;
import com.trivial.upv.android.model.Category;
import com.trivial.upv.android.persistence.TrivialJSonHelper;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapterJSON extends RecyclerView.Adapter<CategoryAdapterJSON.ViewHolder> {

    public static final String DRAWABLE = "drawable";
    private static final String ICON_CATEGORY = "icon_category_";
    private final Resources mResources;
    private final String mPackageName;
    private final LayoutInflater mLayoutInflater;
    private final Activity mActivity;
    private List<Category> mCategories;

    private OnItemClickListener mOnItemClickListener;
    private OnLongItemClickListener mOnLongItemClickListener;

    public interface OnItemClickListener {
        void onClick(View view, int position);
    }

    public interface OnLongItemClickListener {
        void onClick(View view, int position);
    }

    public CategoryAdapterJSON(Activity activity) {
        mActivity = activity;
        mResources = mActivity.getResources();
        mPackageName = mActivity.getPackageName();
        mLayoutInflater = LayoutInflater.from(activity.getApplicationContext());

        // JVG.S
//        updateCategories();
        mCategories = new ArrayList<>();
        // JVG.E
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder((ItemCategoryBinding) DataBindingUtil
                .inflate(mLayoutInflater, R.layout.item_category, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        ItemCategoryBinding binding = holder.getBinding();
        Category category = mCategories.get(position);
        binding.setCategory(category);
        binding.executePendingBindings();
        setCategoryIcon(category, binding.categoryIcon, position);
        holder.itemView.setBackgroundColor(getColor(category.getTheme().getWindowBackgroundColor()));
        binding.categoryTitle.setTextColor(getColor(category.getTheme().getTextPrimaryColor()));
        binding.categoryTitle.setBackgroundColor(getColor(category.getTheme().getPrimaryColor()));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnItemClickListener.onClick(v, holder.getAdapterPosition());
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mOnLongItemClickListener.onClick(v,  holder.getAdapterPosition());
                return true;
            }
        });
    }

    @Override
    public long getItemId(int position) {
        return mCategories.get(position).getId().hashCode();
    }

    @Override
    public int getItemCount() {
        return mCategories.size();
    }

    public Category getItem(int position) {
        return mCategories.get(position);
    }

    /**
     * @param id Id of changed category.
     * @see RecyclerView.Adapter#notifyItemChanged(int)
     */
    public final void notifyItemChanged(String id) {
        //JVG.S
        //updateCategories(mCategoriesTxt);
        //JVG.E
        notifyItemChanged(getItemPositionById(id));
    }

    private int getItemPositionById(String id) {
        for (int i = 0; i < mCategories.size(); i++) {

            if (mCategories.get(i).getId().equals(id)) {
                return i;
            }

        }
        return -1;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public void setOnLongItemClickListener(OnLongItemClickListener onLongItemClickListener) {
        mOnLongItemClickListener = onLongItemClickListener;
    }

    //JVG.S
    ///private void setCategoryIcon(Category category, ImageView icon) {
    //JVG.E
    private void setCategoryIcon(Category category, final ImageView icon, int position) {
        final int categoryImageResource = mResources.getIdentifier(
                ICON_CATEGORY + category.getId(), DRAWABLE, mPackageName);
        //JVG.S
//        final boolean solved = category.isSolved();
        final boolean solved = TrivialJSonHelper.getInstance(mActivity, false).isSolvedCurrentCategory(position);

        //JVG.E
        if (solved) {
            Drawable solvedIcon = loadSolvedIcon(category, categoryImageResource);
            icon.setImageDrawable(solvedIcon);
        } else {
            //JVG.S
            /// icon.setImageResource(categoryImageResource);
            //JVG.E
            VolleySingleton.getInstance(mActivity.getBaseContext()).getImageLoader().get(category.getImg(), new ImageLoader.ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                    icon.setImageBitmap(response.getBitmap());
                }

                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("VOLLEY", "Error cargando icon!");
                }
            });
            ////icon.setImageUrl(category.getImg(),VolleySingleton.getLectorImagenes());
            ////icon.setImageResource(categoryImageResource);
        }
    }


    public void updateCategories() {
        // JVG.S
        //return TopekaDatabaseHelper.getCategories(activity, true);
        if (TrivialJSonHelper.getInstance(mActivity, false).isLoaded()) {
            mCategories = TrivialJSonHelper.getInstance(mActivity, false).getCategories(true);
//            Log.d("ADAPTER", "CATEGORIAS CARGADAS");
        } else {
            if (mCategories!=null) {
                mCategories.clear();
            }
            else
                mCategories = new ArrayList<>();
//            Log.d("ADAPTER", "NO HA TERMINADO LA CARGA DE LAS CATEGORIAS");
        }

        // JVG.E
    }

    /**
     * Loads an icon that indicates that a category has already been solved.
     *
     * @param category              The solved category to display.
     * @param categoryImageResource The category's identifying image.
     * @return The icon indicating that the category has been solved.
     */
    private Drawable loadSolvedIcon(Category category, int categoryImageResource) {
        if (ApiLevelHelper.isAtLeast(Build.VERSION_CODES.LOLLIPOP)) {
            return loadSolvedIconLollipop(category, categoryImageResource);
        }
        return loadSolvedIconPreLollipop(category, categoryImageResource);
    }

    @NonNull
    private LayerDrawable loadSolvedIconLollipop(Category category, int categoryImageResource) {
        final Drawable done = loadTintedDoneDrawable();
        final Drawable categoryIcon = loadTintedCategoryDrawable(category, categoryImageResource);
        Drawable[] layers = new Drawable[]{categoryIcon, done}; // ordering is back to front
        return new LayerDrawable(layers);
    }

    private Drawable loadSolvedIconPreLollipop(Category category, int categoryImageResource) {
        return loadTintedCategoryDrawable(category, categoryImageResource);
    }

    /**
     * Loads and tints a drawable.
     *
     * @param category              The category providing the tint color
     * @param categoryImageResource The image resource to tint
     * @return The tinted resource
     */
    private Drawable loadTintedCategoryDrawable(Category category, int categoryImageResource) {
        //JVG.S
        /*final Drawable categoryIcon = ContextCompat
                .getDrawable(mActivity, categoryImageResource).mutate();*/
        //JVG.E
        final Drawable[] categoryIcon = new Drawable[1];
        VolleySingleton.getInstance(mActivity.getBaseContext()).getImageLoader().get(category.getImg(), new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                categoryIcon[0] = new BitmapDrawable(mActivity.getResources(), response.getBitmap());
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("VOLLEY", "Error cargando icon!");
            }
        });

        return wrapAndTint(categoryIcon[0], category.getTheme().getPrimaryColor());
    }

    /**
     * Loads and tints a check mark.
     *
     * @return The tinted check mark
     */
    private Drawable loadTintedDoneDrawable() {
        final Drawable done = ContextCompat.getDrawable(mActivity, R.drawable.ic_tick);
        return wrapAndTint(done, android.R.color.white);
    }

    private Drawable wrapAndTint(Drawable done, @ColorRes int color) {
        Drawable compatDrawable = DrawableCompat.wrap(done);
        DrawableCompat.setTint(compatDrawable, getColor(color));
        return compatDrawable;
    }

    /**
     * Convenience method for color loading.
     *
     * @param colorRes The resource id of the color to load.
     * @return The loaded color.
     */
    private int getColor(@ColorRes int colorRes) {
        return ContextCompat.getColor(mActivity, colorRes);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private ItemCategoryBinding mBinding;

        public ViewHolder(ItemCategoryBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

        public ItemCategoryBinding getBinding() {
            return mBinding;
        }
    }
}
