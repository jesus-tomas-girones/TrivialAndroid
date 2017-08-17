package com.trivial.upv.android.persistence;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.trivial.upv.android.model.JsonAttributes;
import com.trivial.upv.android.model.quiz.FourQuarterQuiz;
import com.trivial.upv.android.model.quiz.MultiSelectQuiz;
import com.trivial.upv.android.model.quiz.Quiz;
import com.trivial.upv.android.model.quiz.SelectItemQuiz;

import java.lang.reflect.Type;

/**
 * Created by jvg63 on 07/08/2017.
 */

class QuizDeserializer implements JsonDeserializer<Quiz> {
    @Override
    public Quiz deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject quiz = json.getAsJsonObject();

        if (!quiz.has("mQuizType"))
            Log.d("ACTIVO","ERROR");
        String type = quiz.get("mQuizType").getAsString();

        Quiz tmpQuiz = null;

        tmpQuiz = TopekaJSonHelper.createQuizDueToTypeJson(quiz, type);

        return tmpQuiz;
    }


}
