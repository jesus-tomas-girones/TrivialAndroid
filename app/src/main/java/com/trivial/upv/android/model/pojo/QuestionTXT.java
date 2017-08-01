package com.trivial.upv.android.model.pojo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jvg63 on 30/07/2017.
 */

class QuestionTXT {
    private String enunciado;
    private List<String> respuestas;
    private List<Integer> respuestaCorrecta;
    private List<String> comentariosRespuesta;

    public QuestionTXT() {
        respuestas = new ArrayList<>();
        comentariosRespuesta = new ArrayList<>();
        respuestaCorrecta = new ArrayList<>();
    }

    public String getEnunciado() {
        return enunciado;
    }

    public void setEnunciado(String enunciado) {
        this.enunciado = enunciado;
    }

    public List<String> getRespuestas() {
        return respuestas;
    }

    public void setRespuestas(List<String> respuestas) {
        this.respuestas = respuestas;
    }

    public List<Integer> getRespuestaCorrecta() {
        return respuestaCorrecta;
    }

    public void setRespuestaCorrecta(List<Integer> respuestaCorrecta) {
        this.respuestaCorrecta = respuestaCorrecta;
    }

    public List<String> getComentariosRespuesta() {
        return comentariosRespuesta;
    }

    public void setComentariosRespuesta(List<String> comentariosRespuesta) {
        this.comentariosRespuesta = comentariosRespuesta;
    }
}

