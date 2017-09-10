
package com.trivial.upv.android.model.json;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.trivial.upv.android.model.quiz.Quiz;

import java.util.List;

public class CategoryJSON {

    /*@SerializedName("id")
    @Expose
    private String id;
    */
    @SerializedName("category")
    @Expose
    private String category;

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

    @SerializedName("video")
    @Expose
    private String video = null;


    @SerializedName("subcategories")
    @Expose
    private List<CategoryJSON> subcategories = null;
    /*@SerializedName("quizzes")
    @Expose
    private List<String> quizzes = null;*/

    @SerializedName("quizzes")
    @Expose
    private List<Quiz> quizzes = null;

    @SerializedName("score")
    @Expose
    private int[] score = null;

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

    public List<CategoryJSON> getSubcategories() {
        return subcategories;
    }

    public void setSubcategories(List<CategoryJSON> subcategories) {
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

    public int[] getScore() {
        return score;
    }

    public void setScore(int[] score) {
        this.score = score;
    }

    public String getVideo() {
        return video;
    }

    public void setVideo(String video) {
        this.video = video;
    }
}