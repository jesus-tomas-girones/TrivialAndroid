package com.trivial.upv.android.model.pojo.preguntastxt;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jvg63 on 30/07/2017.
 */

class QuestionsTXT {
    private String subject;
    private List<QuestionTXT> questions;

    public QuestionsTXT() {
        questions = new ArrayList<>();
    }

    public List<QuestionTXT> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionTXT> questions) {
        this.questions = questions;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

}

