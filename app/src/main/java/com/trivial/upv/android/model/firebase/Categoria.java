
package com.trivial.upv.android.model.firebase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Categoria {

    @SerializedName("accesos")
    @Expose
    private Long accesos;
    @SerializedName("aciertos")
    @Expose
    private Long aciertos;
    @SerializedName("descripcion")
    @Expose
    private String descripcion;
    @SerializedName("img")
    @Expose
    private String img;
    @SerializedName("masinfo")
    @Expose
    private String masinfo;
    @SerializedName("subtemas")
    @Expose
    public HashMap<String,Subtema> subtemas;
    @SerializedName("tema")
    @Expose
    private String tema;

    public Long getAccesos() {
        return accesos;
    }

    public void setAccesos(Long accesos) {
        this.accesos = accesos;
    }

    public Long getAciertos() {
        return aciertos;
    }

    public void setAciertos(Long aciertos) {
        this.aciertos = aciertos;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public String getMasinfo() {
        return masinfo;
    }

    public void setMasinfo(String masinfo) {
        this.masinfo = masinfo;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public Categoria() {}
    public HashMap<String, Subtema> getSubtemas() {
        return subtemas;
    }

    public void setSubtemas(HashMap<String, Subtema> subtemas) {
        this.subtemas = subtemas;
    }

    // Obtiene todos los links de preguntas
    public List<String> getListadoPreguntas() {

        List<String> listadPreguntas = new ArrayList<>();

        for (Map.Entry<String, Subtema> subtema: subtemas.entrySet()) {
            String key = subtema.getKey();
            Subtema value = subtema.getValue();

            listadPreguntas.addAll(value.getPreguntas());
        }
        return listadPreguntas;
    }

}
