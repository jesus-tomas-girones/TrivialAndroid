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

package com.trivial.upv.android.model.quiz;

import android.annotation.SuppressLint;
import android.os.Parcel;

import com.trivial.upv.android.helper.AnswerHelper;
import com.trivial.upv.android.helper.AnswerHelper;

@SuppressLint("ParcelCreator")
public final class SelectItemQuiz extends OptionsQuiz<String> {

    // JVG.S
//    public SelectItemQuiz(String question, int[] answer, String[] options, boolean solved) {
    public SelectItemQuiz(String question, int[] answer, String[] options, String[] comments, boolean solved) {
        super(question, answer, options, solved);
        // JVG.S
        mComments = comments;
    }

    public String[] getComments() {
        return mComments;
    }

    public void setComments(String[] mComments) {
        this.mComments = mComments;
    }

    private String[] mComments;
    //JVG.E

    @SuppressWarnings("unused")
    public SelectItemQuiz(Parcel in) {
        super(in);
        String[] options = in.createStringArray();
        setOptions(options);
        // JVG.S
        mComments = in.createStringArray();
        // JVG.E
    }

    @Override
    public QuizType getType() {
        return QuizType.SINGLE_SELECT;
    }

    @Override
    public String getStringAnswer() {
        return AnswerHelper.getAnswer(getAnswer(), getOptions());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeStringArray(getOptions());
        // JVG.S
        dest.writeStringArray(getComments());
        // JVG.E
    }
}
