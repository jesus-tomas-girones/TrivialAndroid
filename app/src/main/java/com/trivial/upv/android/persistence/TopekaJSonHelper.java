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

package com.trivial.upv.android.persistence;

import com.trivial.upv.android.helper.JsonHelper;
import com.trivial.upv.android.model.JsonAttributes;
import com.trivial.upv.android.model.pojo.preguntastxt.QuestionTXT;
import com.trivial.upv.android.model.quiz.AlphaPickerQuiz;
import com.trivial.upv.android.model.quiz.FillBlankQuiz;
import com.trivial.upv.android.model.quiz.FillTwoBlanksQuiz;
import com.trivial.upv.android.model.quiz.FourQuarterQuiz;
import com.trivial.upv.android.model.quiz.MultiSelectQuiz;
import com.trivial.upv.android.model.quiz.PickerQuiz;
import com.trivial.upv.android.model.quiz.Quiz;
import com.trivial.upv.android.model.quiz.SelectItemQuiz;
import com.trivial.upv.android.model.quiz.ToggleTranslateQuiz;
import com.trivial.upv.android.model.quiz.TrueFalseQuiz;

import org.json.JSONArray;

/**
 * Database for storing and retrieving info for subtemas and quizzes
 */
public class TopekaJSonHelper {

    public static Quiz createQuizDueToType(QuestionTXT questionTXT, String type) {
        // "magic numbers" based on QuizTable#PROJECTION
        //final String type = cursor.getString(2);
        final String question = questionTXT.getEnunciado();
        final String answer = new JSONArray(questionTXT.getRespuestaCorrecta()).toString();
        final String options = new JSONArray(questionTXT.getRespuestas()).toString();
        final int min = 0;
        final int max = 0;
        final int step = 0;
        final boolean solved = false;

        switch (type) {
            case JsonAttributes.QuizType.ALPHA_PICKER: {
                return new AlphaPickerQuiz(question, answer, solved);
            }
            case JsonAttributes.QuizType.FILL_BLANK: {
                return createFillBlankQuiz(question, answer, solved);
            }
            case JsonAttributes.QuizType.FILL_TWO_BLANKS: {
                return createFillTwoBlanksQuiz(question, answer, solved);
            }
            case JsonAttributes.QuizType.FOUR_QUARTER: {
                return createFourQuarterQuiz(question, answer, options, solved);
            }
            case JsonAttributes.QuizType.MULTI_SELECT: {
                return createMultiSelectQuiz(question, answer, options, solved);
            }
            case JsonAttributes.QuizType.PICKER: {
                return new PickerQuiz(question, Integer.valueOf(answer), min, max, step, solved);
            }
            case JsonAttributes.QuizType.SINGLE_SELECT:
                //fall-through intended
            case JsonAttributes.QuizType.SINGLE_SELECT_ITEM: {
                return createSelectItemQuiz(question, answer, options, solved);
            }
            case JsonAttributes.QuizType.TOGGLE_TRANSLATE: {
                return createToggleTranslateQuiz(question, answer, options, solved);
            }
            case JsonAttributes.QuizType.TRUE_FALSE: {
                return createTrueFalseQuiz(question, answer, solved);

            }
            default: {
                throw new IllegalArgumentException("Quiz type " + type + " is not supported");
            }
        }
    }

    private static Quiz createFillBlankQuiz(String question,
                                            String answer, boolean solved) {
        /*JVG.S*/
        final String start = "";
        final String end = "";
        /*JVG.E*/
        return new FillBlankQuiz(question, answer, start, end, solved);
    }

    private static Quiz createFillTwoBlanksQuiz(String question, String answer, boolean solved) {
        final String[] answerArray = JsonHelper.jsonArrayToStringArray(answer);
        return new FillTwoBlanksQuiz(question, answerArray, solved);
    }

    private static Quiz createFourQuarterQuiz(String question, String answer,
                                              String options, boolean solved) {
        final int[] answerArray = JsonHelper.jsonArrayToIntArray(answer);
        final String[] optionsArray = JsonHelper.jsonArrayToStringArray(options);
        return new FourQuarterQuiz(question, answerArray, optionsArray, solved);
    }

    private static Quiz createMultiSelectQuiz(String question, String answer,
                                              String options, boolean solved) {
        final int[] answerArray = JsonHelper.jsonArrayToIntArray(answer);
        final String[] optionsArray = JsonHelper.jsonArrayToStringArray(options);
        return new MultiSelectQuiz(question, answerArray, optionsArray, solved);
    }

    private static Quiz createSelectItemQuiz(String question, String answer,
                                             String options, boolean solved) {
        final int[] answerArray = JsonHelper.jsonArrayToIntArray(answer);
        final String[] optionsArray = JsonHelper.jsonArrayToStringArray(options);
        return new SelectItemQuiz(question, answerArray, optionsArray, solved);
    }

    private static Quiz createToggleTranslateQuiz(String question, String answer,
                                                  String options, boolean solved) {
        final int[] answerArray = JsonHelper.jsonArrayToIntArray(answer);
        // JVG.S
//        final String[][] optionsArrays = extractOptionsArrays(options);
        final String[][] optionsArrays = null;
        // JVG.E
        return new ToggleTranslateQuiz(question, answerArray, optionsArrays, solved);
    }

    private static Quiz createTrueFalseQuiz(String question, String answer, boolean solved) {
    /*
     * parsing json with the potential values "true" and "false"
     * see res/raw/subtemas.json for reference
     */
        final boolean answerValue = "true".equals(answer);
        return new TrueFalseQuiz(question, answerValue, solved);
    }


}
