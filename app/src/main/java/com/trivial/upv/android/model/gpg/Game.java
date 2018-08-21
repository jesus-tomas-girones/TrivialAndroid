package com.trivial.upv.android.model.gpg;

import com.google.android.gms.games.TurnBasedMultiplayerClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.trivial.upv.android.model.Category;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jvg63 on 22/06/2017.
 */

public class Game {


    public static String mMyId;
    public static int jugadorLocal;

    // Se ha quedado obsoleto GoogleApiClient
//    public static GoogleApiClient mGoogleApiClient;
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
    public static Map<String, Long> mFinishedParticipants = new HashMap<>();
    public static Map<String, Long> mParticipantScore = new HashMap<>();

    public static int numPlayers;
    public static int totalTime;
    public static String myName;

    public static int tmpNumQuizzes = 10;
    public static int tmpNumPlayers = 2;
    public static int tmpTotalTime = 250;
    public static String master;
    public static List<String> listCategories = new ArrayList<>();
    public static long level;
    public static Invitation pendingInvitation;
    public static TurnBasedMatch pendingTurnBasedMatch;
    public static String mode;
    public static int categorySelected;
    public static Turn mTurnData;
    public static String mPlayerId;
    public static TurnBasedMatch mMatch;

    public static void resetGameVars() {
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

        categorySelected = -1;
//        level = 0;
    }

    public static int numParticipantsOK() {
        int total = 0;
        for (Participant p : Game.mParticipants) {
            if (!p.getParticipantId().equals(Game.mMyId) && p.getStatus() == Participant.STATUS_JOINED) {
                total++;
            }
        }

        return total;
    }
}
