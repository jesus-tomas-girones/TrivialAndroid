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

import android.os.Parcel;
import android.os.Parcelable;

import com.trivial.upv.android.helper.AnswerHelper;

import java.util.Arrays;

public final class FourQuarterQuiz extends OptionsQuiz<String> {

    public static final Parcelable.Creator<FourQuarterQuiz> CREATOR
            = new Parcelable.Creator<FourQuarterQuiz>() {
        @Override
        public FourQuarterQuiz createFromParcel(Parcel in) {
            return new FourQuarterQuiz(in);
        }

        @Override
        public FourQuarterQuiz[] newArray(int size) {
            return new FourQuarterQuiz[size];
        }
    };

    //JVG.S
//    public FourQuarterQuiz(String question, int[] answer, String[] options, boolean solved) {
//        super(question, answer, options, solved);
//    }
    public FourQuarterQuiz(String question, int[] answer, String[] options, String[] comments, boolean solved) {
        super(question, answer, options, solved);

        //JVG.S
        mComments = comments;
        //JVG.E
    }

    public String[] getComments() {
        return mComments;
    }

    public void setComments(String[] mComments) {
        this.mComments = mComments;
    }

    private String[] mComments;
    //JVG.E


    public FourQuarterQuiz(Parcel in) {
        super(in);
        String options[] = in.createStringArray();
        setOptions(options);
        //JVG.S
        mComments = in.createStringArray();
        //JVG.E
    }

    @Override
    public QuizType getType() {
        return QuizType.FOUR_QUARTER;
    }

    @Override
    public String getStringAnswer() {
        return AnswerHelper.getAnswer(getAnswer(), getOptions());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        String[] options = getOptions();
        dest.writeStringArray(options);
        //JVG.S
        dest.writeStringArray(getComments());
        //JVG.E
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FourQuarterQuiz)) {
            return false;
        }

        FourQuarterQuiz quiz = (FourQuarterQuiz) o;
        final int[] answer = getAnswer();
        final String question = getQuestion();
        if (answer != null ? !Arrays.equals(answer, quiz.getAnswer()) : quiz.getAnswer() != null) {
            return false;
        }
        if (!question.equals(quiz.getQuestion())) {
            return false;
        }

        //noinspection RedundantIfStatement
        if (!Arrays.equals(getOptions(), quiz.getOptions())) {
            return false;
        }

        //JVG.S
        if (!Arrays.equals(getComments(), quiz.getComments())) {
            return false;
        }
        //JVG.S

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(getOptions());
        result = 31 * result + Arrays.hashCode(getAnswer());
        //JVG.S
        result = 31 * result + Arrays.hashCode(getComments());
        //JVG.E
        return result;
    }

}
