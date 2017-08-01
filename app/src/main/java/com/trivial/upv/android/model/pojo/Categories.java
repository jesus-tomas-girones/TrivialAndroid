
package com.trivial.upv.android.model.pojo;

import android.content.Context;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.trivial.upv.android.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Categories {

    public List<Category> getCategories() {
        return mCategories;
    }

    public void setCategories(List<Category> mCategories) {
        this.mCategories = mCategories;
    }

    @SerializedName("categories")
    @Expose
    private List<Category> mCategories = null;

    public void readCategoriesFromJSON(Context mContext) throws IOException, JSONException {

        mCategories = new ArrayList<>();

        StringBuilder categoriesJson = new StringBuilder();
        InputStream rawCategories = mContext.getResources().openRawResource(R.raw.categories_upv);
        BufferedReader reader = new BufferedReader(new InputStreamReader(rawCategories));
        String line;


        while ((line = reader.readLine()) != null) {
            categoriesJson.append(line);
        }

        rawCategories.close();
        reader.close();
        // Recorremos el JSON
        String categoriesStr = categoriesJson.toString();
        // Raiz principal
        JSONObject root = new JSONObject(categoriesStr);
        // Objeto mCategories
        JSONArray categories = root.getJSONArray("categories");

        // Listado de mCategories
        JSONObject category;
        Category mCategory;
        for (int i = 0; i < categories.length(); i++) {
            mCategory = new Category();
            category = categories.getJSONObject(i);

            mCategory.setId(category.getString("id"));
            mCategory.setCategory(category.getString("category"));
            mCategory.setAccess(category.getLong("access"));
            mCategory.setSuccess(category.getLong("success"));
            mCategory.setDescription(category.getString("description"));
            mCategory.setImg(category.getString("img"));
            mCategory.setMoreinfo(category.getString("moreinfo"));
            mCategory.setTheme(category.getString("theme"));

            if (category.has("subcategories")) {
                mCategory.setSubcategories(asignaSubtemas(category.getJSONArray("subcategories")));
            } else {
                mCategory.setSubcategories(null);
            }

            mCategory.setQuizzes(null);

            this.mCategories.add(mCategory);
        }
    }

    private List<Category> asignaSubtemas(JSONArray subcategorias) throws JSONException {

        JSONObject subcategory;

        List<Category> preguntas = new ArrayList<>();

        Category pregunta = null;
        for (int i = 0; i < subcategorias.length(); i++) {
            pregunta = new Category();

            subcategory = subcategorias.getJSONObject(i);

            pregunta.setId(subcategory.getString("id"));
            pregunta.setCategory(subcategory.getString("category"));
            pregunta.setAccess(subcategory.getLong("access"));
            pregunta.setSuccess(subcategory.getLong("success"));
            pregunta.setDescription(subcategory.getString("description"));
            pregunta.setImg(subcategory.getString("img"));
            pregunta.setMoreinfo(subcategory.getString("moreinfo"));
            pregunta.setTheme(subcategory.getString("theme"));

            if (subcategory.has("subcategories")) {
                pregunta.setQuizzes(null);
                pregunta.setSubcategories(asignaSubtemas(subcategory.getJSONArray("subcategories")));
            } else {
                pregunta.setSubcategories(null);

                if (subcategory.has("quizzes")) {
                    // Quizzes
                    JSONArray preguntarJSon = subcategory.getJSONArray("quizzes");

                    List<String> quizzies = new ArrayList<>();
                    for (int j = 0; j < preguntarJSon.length(); j++) {
                        quizzies.add((String) preguntarJSon.get(j));
                    }

                    pregunta.setQuizzes(quizzies);
                } else {
                    pregunta.setQuizzes(null);
                }
            }

            preguntas.add(pregunta);

        }
        return preguntas;
    }
}
