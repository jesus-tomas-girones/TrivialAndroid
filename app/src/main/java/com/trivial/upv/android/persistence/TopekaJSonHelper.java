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

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.trivial.upv.android.helper.JsonHelper;
import com.trivial.upv.android.helper.singleton.VolleySingleton;
import com.trivial.upv.android.model.JsonAttributes;
import com.trivial.upv.android.model.Theme;
import com.trivial.upv.android.model.json.CategoryJSON;
import com.trivial.upv.android.model.txtquiz.QuestionTXT;
import com.trivial.upv.android.model.txtquiz.QuestionsTXTHelper;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Database for storing and retrieving info for subtemas and quizzes
 */
public class TopekaJSonHelper {
    public static final String ACTION_RESP = "com.trivial.upv.android.activity.END_LOADING_CATEGORIES";

    private static Context mContext = null;

    private boolean isLoaded = false;


    private TopekaJSonHelper(final Context context) {
        isLoaded = false;
        mContext = context.getApplicationContext();
    }


    //1) Obtiene json de Firebase:
//    https://trivialandroid-d2b33.firebaseio.com/ (FireBase del Proyecto)
//2) Compara la versión almacenada en local con la versión descargada
//	2.1) Si son iguales, búsca si la información está cacheada (fichero json deserializada) con las puntuaciones, quizzes resueltos, etc
//		2.1.1) Si la información esta cacheada la carga en POJO: List<Category>
//		2.1.2) Si no,
//			2.1.2.1) carga List<Category> con los Quizzes a partir de los ficheros txt ubicados en internet, y lo complementa con el ficher .json de categorias
//			2.1.2.2) Cachéa la información una vez finalizado
//	2.2) Si son distintos
//		2.2.1) carga List<Category> con los Quizzes a partir de los ficheros txt ubicados en internet, y lo complementa con el ficher .json de categorias
//		2.2.2) Cachéa la información una vez finalizado
//3) Devueve el control a la aplicación.
    private void isSameFileJSON() {
        StringRequest request = new StringRequest(Request.Method.GET, QuestionsTXTHelper.JsonURL, new Response.Listener<String>() {
            @Override
            public void onResponse(final String response) {
                new Thread() {
                    public void run() {
                        String[] preguntas = response.split("\\r?\\n");

                        try {
                            BufferedReader br = new BufferedReader(new InputStreamReader(mContext.openFileInput("categories.json")));

                            // Line JSON saved
                            String line = null;

                            boolean iguales = true;
                            int contador = 0;
                            while ((line = br.readLine()) != null && contador < preguntas.length && iguales) {
                                if (!preguntas[contador].equals(line)) {
                                    iguales = false;
                                    break;
                                }
                                contador++;
                            }
                            br.close();

                            if (contador > 0 && iguales && contador == preguntas.length && line == null) {
                            /* The same file  Take information from cache*/
                                Log.d("TRAZA", "fichero no se ha modificado");
                                readCategoriesFromCache(response, preguntas);
                                return;
                            } else {
                                // New File categories readed. Update
                                readNewFileJson(response, preguntas, true);

                                return;
                            }

                        } catch (FileNotFoundException e1) {
                            e1.printStackTrace();
                            Log.d("TRAZA", "nuevo fichero");
                            try {
                                readNewFileJson(response, preguntas, true);
                                return;
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                                sendBroadCastError("FILE", "Error creando fichero IO");
                            }

                            // continuar carga
                        } catch (IOException e1) {
                            e1.printStackTrace();

                            sendBroadCastError("FILE", "Error comparando IO");

                            return;
                        }

                    }
                }.start();
            }
        }, new Response.ErrorListener()

        {
            @Override
            public void onErrorResponse(VolleyError error) {
                sendBroadCastError("FILE", "Error recuperando JsonURL");
            }
        });
        VolleySingleton.getColaPeticiones().add(request);
    }

    public void sendBroadCastError(String errorCode, String errorDescription) {
        Log.d(errorCode, errorDescription);

        sendBroadCastMessage("ERROR");
        isLoaded = false;

        cancelRequests();
    }

    private void readNewFileJson(final String response, String[] preguntas, boolean createFileCategories) throws FileNotFoundException {
        // File is diferent
        if (createFileCategories)
            newFileJson(preguntas);


        JSONObject categoriesJSON = null;
        try {
            categoriesJSON = new JSONObject(response);
            readCategoriesFromJSON(categoriesJSON);
        } catch (JSONException e) {
            sendBroadCastError("JSON", "Error comparando JSON IO");
            e.printStackTrace();

            return;
        }
        ///sendBroadCastMessage("OK");
    }


    private void newFileJson(String[] preguntas) throws FileNotFoundException {
         /* New File*/

        PrintWriter pw = null;
        pw = new PrintWriter(mContext.openFileOutput("categories.json", Context.MODE_PRIVATE));

        String line;
        for (int contador = 0; contador < preguntas.length; contador++) {
            line = preguntas[contador];
            pw.println(line);
        }
        pw.close();
        Log.d("TRAZA", "nuevo fichero creado");

    }


    public List<CategoryJSON> categories;

    public List<CategoryJSON> getCategoriesCurrent() {
        return categoriesCurrent;
    }

    public List<CategoryJSON> categoriesCurrent;
    public ArrayList<List<CategoryJSON>> categoriesPath;


    private void readCategoriesFromJSON(final JSONObject response) {

        QuestionsTXTHelper helper = new QuestionsTXTHelper(mContext);
        try {
            resetData();
            categories = helper.readCategoriesFromJSON(response);
            categoriesCurrent = categories;
            categoriesPath = new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
            sendBroadCastError("Volley", "Error IO");
        } catch (JSONException e) {
            e.printStackTrace();
            sendBroadCastError("Volley", "Error JSON");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            sendBroadCastError("Volley", "Error URI");
        }
    }

    private static TopekaJSonHelper mInstance = null;

    public synchronized static TopekaJSonHelper getInstance(Context context, boolean forceLoad) {
        if (mInstance == null) {
            mInstance = new TopekaJSonHelper(context.getApplicationContext());
        }

        if (forceLoad)
            mInstance.isSameFileJSON();

        return mInstance;
    }



    public static void sendBroadCastMessage(String msg) {
        Intent i = new Intent();
        i.setAction(ACTION_RESP);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.putExtra("RESULT", msg);
        mContext.sendBroadcast(i);
    }

    public static void sendBroadCastMessageRefresh(int val) {
        Intent i = new Intent();
        i.setAction(ACTION_RESP);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.putExtra("RESULT", "REFRESH");
        i.putExtra("REFRESH", val);
        mContext.sendBroadcast(i);
    }

    public static Quiz createQuizDueToType(QuestionTXT questionTXT, String type) {
//JVG.S
// "magic numbers" based on QuizTable#PROJECTION
//        final String type = cursor.getString(2);
//        final String question = cursor.getString(3);
//        final String answer = cursor.getString(4);
//        final String options = cursor.getString(5);
//        final int min = cursor.getInt(6);
//        final int max = cursor.getInt(7);
//        final int step = cursor.getInt(8);
//        final boolean solved = getBooleanFromDatabase(cursor.getString(11));

        final String question = questionTXT.getEnunciado();
        final String answer = new JSONArray(questionTXT.getRespuestaCorrecta()).toString();
        final String options = new JSONArray(questionTXT.getRespuestas()).toString();
        final int min = 0;
        final int max = 0;
        final int step = 0;
        final boolean solved = false;
// JVG.E
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

    public static Quiz createQuizDueToTypeJson(JsonObject object, String type) {
//JVG.S
// "magic numbers" based on QuizTable#PROJECTION
//        final String type = cursor.getString(2);
//        final String question = cursor.getString(3);
//        final String answer = cursor.getString(4);
//        final String options = cursor.getString(5);
//        final int min = cursor.getInt(6);
//        final int max = cursor.getInt(7);
//        final int step = cursor.getInt(8);
//        final boolean solved = getBooleanFromDatabase(cursor.getString(11));

        final String question = object.get("mQuestion").getAsString();
        final String answer = object.get("mAnswer").getAsJsonArray().toString();

        final String options = object.get("mOptions").getAsJsonArray().toString();
        final int min = 0;
        final int max = 0;
        final int step = 0;
        final boolean solved = object.get("mSolved").getAsBoolean();
// JVG.E
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
        final String[][] optionsArrays = extractOptionsArrays(options);
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


    private static String[][] extractOptionsArrays(String options) {
        final String[] optionsLvlOne = JsonHelper.jsonArrayToStringArray(options);
        final String[][] optionsArray = new String[optionsLvlOne.length][];
        for (int i = 0; i < optionsLvlOne.length; i++) {
            optionsArray[i] = JsonHelper.jsonArrayToStringArray(optionsLvlOne[i]);
        }
        return optionsArray;
    }

    public List<com.trivial.upv.android.model.Category> mCategories;

    /**
     * Gets all categories with their quizzes.
     *
     * @param fromDatabase <code>true</code> if a data refresh is needed, else <code>false</code>.  @return All categories stored in the database.
     */
    public List<com.trivial.upv.android.model.Category> getCategories(boolean fromDatabase) {
        if (mCategories == null || fromDatabase) {
            mCategories = loadCategories();
        }
        return mCategories;
    }

    private List<com.trivial.upv.android.model.Category> loadCategories() {

        int i = 0;

        List<com.trivial.upv.android.model.Category> tmpCategories = new ArrayList<>();

        if (categoriesCurrent != null) {
            for (CategoryJSON categoryTXT : categoriesCurrent) {

                com.trivial.upv.android.model.Category category = getCategory(categoryTXT);

                category.setSolved(isSolvedCategory(category));

                tmpCategories.add(category);

            }
        }
        return tmpCategories;
    }

    private static boolean isSolvedCategory(com.trivial.upv.android.model.Category category) {
        int numSolvedQuizzes = 0;

        if (category.getQuizzes() != null) {
            for (Quiz quiz : category.getQuizzes()) {

                if (quiz.isSolved()) numSolvedQuizzes++;
            }
        }
        return numSolvedQuizzes > 0 && numSolvedQuizzes == category.getQuizzes().size();
    }


    /**
     * Gets a category from the given position of the cursor provided.
     */
    private static com.trivial.upv.android.model.Category getCategory(CategoryJSON categoryTXT) {
// "magic numbers" based on CategoryTable#PROJECTION
        final String id = categoryTXT.getCategory();
        final String name = categoryTXT.getCategory();
        final String themeName = categoryTXT.getTheme();
        final Theme theme = Theme.valueOf(themeName);
        final String isSolved = "false";
        final boolean solved = getBooleanFromDatabase(isSolved);
        final int[] scores = categoryTXT.getScore();
        final List<Quiz> quizzes = categoryTXT.getQuizzes();
        final String img = categoryTXT.getImg();

        return new com.trivial.upv.android.model.Category(name, id, theme, quizzes, scores, solved, img);
    }

    public static int[] createArrayIntFromNumQuizzes(CategoryJSON categoryTXT) {
        int numQuizzes = getNumQuizzesForCategory(categoryTXT);

        int[] tmpScores = new int[numQuizzes];

        for (int i = 0; i < tmpScores.length; i++) {
            tmpScores[i] = 0;
        }

        return tmpScores;
    }

    private static int getNumQuizzesForCategory(CategoryJSON categoryTXT) {
        int numQuizzes = 0;


        if (categoryTXT.getQuizzes() == null)
            for (CategoryJSON subcategoryTXT : categoryTXT.getSubcategories())
                numQuizzes += getNumQuizzesForCategory(subcategoryTXT);
        else return categoryTXT.getQuizzes().size();

        return numQuizzes;
    }

    private static boolean getBooleanFromDatabase(String isSolved) {
        // json stores booleans as true/false strings, whereas SQLite stores them as 0/1 values
        return null != isSolved && isSolved.length() == 1 && Integer.valueOf(isSolved) == 1;
    }

    /**
     * Looks for a category with a given id.
     *
     * @param categoryId Id of the category to look for.
     * @return The found category.
     */
    public com.trivial.upv.android.model.Category getCategoryWith(String categoryId) {
        com.trivial.upv.android.model.Category tmpCategory = null;
        for (com.trivial.upv.android.model.Category category : mCategories) {
            if (category.getId().equals(categoryId)) {
                tmpCategory = category;
                break;
            }
        }
        return tmpCategory;
    }

    public int getScore() {
        int tmpScore = 0;

        if (categories != null && isLoaded) {
            for (CategoryJSON category : categories) {
                if (category.getQuizzes() == null) {

                    tmpScore += getScore2(category.getSubcategories());

                } else {
                    if (category.getScore() != null) {
                        for (int score : category.getScore())
                            tmpScore += score;
                    }
                }
            }
        }
        return tmpScore;
    }

    private static int getScore2(List<CategoryJSON> subcategories) {
        int tmpScore = 0;

        if (subcategories != null) {
            for (CategoryJSON category : subcategories) {
                if (category.getQuizzes() == null) {

                    tmpScore += getScore2(category.getSubcategories());

                } else {
                    if (category.getScore() != null) {
                        for (int score : category.getScore())
                            tmpScore += score;
                    }
                }
            }
        }
        return tmpScore;
    }

    public boolean thereAreMorePreviusCategories() {
        if (categoriesPath == null || categoriesPath.size() == 0)
            return false;
        return true;
    }

    public void navigatePreviusCategory() {
        categoriesCurrent = categoriesPath.remove(categoriesPath.size() - 1);
    }

    public void navigateNextCategory(int position) {
        categoriesPath.add(categoriesCurrent);
        categoriesCurrent = categoriesCurrent.get(position).getSubcategories();

    }

    public boolean isSolvedCurrentCategory(int id) {
        if (categoriesCurrent != null) {
            int numQuizzes = getNumQuizzesForCategory(categoriesCurrent.get(id));
            int getNumSolvedByCategory = getScoreCategory(categoriesCurrent.get(id));

            return numQuizzes - getNumSolvedByCategory == 0;

        }
        return false;
    }


    public static int getScoreCategory(CategoryJSON category) {
        int tmpSolved = 0;

        if (category != null) {
            if (category.getSubcategories() != null) {
                for (CategoryJSON subcategory : category.getSubcategories()) {
                    if (subcategory.getQuizzes() == null) {
                        tmpSolved += getScoreCategory2(subcategory.getSubcategories());

                    } else {
                        if (subcategory.getQuizzes() != null) {
                            for (Quiz quiz : subcategory.getQuizzes()) {
                                if (quiz.isSolved())
                                    tmpSolved++;
                            }
                        }
                    }
                }
            } else {
                if (category.getQuizzes() != null) {
                    for (Quiz quiz : category.getQuizzes()) {
                        if (quiz.isSolved())
                            tmpSolved++;
                    }
                }
            }
        }
        return tmpSolved;
    }

    private static int getScoreCategory2(List<CategoryJSON> subcategories) {
        int tmpSolved = 0;
        if (subcategories != null) {
            for (CategoryJSON subsubcategory : subcategories) {
                if (subsubcategory.getQuizzes() == null) {
                    tmpSolved += getScoreCategory2(subsubcategory.getSubcategories());

                } else {
                    if (subsubcategory.getQuizzes() != null) {
                        for (Quiz quiz : subsubcategory.getQuizzes()) {
                            if (quiz.isSolved())
                                tmpSolved++;
                        }
                    }
                }
            }
        }
        return tmpSolved;
    }


    public void signOut(Context context) {
        resetData();

        mInstance = null;

        context.deleteFile("cache.json");

        mContext = null;


    }

    public void resetData() {
        isLoaded = false;

        if (categories != null) {
            categories.clear();
        }
        if (categoriesCurrent != null) {
            categoriesCurrent.clear();
        }
        if (categoriesPath != null) {
            categoriesPath.clear();
        }

        if (mCategories != null) {
            mCategories.clear();
        }

        categories = null;
        categoriesCurrent = null;
        categoriesPath = null;
        mCategories = null;
    }

    public void updateCategory() {
        Gson gson = new Gson();
        Type type = new TypeToken<List<CategoryJSON>>() {
        }.getType();

        String json = gson.toJson(categories, type);

        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(mContext.openFileOutput("cache.json", Context.MODE_PRIVATE)));
            try {
                bw.write(json);
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

//        Log.d("OBJECT", "TRAZA");
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public void setLoaded(boolean loaded) {
        isLoaded = loaded;
    }

    public void readCategoriesFromCache(String response, String[] preguntas) {
        Type type = new TypeToken<List<CategoryJSON>>() {
        }.getType();

        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Quiz.class, new QuizDeserializer());
        Gson gson = builder.create();

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(mContext.openFileInput("cache.json")));

            StringBuilder sb = new StringBuilder();

            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();

            resetData();

            categories = gson.fromJson(sb.toString(), type);
            categoriesCurrent = categories;
            categoriesPath = new ArrayList<>();
            isLoaded = true;

            sendBroadCastMessageRefresh(100);
            sendBroadCastMessage("OK");

            Log.d("CACHE", "CARGA OK");

        } catch (FileNotFoundException e) {
            Log.d("CACHE", "Error loading NOT FOUND");
            try {
                readNewFileJson(response, preguntas, false);
                Log.d("CACHE", "CACHE NOT FOUND");
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
                sendBroadCastError("CACHE", "Error loading");
            }

        } catch (IOException e) {
            e.printStackTrace();
            sendBroadCastError("CACHE", "Error loading");
        }


    }

    public static void cancelRequests() {
        VolleySingleton.getColaPeticiones().cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }
}
