
package com.trivial.upv.android.model.pojo.json;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.trivial.upv.android.model.pojo.preguntastxt.QuestionsTXTHelper;

import java.util.List;

public class Categories {

    public List<Category> getCategories() {
        return mCategories;
    }

    public Categories() {

    }

    public void setCategories(List<Category> mCategories) {
        this.mCategories = mCategories;
    }

    @SerializedName("categories")
    @Expose
    private List<Category> mCategories = null;

}
