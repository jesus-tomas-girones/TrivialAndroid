package com.trivial.upv.android.model.txtquiz;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.trivial.upv.android.R;
import com.trivial.upv.android.helper.singleton.VolleySingleton;
import com.trivial.upv.android.model.JsonAttributes;
import com.trivial.upv.android.model.json.CategoryJSON;
import com.trivial.upv.android.model.quiz.Quiz;
import com.trivial.upv.android.persistence.TopekaJSonHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.trivial.upv.android.persistence.TopekaJSonHelper.createArrayIntFromNumQuizzes;
import static com.trivial.upv.android.persistence.TopekaJSonHelper.sendBroadCastMessage;
import static com.trivial.upv.android.persistence.TopekaJSonHelper.sendBroadCastMessageRefresh;


/**
 * Created by jvg63 on 30/07/2017.
 */

public class QuestionsTXTHelper {

    public static String JsonURL = "https://trivialandroid-d2b33.firebaseio.com/.json";

    Context mContext;


    public QuestionsTXTHelper(Context signInActivity) {
        mContext = signInActivity;
    }

    /**
     * Dada la descarga de una url de texto plano(.txt), las adapta a una clase Quizz
     *
     * @param category
     * @param preguntasTXT
     * @throws UnsupportedEncodingException
     */
    public void getQuizzesFromString(CategoryJSON category, String preguntasTXT) throws UnsupportedEncodingException {
        String line;

        String utf8 = URLDecoder.decode(URLEncoder.encode(preguntasTXT, "iso8859-1"), "UTF-8");

        QuestionTXT questionTXT = null;
        QuestionsTXT questionsTXT = new QuestionsTXT();

        List<Quiz> quizzes = new ArrayList<>();

        String[] preguntas = utf8.split("\\r?\\n");
        int contador = 0;
        for (; contador < preguntas.length; contador++) {
            line = removeWordsUnWanted(preguntas[contador]);
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
                    } else {
                        questionTXT.getRespuestas().add(datos[0]);
                    }
                }
                // Enunciados
                else {
                    questionTXT.setEnunciado(line);
                }
            }
        }
        if (contador > 0 && questionTXT.getEnunciado() != null) {
            questionsTXT.getQuestions().add(questionTXT);
        }

        category.setDescription(questionsTXT.getSubject());
        category.setCategory(questionsTXT.getSubject());
        category.setMoreinfo(questionsTXT.getSubject());

        Quiz quizz;
        for (QuestionTXT question : questionsTXT.getQuestions()) {
            if (question.getRespuestaCorrecta().size() > 1) {
                quizz = TopekaJSonHelper.createQuizDueToType(question, JsonAttributes.QuizType.MULTI_SELECT);
            } else if (question.getRespuestas().size() == 4) {
                quizz = TopekaJSonHelper.createQuizDueToType(question, JsonAttributes.QuizType.FOUR_QUARTER);
            } else {
                quizz = TopekaJSonHelper.createQuizDueToType(question, JsonAttributes.QuizType.SINGLE_SELECT);
            }
            quizzes.add(quizz);
        }
        category.setQuizzes(quizzes);
        category.setScore(createArrayIntFromNumQuizzes(category));
    }

    private synchronized void addRequest() {
        pendingRequests++;
        maxPendingRequests++;
    }

    int pendingRequests = 0;
    int maxPendingRequests = 0;

    public void getQuizzesAsyncTask(CategoryJSON category, String url) {

        addRequest();

        EnviarMensajeEnServidorWebTask tarea = new EnviarMensajeEnServidorWebTask();
        tarea.url = url;
        tarea.category = category;
        tarea.execute();
    }


    private class EnviarMensajeEnServidorWebTask extends AsyncTask<Void, Void, String> {

        public String response = "ok";
        public String url;
        public CategoryJSON category;

        @Override
        public void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... arg0) {
            //for (int i = 0; i < mensaje.size() && "ok".equals(response); i++) {
            try {
                // Añadir parametros;
                Uri.Builder constructorParametros = new Uri.Builder();
                //.appendQueryParameter("mensaje", mensaje).
                //        appendQueryParameter("idapp", ID_PROYECTO).appendQueryParameter("apiKey", API_KEY);
                String parametros = constructorParametros.build().getEncodedQuery();

                URL direccion = new URL(url);
                HttpURLConnection conexion = (HttpURLConnection) direccion.openConnection();
                conexion.setRequestMethod("GET");
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

                    List<Quiz> quizzes = new ArrayList<>();

                    while ((line = in.readLine()) != null) {
                        responseData.append(line + "\n");
                    }

                    in.close();
                    conexion.disconnect();

                    String[] preguntas = responseData.toString().split("\\n");

                    int contador = 0;

                    for (; contador < preguntas.length; contador++) {
                        line = removeWordsUnWanted(preguntas[contador]);
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
                                } else {
                                    questionTXT.getRespuestas().add(datos[0]);
                                }
                            }
                            // Enunciados
                            else {
                                questionTXT.setEnunciado(line);
                            }
                        }

                    }
                    if (contador > 0 && questionTXT.getEnunciado() != null)
                        questionsTXT.getQuestions().add(questionTXT);


                    category.setDescription(questionsTXT.getSubject());
                    category.setCategory(questionsTXT.getSubject());
                    category.setMoreinfo(questionsTXT.getSubject());

                    Quiz quizz;
                    for (QuestionTXT question : questionsTXT.getQuestions()) {
                        if (question.getRespuestaCorrecta().size() > 1) {
                            quizz = TopekaJSonHelper.createQuizDueToType(question, JsonAttributes.QuizType.MULTI_SELECT);
                        } else if (question.getRespuestas().size() == 4) {
                            quizz = TopekaJSonHelper.createQuizDueToType(question, JsonAttributes.QuizType.FOUR_QUARTER);
                        } else {
                            quizz = TopekaJSonHelper.createQuizDueToType(question, JsonAttributes.QuizType.SINGLE_SELECT);
                        }

                        quizzes.add(quizz);
                    }
                    category.setQuizzes(quizzes);

                } else {
                    response = "error";
                }
            } catch (IOException e) {
                response = "error";
            }
            return response;
        }

        public void onPostExecute(String res) {
            if (res == "ok") {
                updateProgress();
            }
        }
    }

    private String removeWordsUnWanted(String line) {

        String lineTmp = line.replaceAll("<code>", "\"");
        lineTmp = lineTmp.replaceAll("</code>", "\"");
        lineTmp = lineTmp.replaceAll("<br/>", "\n");
        lineTmp = lineTmp.replaceAll("<br/ >", "\n");
        lineTmp = lineTmp.replaceAll("&nbsp;", " ");
        return lineTmp;
    }

    private void updateProgress() {

        removeRequest();
        synchronized (this) {
            if (pendingRequests == 0) {
                Log.d("CARGA", "CARGA_FINALIZADA");

                new Thread() {
                    public void run() {
                        TopekaJSonHelper.getInstance(mContext, false).updateCategory();
                        TopekaJSonHelper.getInstance(mContext, false).setLoaded(true);
                        sendBroadCastMessageRefresh(100);
                        sendBroadCastMessage("OK");
                    }
                }.start();
            }
//        Log.d("CARGA", "PENDING:" + pendingRequests);
            else {
                sendBroadCastMessageRefresh((int) ((float) (maxPendingRequests - pendingRequests) / (float) maxPendingRequests * 100f));
            }
        }

    }

    private synchronized void removeRequest() {
        pendingRequests--;
    }


    public static boolean DEBUG = false;

    /**
     * Partiendo de un fichero JSON generar la estructura recursiva de Categorias, Subcategorias, Sub-Subcategorias, ...
     * hasta llegar al módulo raíz con los Quizzes
     *
     * @param mJSON
     * @throws IOException
     * @throws JSONException
     */
    public List<CategoryJSON> readCategoriesFromJSON(JSONObject mJSON) throws IOException, JSONException, URISyntaxException {

        List<CategoryJSON> mCategories = new ArrayList<>();
        JSONObject root;

        if (DEBUG) {
            String line;
            StringBuilder categoriesJson = new StringBuilder();
            InputStream rawCategories = mContext.getResources().openRawResource(R.raw.categories_upv);
            BufferedReader reader = new BufferedReader(new InputStreamReader(rawCategories));

            // Crear una cadena con el Fichero JSON completo
            while ((line = reader.readLine()) != null) {
                categoriesJson.append(line);
            }
            rawCategories.close();
            reader.close();
            // Recorremos el JSON
            String categoriesStr = categoriesJson.toString();
            // Raiz principal
            root = new JSONObject(categoriesStr);

        } else {
            root = mJSON;
        }

        // Objeto categories
        JSONObject categories = root.getJSONObject("categories");

        // Genera tantas categorías como keys distintos tiene el JSON
        JSONObject category;
        CategoryJSON mCategory;
        Iterator<String> keys = categories.keys();
        while (keys.hasNext()) {
            // Note that "key" must be "1", "2", "3"...
            String key = keys.next();

            mCategory = new CategoryJSON();
            category = (JSONObject) categories.get(key);

            //mCategory.setId(category.getString("id"));
            mCategory.setCategory(category.getString("category"));
            mCategory.setAccess(category.getLong("access"));
            mCategory.setSuccess(category.getLong("success"));
            mCategory.setDescription(category.getString("description"));
            mCategory.setImg(category.getString("img"));
            mCategory.setMoreinfo(category.getString("moreinfo"));
            mCategory.setTheme(category.getString("theme"));

            // Genera las subcategorías recursivamente
            if (category.has("subcategories")) {
                mCategory.setSubcategories(asignaSubtemas(category.getJSONObject("subcategories")));
            } else {
                mCategory.setSubcategories(null);
            }

            // Una categoría principal no debería tener Quizzes
            if (category.has("quizzes")) {
                // Quizzes
                JSONArray preguntasJSon = category.getJSONArray("quizzes");

                List<String> quizzies = new ArrayList<>();

                List<CategoryJSON> sub_subcategories = new ArrayList<>();
                CategoryJSON sub_subcategory = null;

                for (int j = 0; j < preguntasJSon.length(); j++) {
                    sub_subcategory = new CategoryJSON();
                    //sub_subcategory.setCategory(subcategory.getString("category"));
                    sub_subcategory.setAccess(category.getLong("access"));
                    sub_subcategory.setSuccess(category.getLong("success"));
                    //sub_subcategory.setDescription(subcategory.getString("description"));
                    sub_subcategory.setImg(category.getString("img"));
                    //sub_subcategory.setMoreinfo(subcategory.getString("moreinfo"));
                    sub_subcategory.setTheme(category.getString("theme"));
                    // Añade manualmente las subtcategorias de las preguntas

                    quizzies.add((String) preguntasJSon.get(j));

                    sub_subcategories.add(sub_subcategory);

                    getQuizzesTXTFromInternetVolley(sub_subcategory, (String) preguntasJSon.get(j));
//                    localizaPreguntasTXT(sub_subcategory, (String) preguntasJSon.get(j));
                }
                mCategory.setSubcategories(sub_subcategories);
                // Los QUIZZIES se asignan mediente peticiones asíncronas
                ///mCategory.setQuizzes(quizzies);
            } else {
                mCategory.setQuizzes(null);
            }
            mCategories.add(mCategory);
        }
        return mCategories;
    }

    private List<CategoryJSON> asignaSubtemas(JSONObject subcategorias) throws JSONException, MalformedURLException, URISyntaxException {

        JSONObject subcategory;
        List<CategoryJSON> preguntas = new ArrayList<>();
        CategoryJSON pregunta = null;

        Iterator<String> keys = subcategorias.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            pregunta = new CategoryJSON();

            subcategory = (JSONObject) subcategorias.get(key);

            /// pregunta.setId(subcategory.getString("id"));
            pregunta.setCategory(subcategory.getString("category"));
            pregunta.setAccess(subcategory.getLong("access"));
            pregunta.setSuccess(subcategory.getLong("success"));
            pregunta.setDescription(subcategory.getString("description"));
            pregunta.setImg(subcategory.getString("img"));
            pregunta.setMoreinfo(subcategory.getString("moreinfo"));
            pregunta.setTheme(subcategory.getString("theme"));

            if (subcategory.has("subcategories")) {
                pregunta.setQuizzes(null);
                pregunta.setSubcategories(asignaSubtemas(subcategory.getJSONObject("subcategories")));
            } else {
                pregunta.setSubcategories(null);

                if (subcategory.has("quizzes")) {
                    // Quizzes
                    JSONArray preguntasJSon = subcategory.getJSONArray("quizzes");

                    List<CategoryJSON> sub_subcategories = new ArrayList<>();
                    CategoryJSON sub_subcategory = null;

                    for (int j = 0; j < preguntasJSon.length(); j++) {

                        sub_subcategory = new CategoryJSON();
                        //sub_subcategory.setCategory(subcategory.getString("category"));
                        sub_subcategory.setAccess(subcategory.getLong("access"));
                        sub_subcategory.setSuccess(subcategory.getLong("success"));
                        //sub_subcategory.setDescription(subcategory.getString("description"));
                        sub_subcategory.setImg(subcategory.getString("img"));
                        //sub_subcategory.setMoreinfo(subcategory.getString("moreinfo"));
                        sub_subcategory.setTheme(subcategory.getString("theme"));
                        // Añade manualmente las subtcategorias de las preguntas

                        sub_subcategories.add(sub_subcategory);

                        getQuizzesTXTFromInternetVolley(sub_subcategory, (String) preguntasJSon.get(j));
//                        localizaPreguntasTXT(sub_subcategory, (String) preguntasJSon.get(j));
                    }
                    pregunta.setSubcategories(sub_subcategories);
                    // Los QUIZZIES se asignan mediente peticiones asíncronas
                } else {
                    pregunta.setQuizzes(null);
                }
            }
            preguntas.add(pregunta);
        }
        return preguntas;
    }

    private void localizaPreguntasTXT(CategoryJSON sub_subcategory, String url) {
        getQuizzesAsyncTask(sub_subcategory, url.replace(" ", "%20"));
    }

    /**
     * Get Quizzes from a plain text in Internet
     *
     * @param sub_subcategory
     * @param urlStr
     */
    private void getQuizzesTXTFromInternetVolley(final CategoryJSON sub_subcategory, final String urlStr) throws MalformedURLException, URISyntaxException {
//        URL url = new URL(urlStr);
//        URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
//        String urlVolley = uri.toASCIIString();


        StringRequest request = new StringRequest(Request.Method.GET, urlStr.replace(" ", "%20"), new Response.Listener<String>() {
            @Override
            public void onResponse(final String response) {

                new Thread() {
                    public void run() {

                        try {
                            getQuizzesFromString(sub_subcategory, response);
                            updateProgress();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            sendBroadCastMessage("ERROR");
                        }

                    }
                }.start();
            }
        }, new Response.ErrorListener()

        {
            @Override
            public void onErrorResponse(VolleyError error) {
                TopekaJSonHelper.getInstance(mContext, false).sendBroadCastError("Volley", "Loading quizzes!");

                TopekaJSonHelper.cancelRequests();
            }
        });

        VolleySingleton.getColaPeticiones().add(request);

        addRequest();
    }

}