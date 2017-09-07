package com.trivial.upv.android.model.gpg;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMultiplayer;
import com.trivial.upv.android.model.Category;
import com.trivial.upv.android.persistence.TopekaJSonHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.android.gms.games.GamesStatusCodes.STATUS_OK;

/**
 * Created by jvg63 on 22/06/2017.
 */

public class Game {


    public static String mMyId;
    public static int jugadorLocal;
    public static int turno;
    public static int puntosJ1;
    public static int puntosJ2;

    public static String gameType = "LOCAL";

    public static GoogleApiClient mGoogleApiClient;
    public static int numQuizzes;

    public static Category category;
    public static ArrayList<Participant> mParticipants = null;
    public static String mRoomId;
    public static String mRoomIdInvited;
    public static ArrayList<String> invitees;
    public static int minAutoMatchPlayers;
    public static int maxAutoMatchPlayers;
    public static String mIncomingInvitationId;
    public static long timeStamp;
    public static  Map<String, Long> mFinishedParticipants = new HashMap<>();
    public static Map<String, Long> mParticipantScore = new HashMap<>();
    public static Category categoryGame;
    public static int numPlayers;
    public static int totalTime;
    public static String myName;

    public static int tmpNumQuizzes = 10;
    public static int tmpNumPlayers = 2;
    public static int tmpTotalTime = 250;
    public static String master;

    public static void resetGameVars() {
        Game.turno = 1;
        Game.mRoomId = null;
        if (Game.mParticipants != null && Game.mParticipants.size() > 0)
            Game.mParticipants.clear();
        jugadorLocal = 0;
        mRoomIdInvited = null;
        if (Game.invitees != null && Game.invitees.size() > 0)
            Game.invitees.clear();
        Game.minAutoMatchPlayers = Game.maxAutoMatchPlayers = 1;
        timeStamp = 0;
        mFinishedParticipants.clear();
        mParticipantScore.clear();
        master = null;
    }

    public static int numParticipantsOK() {
        int total =0;
        for (Participant p : Game.mParticipants) {
            if (!p.getParticipantId().equals(Game.mMyId) && p.getStatus()==Participant.STATUS_JOINED) {
                total++;
            }
        }

        return total;
    }
}
