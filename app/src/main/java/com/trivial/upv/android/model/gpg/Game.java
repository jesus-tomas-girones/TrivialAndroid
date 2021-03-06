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
    // Turnbased
    public static final int K_TIME_TO_ANSWER_TURN_BASED = 30;
    public static String mMyId;
    public static int categorySelected;
    public static Turn mTurnData;
    public static String mPlayerId;
    public static TurnBasedMatch mMatch;
    public static Invitation pendingTurnInvitation;
    public static TurnBasedMatch pendingTurnBasedMatch;

    // Common
    public static Category category;
    public static String mode;

    // Real time
    public static int jugadorLocal;
    public static int numQuizzes;
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
    public static int level;
    public static Invitation pendingInvitation;
    public static TurnBasedMultiplayerClient mTurnBasedMultiplayerClient;


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

    /**
     * Get the next participant. In this function, we assume that we are
     * round-robin, with all known players going before all automatch players.
     * This is not a requirement; players can go in any order. However, you can
     * take turns in any order.
     *
     * @return participantId of next player, or null if automatching
     */
    public static String getNextParticipantId() {

        String myParticipantId = Game.mMatch.getParticipantId(Game.mPlayerId);

        ArrayList<String> participantIds = Game.mMatch.getParticipantIds();

        int desiredIndex = -1;

        for (int i = 0; i < participantIds.size(); i++) {
            if (participantIds.get(i).equals(myParticipantId)) {
                desiredIndex = i + 1;
            }
        }

        if (desiredIndex < participantIds.size()) {
            return participantIds.get(desiredIndex);
        }

        if (Game.mMatch.getAvailableAutoMatchSlots() <= 0) {
            // You've run out of automatch slots, so we roulette_rotate over.
            return participantIds.get(0);
        } else {
            // You have not yet fully automatched, so null will find a new
            // person to play against.
            return null;
        }
    }
}
