package com.trivial.upv.android.model.gpg;

import com.trivial.upv.android.fragment.PlayTurnBasedFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class Turn {

    // [][0]->Jugador (0) no se ha jugado
    // [][1]->[Puntuacion]}
    // [][2]->[Category]}
    public int numPreguntas = 0;
    public int numTurnos = 0;
    public int numPreguntasContestadas = 0;
    public int numJugadores = 0;
    public short puntuacion[][] = null;
    public List<String> participantsTurnBased = null;

    public Turn() {
    }

    public byte[] persist() {
        JSONObject retVal = new JSONObject();
        try {
            retVal.put("numPreguntas", numPreguntas);
            retVal.put("numTurnos", numTurnos);
            retVal.put("numPreguntasContestadas", numPreguntasContestadas);
            retVal.put("numJugadores", numJugadores);
            retVal.put("participantsTurnBased", participantsTurnBased);
            String points = "";
            for (int i = 0; i < puntuacion.length; i++) {
                for (int j = 0; j < puntuacion[i].length; j++)
                    if (puntuacion[i][j] < 10) {
                        points += "0" + puntuacion[i][j];
                    } else {
                        points += puntuacion[i][j];
                    }
            }
            retVal.put("puntuacion", points);
        } catch (JSONException e) {
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
            if (obj.has("numTurnos")) {
                retVal.numTurnos = obj.getInt("numTurnos");
            }
            if (obj.has("numPreguntasContestadas")) {
                retVal.numPreguntasContestadas = obj.getInt("numPreguntasContestadas");
            }

            if (obj.has("numJugadores")) {
                retVal.numJugadores = obj.getInt("numJugadores");
            }
            if (obj.has("participantsTurnBased")) {
                JSONArray jugadores = new JSONArray(obj.getString("participantsTurnBased"));
                retVal.participantsTurnBased = new ArrayList<>();
                for (int i = 0; i < jugadores.length(); i++) {
                    retVal.participantsTurnBased.add(jugadores.get(i).toString());
                }
            }

            if (obj.has("puntuacion")) {
                String auxPuntuacion = obj.getString("puntuacion");
                retVal.puntuacion = new short[retVal.numPreguntas][3];
                int k = 0;
                for (int i = 0; i < retVal.numPreguntas; i++) {
                    retVal.puntuacion[i][0] = Short.parseShort(auxPuntuacion.substring(k, k + 2));
                    k = k + 2;
                    retVal.puntuacion[i][1] = Short.parseShort(auxPuntuacion.substring(k, k + 2));
                    k = k + 2;
                    retVal.puntuacion[i][2] = Short.parseShort(auxPuntuacion.substring(k, k + 2));
                    k = k + 2;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return retVal;
    }

    public  int calculateScorePlayer(String playerId) {
        int auxPuntuacion = 0;
        if (participantsTurnBased!=null) {
            String myParticipantId = Game.mMatch.getParticipantId(playerId);
            int indexPlayer = participantsTurnBased.indexOf(myParticipantId);

            if (indexPlayer != -1) {
                for (int pos = 0; pos < numPreguntasContestadas; pos++) {
                    if (puntuacion[pos][0] == indexPlayer && puntuacion[pos][1] > 0) {
                        auxPuntuacion++;
                    }
                }
                auxPuntuacion *= PlayTurnBasedFragment.K_PUNTOS_POR_PREGUNTA;
            }
        }
        return auxPuntuacion;
    }

//    public int getNumQuizz() {
//        int pos = 0;
//        for (short[] quiz : puntuacion) {
//            if (quiz[0] == 0) return pos;
//            pos++;
//        }
//        return -1;
//    }
}
