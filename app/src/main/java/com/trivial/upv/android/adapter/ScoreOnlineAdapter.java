/*
 * Creador: JVG
 */

package com.trivial.upv.android.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.trivial.upv.android.R;
import com.trivial.upv.android.model.gpg.Game;
import com.trivial.upv.android.model.gpg.ScoreOnline;

/**
 * Adapter for displaying score cards.
 */
public class ScoreOnlineAdapter extends BaseAdapter {

    private final ScoreOnline mScoreOnline;
    private final Context mContext;

    private Drawable mSuccessIcon;
    private Drawable mFailedIcon;

    public ScoreOnlineAdapter(ScoreOnline scoreOnline, Context context) {
        mScoreOnline = scoreOnline;
        mContext = context;
    }

    @Override
    public int getCount() {
        return mScoreOnline.getScoreOnline().size();
    }

    @Override
    public ScoreOnline.Score getItem(int position) {
        return mScoreOnline.getScoreOnline().get(position);
    }

    @Override
    public long getItemId(int position) {
        if (position > getCount() || position < 0) {
            return AbsListView.INVALID_POSITION;
        }
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (null == convertView) {
            convertView = createView(parent);
        }

        final ScoreOnline.Score score = getItem(position);
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();

        if (position == 0) {
//            viewHolder.mDisplayName.setTextColor(mContext.getResources().getColor(R.color.theme_blue_text));
//            viewHolder.mPoints.setTextColor(mContext.getResources().getColor(R.color.theme_blue_text));
//            viewHolder.mStatus.setTextColor(mContext.getResources().getColor(R.color.theme_blue_text));
//            viewHolder.mTimeLeft.setTextColor(mContext.getResources().getColor(R.color.theme_blue_text));

            viewHolder.mDisplayName.setText("PLAYER");
            viewHolder.mTimeLeft.setText("TIME");
            viewHolder.mPoints.setText("POINTS");
            viewHolder.mStatus.setText("STATUS");

            viewHolder.mWin.setVisibility(View.INVISIBLE);
        } else {
//            viewHolder.mDisplayName.setTextColor(mContext.getResources().getColor(R.color.topeka_blank));
//            viewHolder.mPoints.setTextColor(mContext.getResources().getColor(R.color.topeka_blank));
//            viewHolder.mStatus.setTextColor(mContext.getResources().getColor(R.color.topeka_blank));
//            viewHolder.mTimeLeft.setTextColor(mContext.getResources().getColor(R.color.topeka_blank));
            viewHolder.mWin.setVisibility(View.VISIBLE);
            if (score.getParticipant().equals(Game.mMyId)) {
                viewHolder.mDisplayName.setText(bold(score.getDisplayName()));
                viewHolder.mStatus.setText(bold(score.getStatus()));
                viewHolder.mTimeLeft.setText(bold(String.valueOf(score.getTimeLeft())));
                viewHolder.mPoints.setText(bold(String.valueOf(score.getPoints())));
                setSolvedStateForQuiz(viewHolder.mWin, position);
            } else {
                if (score.getStatus().equals("FINISHED")) {
                    viewHolder.mTimeLeft.setText(String.valueOf(score.getTimeLeft()));
                    viewHolder.mPoints.setText(String.valueOf(score.getPoints()));
                } else {
                    viewHolder.mTimeLeft.setText("--");
                    viewHolder.mPoints.setText("--");
                }
                viewHolder.mDisplayName.setText(score.getDisplayName());
                viewHolder.mStatus.setText(score.getStatus());
                setSolvedStateForQuiz(viewHolder.mWin, position);
            }
        }
        return convertView;
    }

    private Spanned bold(String text) {
        return Html.fromHtml("<B>" + text + "</B>");
    }

    private void setSolvedStateForQuiz(ImageView solvedState, int position) {
        final Context context = solvedState.getContext();
        final Drawable tintedImage;
        if (getItem(position).isWinner()) {
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
                R.layout.item_score_online_card, parent, false);
        convertView = scorecardItem;
        ViewHolder holder = new ViewHolder(scorecardItem);
        convertView.setTag(holder);
        return convertView;
    }

    private TextView tmpImg;


    private class ViewHolder {

        TextView mTimeLeft;
        TextView mPoints;
        TextView mDisplayName;
        TextView mStatus;
        ImageView mWin;

        public ViewHolder(ViewGroup scorecardItem) {
            mTimeLeft = (TextView) scorecardItem.findViewById(R.id.timeLeft);
            mPoints = (TextView) scorecardItem.findViewById(R.id.points);
            mDisplayName = (TextView) scorecardItem.findViewById(R.id.display_name);
            mWin = (ImageView) scorecardItem.findViewById(R.id.winner);
            mStatus = (TextView) scorecardItem.findViewById(R.id.status);
        }

    }
}
