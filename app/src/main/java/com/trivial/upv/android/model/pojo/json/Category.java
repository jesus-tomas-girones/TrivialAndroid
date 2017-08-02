
package com.trivial.upv.android.model.pojo.json;

import android.util.Log;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.trivial.upv.android.model.quiz.Quiz;

import java.util.List;

public class Category {

    /*@SerializedName("id")
    @Expose
    private String id;
    @SerializedName("category")*/
    @Expose
    private String category;
    @SerializedName("access")
    @Expose
    private Long access;
    @SerializedName("success")
    @Expose
    private Long success;
    @SerializedName("description")
    @Expose
    private String description;
    @SerializedName("theme")
    @Expose
    private String theme;
    @SerializedName("img")
    @Expose
    private String img;
    @SerializedName("moreinfo")
    @Expose
    private String moreinfo;
    @SerializedName("subcategories")
    @Expose
    private List<Category> subcategories = null;
    /*@SerializedName("quizzes")
    @Expose
    private List<String> quizzes = null;*/
    private List<Quiz> quizzes = null;


    /*public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }*/

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getAccess() {
        return access;
    }

    public void setAccess(Long access) {
        this.access = access;
    }

    public Long getSuccess() {
        return success;
    }

    public void setSuccess(Long success) {
        this.success = success;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public String getMoreinfo() {
        return moreinfo;
    }

    public void setMoreinfo(String moreinfo) {
        this.moreinfo = moreinfo;
    }

    public List<Category> getSubcategories() {
        return subcategories;
    }

    public void setSubcategories(List<Category> subcategories) {
        this.subcategories = subcategories;
    }

    /*public List<String> getQuizzes() {
        return quizzes;
    }

    public void setQuizzes(List<String> quizzes) {
        this.quizzes = quizzes;
    }*/

    public List<Quiz> getQuizzes() {
        return quizzes;
    }

    public void setQuizzes(List<Quiz> quizzes) {
        this.quizzes = quizzes;
    }

}