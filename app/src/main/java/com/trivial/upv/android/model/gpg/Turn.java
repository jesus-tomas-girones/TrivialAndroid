package com.trivial.upv.android.model.gpg;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class Turn {

    // [][0]->Jugador (0) no se ha jugado
    // [][1]->[Puntuacion]}
    public int numPreguntas = 0;
    public short puntuacion[][];
    public int puntosJ1 = 0;
    public int puntosJ2 = 0;
    public int turnoJugador = 0;
    private int numQuizz;

    public Turn() {
    }

    public byte[] persist() {
        JSONObject retVal = new JSONObject();
        try {
            retVal.put("numPreguntas", numPreguntas);
            retVal.put("puntosJ1", puntosJ1);
            retVal.put("puntosJ2", puntosJ2);
            retVal.put("turnoJugador", turnoJugador);
            String points = "";
            for (int i = 0; i < numPreguntas; i++) {
                if (puntuacion[i][1] < 10) {
                    points += "0" + puntuacion[i][0] + "0" + puntuacion[i][1];
                } else {
                    points += "0" + puntuacion[i][0] + puntuacion[i][1];
                }
            }

            retVal.put("puntuacion", points);
        } catch (
                JSONException e)

        {
            e.printStackTrace();
        }

        String st = retVal.toString();
        return st.getBytes(Charset.forName("UTF-8"));
    }

    static public Turn unpersist(byte[] byteArray) {
        if (byteArray == null) {
            return new Turn();
        }
        String st = null;
        try {
            st = new String(byteArray, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
            return null;
        }
        Turn retVal = new Turn();
        try {
            JSONObject obj = new JSONObject(st);
            if (obj.has("numPreguntas")) {
                retVal.numPreguntas = obj.getInt("numPreguntas");
            }

            if (obj.has("puntosJ1")) {
                retVal.puntosJ1 = obj.getInt("puntosJ1");
            }
            if (obj.has("puntosJ2")) {
                retVal.puntosJ2 = obj.getInt("puntosJ2");
            }
            if (obj.has("turnoJugador")) {
                retVal.turnoJugador = obj.getInt("turnoJugador");
            }

            if (obj.has("puntuacion")) {
                String auxPuntuacion = obj.getString("puntuacion");
                retVal.puntuacion = new short[retVal.numPreguntas][2];
                int k = 0;
                for (int i = 0; i < retVal.numPreguntas; i++) {
                    retVal.puntuacion[i][0] =
                            Short.parseShort(auxPuntuacion.substring(k, k + 2));
                    k = k + 2;
                    retVal.puntuacion[i][1] = Short.parseShort(auxPuntuacion.substring(k, k + 2));
                    k = k + 2;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return retVal;
    }

    public int getNumQuizz() {
        int pos = 0;
        for (short[] quiz: puntuacion) {
            if (quiz[0]==0) return pos;
            pos++;
        }
        return -1;
    }
}
