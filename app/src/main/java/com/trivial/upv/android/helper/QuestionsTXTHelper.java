package com.trivial.upv.android.helper;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.trivial.upv.android.R;
import com.trivial.upv.android.helper.singleton.volley.StringRequestHeaders;
import com.trivial.upv.android.helper.singleton.volley.VolleySingleton;
import com.trivial.upv.android.model.JsonAttributes;
import com.trivial.upv.android.model.json.CategoryJSON;
import com.trivial.upv.android.model.quiz.Quiz;
import com.trivial.upv.android.model.txtquiz.QuestionTXT;
import com.trivial.upv.android.model.txtquiz.QuestionsTXT;
import com.trivial.upv.android.persistence.TrivialJSonHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.trivial.upv.android.persistence.TrivialJSonHelper.createArrayIntFromNumQuizzes;


/**
 * Created by jvg63 on 30/07/2017.
 */

public class QuestionsTXTHelper {

    public static String DEBUG_STR = "LOAD_JSON";
    //    public static String JsonURL = "https://trivialandroid-d2b33.firebaseio.com/.json";
//    public static String JsonURL = "http://eventosjvg.esy.es/categories_upv.json";
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
    public void getQuizzesFromString(CategoryJSON category, String preguntasTXT, String url) throws UnsupportedEncodingException {
        String line;

        QuestionTXT questionTXT = null;
        QuestionsTXT questionsTXT = new QuestionsTXT();
        List<Quiz> quizzes = new ArrayList<>();
        boolean firsTime = true;

        String tmpPreguntasTXT = preguntasTXT.replaceAll("(\\r?\\n\\r?\\n)(\\r?\\n)*", "$1");

        String[] preguntas = tmpPreguntasTXT.split("\\r?\\n");
        int contador = 0;

        for (; contador < preguntas.length; contador++) {
            line = removeWordsUnWanted(preguntas[contador], url); // Primera linea: Temática
            // Primea Línea del fichero

            if (contador == 0) {
                questionTXT = new QuestionTXT();
                questionsTXT.setSubject(line);
            }// Detect if after the title of the quizz there are blank lines
            else if (contador > 0 && line.isEmpty() && firsTime) {
                continue;
            }
            // Si linea en blanco. Nueva pregunta
            else if (line.isEmpty()) {
                questionsTXT.getQuestions().add(questionTXT);
                questionTXT = new QuestionTXT();

            } else {
                // Respuestas
                if (firsTime) {
                    firsTime = false;
                }

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

        if (category.getDescription() == null)
            category.setDescription(questionsTXT.getSubject());

        if (category.getCategory() == null)
            category.setCategory(questionsTXT.getSubject());

//        if (category.getMoreinfo() == null)
//            category.setMoreinfo(null);

        Quiz quizz;
        for (QuestionTXT question : questionsTXT.getQuestions()) {
            if (question.getRespuestaCorrecta().size() > 1) {
                quizz = TrivialJSonHelper.createQuizDueToType(question, JsonAttributes.QuizType.MULTI_SELECT);
            } else if (question.getRespuestas().size() == 4) {
                quizz = TrivialJSonHelper.createQuizDueToType(question, JsonAttributes.QuizType.FOUR_QUARTER);
            } else {
                quizz = TrivialJSonHelper.createQuizDueToType(question, JsonAttributes.QuizType.SINGLE_SELECT);
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

    // change < and > for &lt; and &gt where there aren't a tag HTML
    private String substHtmlCharacters(String str) {
        StringBuilder sBuilder = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch == '>' && i != 0) {
                char c = str.charAt(i - 1);
                if (Character.isWhitespace(c) || !Character.isLetter(c)) {
                    sBuilder.append("&gt;");
                } else
                    sBuilder.append(ch);
            } else if (ch == '>' && i == 0) {
                sBuilder.append("&gt;");
            } else if (ch == '<' && i < str.length() - 1) {
                char c = str.charAt(i + 1);
                if (!(c == '/' || Character.isLetter(c))) {
                    sBuilder.append("&lt;");
                } else
                    sBuilder.append(ch);
            } else if (ch == '<' && i == str.length() - 1) {
                sBuilder.append("&lt;");
            } else {
                sBuilder.append(ch);
            }
        }
        return sBuilder.toString();
    }

    // Process de strings and remove 2 continuous \n. Also change < and > for &lt; and &gt where there aren't a tag HTML
    private String removeWordsUnWanted(String line, String url) {
        String lineTmp = line.replaceAll("<br><br>", "<br>");
        lineTmp = lineTmp.replaceAll("</br>", "<br>");
        lineTmp = lineTmp.replaceAll("<br/>", "<br>");
        lineTmp = substHtmlCharacters(lineTmp);

        lineTmp = addPathToUrlImg(lineTmp, url);

        lineTmp = lineTmp.replaceAll("<code>", "<tt>");
        lineTmp = lineTmp.replaceAll("</code>", "</tt>");

        return lineTmp;
    }

    private String addPathToUrlImg(String input, String url) {

        String regex = "(<img\\s+src=[\"'])([^\"']+)";

        String replace = "$1" + url.substring(0, url.lastIndexOf("/") + 1) + "$2";

        Pattern p = Pattern.compile(regex);

        // get a matcher object
        Matcher m = p.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (m.find()) {
            if (!input.substring(m.start(), m.end()).contains("http"))
                m.appendReplacement(sb, replace);
        }

        m.appendTail(sb);

        return sb.toString();
    }


    // Auxiliary functions to remove TAGS
    private static final Pattern REMOVE_TAGS = Pattern.compile("<.+?>");

    public static String removeTags(String string) {
        if (string == null || string.length() == 0) {
            return string;
        }

        Matcher m = REMOVE_TAGS.matcher(string);
        return m.replaceAll("");
    }

    private void updateProgress() {

        synchronized (this) {
            removeRequest();

            if (pendingRequests == 0) {
//                Log.d("CARGA", "CARGA_FINALIZADA");

                new Thread() {
                    public void run() {
                        TrivialJSonHelper.getInstance(mContext, false).updateCategory();
                        TrivialJSonHelper.getInstance(mContext, false).setLoaded(true);
                        TrivialJSonHelper.getInstance(mContext, false).sendBroadCastMessageRefresh(100);
                        TrivialJSonHelper.getInstance(mContext, false).sendBroadCastMessage("OK");
                    }
                }.start();
            }
//        Log.d("CARGA", "PENDING:" + pendingRequests);
            else {

                int avance = (maxPendingRequests - pendingRequests) * 100 / maxPendingRequests;
                if (avance % 5 == 0 && avance > 0)
                    TrivialJSonHelper.getInstance(mContext, false).sendBroadCastMessageRefresh((int) ((float) (maxPendingRequests - pendingRequests) / (float) maxPendingRequests * 100f));
            }
        }

    }

    private synchronized void removeRequest() {
        pendingRequests--;
    }


    public static boolean DEBUG = false;
    public static boolean LOAD_LOCAL_FILE = false;

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

        if (LOAD_LOCAL_FILE) {
            String line;
            StringBuilder categoriesJson = new StringBuilder();
            InputStream rawCategories = mContext.getResources().openRawResource(R.raw.trivialandroid);
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
        writeLog("NumCategories: " + (categories != null ? categories.length() : "null"));

        // Genera tantas categorías como keys distintos tiene el JSON
        if (categories != null) {
            writeLog("Reading Categories...");
            mCategories = asignaSubtemas(categories, null);
        } else {
            writeLog("Categories not detected!");
        }

        return mCategories;
    }

    public static void writeLog(String msg) {
        if (DEBUG) {
            Log.d(DEBUG_STR, msg);
        }
    }

    public static void writeLogObject(JSONObject object, String msg) {
        try {
            if (object.has(msg)) {
                writeLog(msg + ": " + object.getString(msg));
            } else {
                writeLog("ERROR GETTING VALUE FOR: " + msg);
            }

        } catch (JSONException e) {
            writeLog("ERROR PARSING OBJECT");
        }

    }

    private List<CategoryJSON> asignaSubtemas(JSONObject subcategorias, JSONObject parent) throws
            JSONException, MalformedURLException, URISyntaxException {

        JSONObject subcategory;
        List<CategoryJSON> preguntas = new ArrayList<>();
        CategoryJSON pregunta = null;

        Iterator<String> keys = subcategorias.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            writeLog("Iniciando Carga Categora: " + key);
            writeLog("------------------------------------------------------------");
            pregunta = new CategoryJSON();
            subcategory = (JSONObject) subcategorias.get(key);
            // ¿Es un nodo hoja?
            if (subcategory.has("subcategories") || subcategory.has("quizzes")) {
                writeLogObject(subcategory, "category");
                pregunta.setCategory(subcategory.getString("category"));
                writeLogObject(subcategory, "description");
                pregunta.setDescription(subcategory.getString("description"));

                writeLogObject(subcategory, "img");
                pregunta.setImg(subcategory.getString("img"));
                writeLogObject(subcategory, "moreinfo");
                pregunta.setMoreinfo(subcategory.getString("moreinfo"));
                writeLogObject(subcategory, "theme");
                pregunta.setTheme(subcategory.getString("theme"));
                writeLogObject(subcategory, "video");
                if (subcategory.has("video")) {
                    pregunta.setVideo(subcategory.getString("video"));
                }

                if (subcategory.has("subcategories")) {
                    writeLog("Categoría tiene subcategorías!");
                    pregunta.setQuizzes(null);

                    Iterator<String> names = subcategorias.keys();
                    String tmpParent = names.next();

                    pregunta.setSubcategories(asignaSubtemas(subcategory.getJSONObject("subcategories"), subcategorias.getJSONObject(tmpParent)));
                } else {
                    writeLog("Categoría tiene quizzes!");
                    if (subcategory.has("quizzes")) {
                        // Quizzes
                        pregunta.setSubcategories(null);

                        JSONArray preguntasJSon = subcategory.getJSONArray("quizzes");

                        List<CategoryJSON> sub_subcategories = new ArrayList<>();
                        CategoryJSON sub_subcategory = null;

                        for (int j = 0; j < preguntasJSon.length(); j++) {
                            sub_subcategory = new CategoryJSON();
                            //sub_subcategory.setCategory(subcategory.getString("category"));
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
                    }
                }
            } else {
                // Create new category with quizzes
                // Quizzes
                //- Campo “video” en objeto Category
                //- Cuando una categoría tienen el campo “quiz” en lugar de “quizzes”, se crea un objeto Category con los campos que se indiquen.
                //                             Si no aparece “img”, “theme” los hereda de la categoría.
                //                             Si no aparece “category” el título lo coge del fichero txt  (indicado en “quiz”)
                //                             Si no aparece  "description", "moreinfo", "video" se deja en blanco
                pregunta.setSubcategories(null);
                // Se herada del padre
                if (subcategory.has("img")) {
                    pregunta.setImg(subcategory.getString("img"));
                } else {
                    pregunta.setImg(parent.getString("img"));
                }
                // Se herada del padre
                if (subcategory.has("theme")) {
                    pregunta.setTheme(subcategory.getString("theme"));
                } else {
                    pregunta.setTheme(parent.getString("theme"));
                }
                // Si no aparece lo toma del .txt
                if (subcategory.has("category")) {
                    pregunta.setCategory(subcategory.getString("category"));
                } else {
                    pregunta.setCategory(null);
                }
                // Si no aparece, se deja en blanco
                if (subcategory.has("video")) {
                    pregunta.setVideo(subcategory.getString("video"));
                } else {
                    pregunta.setVideo(null);
                }
                // Si no aparece, se deja en blanco
                if (subcategory.has("description")) {
                    pregunta.setDescription(subcategory.getString("description"));
                } else {
                    pregunta.setDescription(null);
                }
                // Si no aparece, se deja en blanco
                if (subcategory.has("moreinfo")) {
                    pregunta.setMoreinfo(subcategory.getString("moreinfo"));
                } else {
                    pregunta.setMoreinfo(null);
                }
                if (subcategory.has("quiz")) {
                    getQuizzesTXTFromInternetVolley(pregunta, subcategory.getString("quiz"));
                } else {
                    if (pregunta.getDescription() == null && pregunta.getCategory() != null) {
                        pregunta.setDescription(pregunta.getCategory());
                    } else {
                        pregunta.setDescription(parent.getString("description"));
                    }
                }
            }

            preguntas.add(pregunta);
        }
        return preguntas;
    }

    /**
     * Get Quizzes from a plain text in Internet
     *
     * @param sub_subcategory
     * @param urlStr
     */
    private void getQuizzesTXTFromInternetVolley(final CategoryJSON sub_subcategory,
                                                 final String urlStr) throws MalformedURLException, URISyntaxException {
//        StringRequestHeaders request = new StringRequestHeaders(Request.Method.GET, "http://mmoviles.upv.es/test/OpenCV/3.2_OpenCV-Segmentacion.txt".replace(" ", "%20"), new Response.Listener<String>() {
        StringRequestHeaders request = new StringRequestHeaders(Request.Method.GET, urlStr.replace(" ", "%20"), new Response.Listener<String>() {
            @Override
            public void onResponse(final String response) {

                new Thread() {
                    public void run() {

                        try {
                            getQuizzesFromString(sub_subcategory, response, urlStr.replace(" ", "%20"));
//                            getQuizzesFromString(sub_subcategory, response, "http://mmoviles.upv.es/test/OpenCV/3.2_OpenCV-Segmentacion.txt".replace(" ", "%20"));
                            updateProgress();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            TrivialJSonHelper.getInstance(mContext, false).sendBroadCastMessage("ERROR");
                        }

                    }
                }.start();
            }
        }, new Response.ErrorListener()

        {
            @Override
            public void onErrorResponse(VolleyError error) {
                TrivialJSonHelper.getInstance(mContext, false).sendBroadCastError("Volley", "Loading quizzes!");

                TrivialJSonHelper.cancelRequests();
            }
        }, true);

        VolleySingleton.getInstance(mContext).addToRequestQueue(request);

        addRequest();
    }

}