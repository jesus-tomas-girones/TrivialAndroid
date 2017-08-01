package com.trivial.upv.android.model.pojo;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.trivial.upv.android.activity.SignInActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jvg63 on 30/07/2017.
 */

public class QuestionsTXTHelper {
    private Category mCategory;
    private Context context;

    public List<QuestionsTXT> getQuestions() {
        return mQuestions;
    }

    private List<QuestionsTXT> mQuestions = new ArrayList<>();

    public QuestionsTXTHelper(Context context, Category category) {
        this.mCategory = category;
        this.context = context;
        mQuestions = new ArrayList<>();
    }

    public void obtienePreguntasAleatorias() {


        EnviarMensajeEnServidorWebTask tarea = new EnviarMensajeEnServidorWebTask();
        tarea.contexto = context;
        tarea.mensaje = mCategory.getQuizzes();
        tarea.execute();
    }

    private class EnviarMensajeEnServidorWebTask extends AsyncTask<Void, Void, String> {
        String response = "ok";
        Context contexto;
        List<String> mensaje;

        @Override
        public void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... arg0) {
            for (int i = 0; i < mensaje.size() && "ok".equals(response); i++) {
                try {
                    // Añadir parametros;
                    Uri.Builder constructorParametros = new Uri.Builder();
                    //.appendQueryParameter("mensaje", mensaje).
                    //        appendQueryParameter("idapp", ID_PROYECTO).appendQueryParameter("apiKey", API_KEY);
                    String parametros = constructorParametros.build().getEncodedQuery();
                    String url = mensaje.get(i);
                    Log.d("FICHERO:", url);
                    URL direccion = new URL(url);
                    HttpURLConnection conexion = (HttpURLConnection) direccion.openConnection();
                    conexion.setRequestMethod("POST");
                    conexion.setRequestProperty("Accept-Language", "UTF-8");
                    conexion.setDoOutput(false);
                    /*OutputStreamWriter outputStreamWriter = new OutputStreamWriter(conexion.getOutputStream());
                    outputStreamWriter.write("");
                    outputStreamWriter.flush();*/
                    int respuesta = conexion.getResponseCode();
                    if (respuesta == 200) {
                        response = "ok";

                        BufferedReader in = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
                        String line = null;

                        StringBuilder responseData = new StringBuilder();


                        QuestionTXT questionTXT = null;
                        QuestionsTXT questionsTXT = new QuestionsTXT();


                        int contador = 0;
                        while ((line = in.readLine()) != null) {
                            // Primera linea: Temática
                            if (contador == 0) {
                                questionTXT = new QuestionTXT();
                                questionsTXT.setSubject(line);
                            }
                            // Si linea en blanco. Nueva pregunta
                            else if (line.isEmpty()) {
                                questionsTXT.getQuestions().add(questionTXT);
                                questionTXT = new QuestionTXT();
                            } else {

                                // Respuestas
                                if (questionTXT.getEnunciado() != null) {
                                    String[] datos = line.split("#");
                                    // Hay Comentario
                                    if (datos.length > 1) {
                                        questionTXT.getComentariosRespuesta().add(datos[1]);
                                    } else {
                                        questionTXT.getComentariosRespuesta().add("");
                                    }

                                    if (datos[0].charAt(0) == '*') {
                                        questionTXT.getRespuestaCorrecta().add(questionTXT.getRespuestas().size());
                                        questionTXT.getRespuestas().add(datos[0].substring(1));
                                    }
                                    else {
                                        questionTXT.getRespuestas().add(datos[0]);
                                    }
                                }
                                // Enunciados
                                else {
                                    questionTXT.setEnunciado(line);
                                }
                            }

                            responseData.append(line);
                            Log.d("TRZA_FICHERO_URL", line);
                            contador++;
                        }

                        if (contador > 0)
                            questionsTXT.getQuestions().add(questionTXT);

                        in.close();
                        conexion.disconnect();

                        mQuestions.add(questionsTXT);
                    } else {
                        response = "error";
                    }
                } catch (IOException e) {
                    response = "error";
                }
            }
            return response;
        }

        public void onPostExecute(String res) {
            if (res == "ok") {

                Toast.makeText(contexto, "Mensaje Enviado Correctamente!", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
