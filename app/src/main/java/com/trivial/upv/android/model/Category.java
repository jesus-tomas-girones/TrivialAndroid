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

package com.trivial.upv.android.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;

import com.trivial.upv.android.helper.ParcelableHelper;
import com.trivial.upv.android.model.quiz.Quiz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Category implements Parcelable {

    public static final String TAG = "Category";
    public static final Creator<Category> CREATOR = new Creator<Category>() {
        @Override
        public Category createFromParcel(Parcel in) {
            return new Category(in);
        }

        @Override
        public Category[] newArray(int size) {
            return new Category[size];
        }
    };
    private static final int NO_SCORE = 0;
    //JVG.S
//    private static final int SCORE = 8;
    private static final int SCORE = 1;
    //JVG.S
    private final String mName;
    private final String mId;
    private final Theme mTheme;
    private final int[] mScores;
    private List<Quiz> mQuizzes;
    private boolean mSolved;

    //JVG.S
    private final String mImg;
    private final String mMoreInfo;
    private final String mDescription;
    private final String mVideo;

    public String getImg() {
        return mImg;
    }

    public String getMoreInfo() {
        return mMoreInfo;
    }
    public String getDescription() {
        return mDescription;
    }

    public String getVideo() {
        return mVideo;
    }
    //JVG.S

    public Category(@NonNull String name, @NonNull String id, @NonNull Theme theme,
                    @NonNull List<Quiz> quizzes, boolean solved, @NonNull String img, String moreInfo, String description, String video) {
        mName = name;
        mId = id;
        mTheme = theme;
        mQuizzes = quizzes;
        mScores = new int[quizzes.size()];
        mSolved = solved;
        //JVG.S
        mImg = img;
        mMoreInfo = moreInfo;
        mDescription = description;
        mVideo = video;
        //JVG.E
    }

    public Category(@NonNull String name, @NonNull String id, @NonNull Theme theme,
                    @NonNull List<Quiz> quizzes, boolean solved) {
        mName = name;
        mId = id;
        mTheme = theme;
        mQuizzes = quizzes;
        mScores = new int[quizzes.size()];
        mSolved = solved;

        //JVG.S
        mImg = null;
        //JVG.E
        mMoreInfo = null;
        mDescription = null;
        mVideo= null;
    }


    // JVG.S
    public Category(@NonNull String name, @NonNull String id, @NonNull Theme theme,
                    @NonNull List<Quiz> quizzes, @NonNull int[] scores, boolean solved) {
        mName = name;
        mId = id;
        mTheme = theme;

        // JVG.S
        if (quizzes.size() == scores.length) {
            mQuizzes = quizzes;
            mScores = scores;
        } else {
            throw new IllegalArgumentException("Quizzes and scores must have the same length");
        }
        mSolved = solved;
        //JVG.S
        mImg = null;
        mMoreInfo = null;
        mDescription = null;
        mVideo = null;
        //JVG.E
    }
    // JVG.E

    public Category(@NonNull String name, @NonNull String id, @NonNull Theme theme,
                    @NonNull List<Quiz> quizzes, @NonNull int[] scores, boolean solved, @NonNull String img, String moreInfo, String description, String video ) {
        mName = name;
        mId = id;
        mTheme = theme;

        // JVG.S
        /*if (quizzes.size() == scores.length) {
            mQuizzes = quizzes;
            mScores = scores;
        } else {
            throw new IllegalArgumentException("Quizzes and scores must have the same length");
        }*/
        mQuizzes = quizzes;
        mScores = scores;
        //JVG.E
        mSolved = solved;
        mImg = img;
        mMoreInfo = moreInfo;
        mDescription = description;
        mVideo = video;
    }

    protected Category(Parcel in) {
        mName = in.readString();
        mId = in.readString();
        mTheme = Theme.values()[in.readInt()];
        mQuizzes = new ArrayList<>();
        in.readTypedList(mQuizzes, Quiz.CREATOR);
        mScores = in.createIntArray();
        mSolved = ParcelableHelper.readBoolean(in);
        //JVG.S
        mImg = in.readString();
        mMoreInfo = in.readString();
        mDescription = in.readString();
        mVideo = in.readString();
        //JVG.E
    }

    public String getName() {
        return mName;
    }

    public String getId() {
        return mId;
    }

    public Theme getTheme() {
        return mTheme;
    }

    @NonNull
    public List<Quiz> getQuizzes() {
        return mQuizzes;
    }

    /**
     * Updates a score for a provided quiz within this category.
     *
     * @param which           The quiz to rate.
     * @param correctlySolved <code>true</code> if the quiz was solved else <code>false</code>.
     */
    public void setScore(Quiz which, boolean correctlySolved) {
        int index = mQuizzes.indexOf(which);
//        Log.d(TAG, "Setting score for " + which + " with index " + index);
        if (-1 == index) {
            return;
        }
        mScores[index] = correctlySolved ? SCORE : NO_SCORE;
    }

    public boolean isSolvedCorrectly(Quiz quiz) {
        return getScore(quiz) == SCORE;
    }

    /**
     * Gets the score for a single quiz.
     *
     * @param which The quiz to look for
     * @return The score if found, else 0.
     */
    public int getScore(Quiz which) {
        try {
            return mScores[mQuizzes.indexOf(which)];
        } catch (IndexOutOfBoundsException ioobe) {
            return 0;
        }
    }

    /**
     * @return The sum of all quiz scores within this category.
     */
    public int getScore() {
        int categoryScore = 0;
        for (int quizScore : mScores) {
            categoryScore += quizScore;
        }
        return categoryScore;
    }

    public int[] getScores() {
        return mScores;
    }

    public boolean isSolved() {
        return mSolved;
    }

    public void setSolved(boolean solved) {
        this.mSolved = solved;
    }

    /**
     * Checks which quiz is the first unsolved within this category.
     *
     * @return The position of the first unsolved quiz.
     */
    public int getFirstUnsolvedQuizPosition() {
        if (mQuizzes == null) {
            return -1;
        }
        for (int i = 0; i < mQuizzes.size(); i++) {
            if (!mQuizzes.get(i).isSolved()) {
                return i;
            }
        }
        return mQuizzes.size();
    }

    @Override
    public String toString() {
        return "Category{" +
                "mName='" + mName + '\'' +
                ", mId='" + mId + '\'' +
                ", mTheme=" + mTheme +
                ", mQuizzes=" + mQuizzes +
                ", mScores=" + Arrays.toString(mScores) +
                ", mSolved=" + mSolved +
                ", mImg=" + mImg +
                ", mMoreInfo=" + mMoreInfo +
                ", mDescription=" + mDescription +
                ", mVideo=" + mVideo +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeString(mId);
        dest.writeInt(mTheme.ordinal());
        dest.writeTypedList(getQuizzes());
        dest.writeIntArray(mScores);
        ParcelableHelper.writeBoolean(dest, mSolved);
        //JVG.S
        dest.writeString(mImg);
        dest.writeString(mMoreInfo);
        dest.writeString(mDescription);
        dest.writeString(mVideo);
        //JVG.S
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Category category = (Category) o;

        if (!mId.equals(category.mId)) {
            return false;
        }
        if (!mName.equals(category.mName)) {
            return false;
        }
        if (!mQuizzes.equals(category.mQuizzes)) {
            return false;
        }
        if (mTheme != category.mTheme) {
            return false;
        }

        //JVG.S
        if (!mImg.equals(category.mImg)) {
            return false;
        }

        if (!mMoreInfo.equals(category.mMoreInfo)) {
            return false;
        }
        //JVG.E

        return true;
    }

    @Override
    public int hashCode() {
        int result = mName.hashCode();
        result = 31 * result + mId.hashCode();
        result = 31 * result + mTheme.hashCode();
        result = 31 * result + mQuizzes.hashCode();
        result = 31 * result + mImg.hashCode();
        return result;
    }
}
