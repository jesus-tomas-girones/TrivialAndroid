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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.pixplicity.htmlcompat.HtmlCompat;
import com.trivial.upv.android.R;
import com.trivial.upv.android.helper.singleton.VolleySingleton;
import com.trivial.upv.android.model.Category;
import com.trivial.upv.android.model.quiz.Quiz;

import org.xml.sax.Attributes;

import java.util.List;

import static java.security.AccessController.getContext;

/**
 * Adapter for displaying score cards.
 */
public class ScoreAdapter extends BaseAdapter  implements HtmlCompat.ImageGetter{

    private final Category mCategory;
    private final int count;
    private final List<Quiz> mQuizList;
    private final Context mContext;

    private Drawable mSuccessIcon;
    private Drawable mFailedIcon;

    public ScoreAdapter(Category category, Context context) {
        mCategory = category;
        mQuizList = mCategory.getQuizzes();
        count = mQuizList.size();
        mContext = context;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public Quiz getItem(int position) {
        return mQuizList.get(position);
    }

    @Override
    public long getItemId(int position) {
        if (position > count || position < 0) {
            return AbsListView.INVALID_POSITION;
        }
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (null == convertView) {
            convertView = createView(parent);
        }

        final Quiz quiz = getItem(position);
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        //JVG.S
//        viewHolder.mQuizView.setText(quiz.getQuestion());
//        viewHolder.mAnswerView.setText(quiz.getStringAnswer());
        tmpImg = viewHolder.mQuizView;
        Spanned fromHtml = HtmlCompat.fromHtml(mContext, quiz.getQuestion(),0, this);
// You may want to provide an ImageGetter, TagHandler and SpanCallback:
//Spanned fromHtml = HtmlCompat.fromHtml(context, source, 0,
//        imageGetter, tagHandler, spanCallback);
        //viewHolder.mQuizView.setMovementMethod(LinkMovementMethod.getInstance());
        viewHolder.mQuizView.setText(fromHtml);
        tmpImg = viewHolder.mQuizView;
        fromHtml = HtmlCompat.fromHtml(mContext, quiz.getStringAnswer(), 0, this);
        viewHolder.mAnswerView.setText(fromHtml);
        //JVG.E
        setSolvedStateForQuiz(viewHolder.mSolvedState, position);
        return convertView;
    }

    private void setSolvedStateForQuiz(ImageView solvedState, int position) {
        final Context context = solvedState.getContext();
        final Drawable tintedImage;
        if (mCategory.isSolvedCorrectly(getItem(position))) {
            tintedImage = getSuccessIcon(context);
        } else {
            tintedImage = getFailedIcon(context);
        }
        solvedState.setImageDrawable(tintedImage);
    }

    private Drawable getSuccessIcon(Context context) {
        if (null == mSuccessIcon) {
            mSuccessIcon = loadAndTint(context, R.drawable.ic_tick, R.color.theme_green_primary);
        }
        return mSuccessIcon;
    }

    private Drawable getFailedIcon(Context context) {
        if (null == mFailedIcon) {
            mFailedIcon = loadAndTint(context, R.drawable.ic_cross, R.color.theme_red_primary);
        }
        return mFailedIcon;
    }

    /**
     * Convenience method to aid tintint of vector drawables at runtime.
     *
     * @param context    The {@link Context} for this app.
     * @param drawableId The id of the drawable to load.
     * @param tintColor  The tint to apply.
     * @return The tinted drawable.
     */
    private Drawable loadAndTint(Context context, @DrawableRes int drawableId,
                                 @ColorRes int tintColor) {
        Drawable imageDrawable = ContextCompat.getDrawable(context, drawableId);
        if (imageDrawable == null) {
            throw new IllegalArgumentException("The drawable with id " + drawableId
                    + " does not exist");
        }
        DrawableCompat.setTint(DrawableCompat.wrap(imageDrawable), tintColor);
        return imageDrawable;
    }

    private View createView(ViewGroup parent) {
        View convertView;
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewGroup scorecardItem = (ViewGroup) inflater.inflate(
                R.layout.item_scorecard, parent, false);
        convertView = scorecardItem;
        ViewHolder holder = new ViewHolder(scorecardItem);
        convertView.setTag(holder);
        return convertView;
    }

    private TextView tmpImg;

    @Override
    public Drawable getDrawable(String source, Attributes attr) {
        final LevelListDrawable drawableTmp = new LevelListDrawable();
        Drawable empty;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            empty = mContext.getResources().getDrawable(R.drawable.ic_cross, mContext.getTheme());
        } else {
            empty = mContext.getResources().getDrawable(R.drawable.ic_cross);
        }
        drawableTmp.addLevel(0, 0, empty);
        drawableTmp.setBounds(0, 0, empty.getIntrinsicWidth(), empty.getIntrinsicHeight());

        VolleySingleton.getInstance(mContext).getImageLoader().get(source, new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                Bitmap bitmap = response.getBitmap();
                if (response.getBitmap() != null) {
                    BitmapDrawable drawable = new BitmapDrawable(mContext.getResources(), bitmap);
                    drawableTmp.addLevel(1, 1, drawable);
                    drawableTmp.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                    drawableTmp.setLevel(1);
                    // i don't know yet a better way to refresh TextView
                    // mTv.invalidate() doesn't work as expected
                    CharSequence t = tmpImg.getText();
                    tmpImg.setText(t);
                    //mTv.invalidate();
                }
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("VOLLEY", "Error cargando icon!");
            }
        });

        return drawableTmp;
    }


    private class ViewHolder {

        final TextView mAnswerView;
        final TextView mQuizView;
        final ImageView mSolvedState;

        public ViewHolder(ViewGroup scorecardItem) {
            mQuizView = (TextView) scorecardItem.findViewById(R.id.quiz);
            mAnswerView = (TextView) scorecardItem.findViewById(R.id.answer);
            mSolvedState = (ImageView) scorecardItem.findViewById(R.id.solved_state);
        }

    }
}
