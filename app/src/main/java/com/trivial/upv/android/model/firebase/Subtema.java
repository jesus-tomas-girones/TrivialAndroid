
package com.trivial.upv.android.model.firebase;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Subtema {

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
    @SerializedName("preguntas")
    @Expose
    private List<String> preguntas = null;
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

    public List<String> getPreguntas() {
        return preguntas;
    }

    public void setPreguntas(List<String> preguntas) {
        this.preguntas = preguntas;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public Subtema() {}
}
