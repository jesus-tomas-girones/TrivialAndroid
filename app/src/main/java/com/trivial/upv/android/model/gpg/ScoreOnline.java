package com.trivial.upv.android.model.gpg;

import com.google.android.gms.games.multiplayer.Participant;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jvg63 on 03/09/2017.
 */

public class ScoreOnline {
    public static class Score {

        public int getTimeLeft() {
            return mTimeLeft;
        }

        public boolean isWinner() {
            return mWinner;
        }

        public int getPoints() {
            return mPoints;
        }

        public String getParticipant() {
            return mParticipant;
        }

        public Score(String participant, String displayName, String status, int points, int timeLeft) {
            this.mTimeLeft = timeLeft;
            this.mPoints = points;
            this.mParticipant = participant;
            this.mDisplayName = displayName;
            this.mWinner = false;
            this.mStatus = status;
        }

        private int mTimeLeft;

        public void setTimeLeft(int mTimeLeft) {
            this.mTimeLeft = mTimeLeft;
        }

        public void setWinner(boolean mWinner) {
            this.mWinner = mWinner;
        }

        public void setPoints(int mPoints) {
            this.mPoints = mPoints;
        }

        public void setParticipant(String mParticipant) {
            this.mParticipant = mParticipant;
        }

        public void setDisplayName(String mDisplayName) {
            this.mDisplayName = mDisplayName;
        }

        public void setStatus(String mStatus) {
            this.mStatus = mStatus;
        }

        private boolean mWinner;
        private int mPoints;
        private String mParticipant;

        public String getDisplayName() {
            return mDisplayName;
        }

        public String getStatus() {
            return mStatus;
        }

        private String mDisplayName;
        private String mStatus;

    }

    private List<Score> mScoreOnline;

    public List<Score> getScoreOnline() {
        return mScoreOnline;
    }

    public void setScoreOnline(List<Score> mScoreOnline) {
        this.mScoreOnline = mScoreOnline;
    }

    public ScoreOnline() {
        mScoreOnline = new ArrayList<>();

    }

    public synchronized void add(Score score) {
        mScoreOnline.add(score);
        updateIsWinner();
    }

    public synchronized void updateIsWinner() {
        if (mScoreOnline != null && mScoreOnline.size() == 0) {
            return;
        } else {
            int maxPoint = Integer.MIN_VALUE;
            int maxTimeLeft = Integer.MIN_VALUE;
            // Máxima puntuación
            for (Score score : mScoreOnline) {
                if (maxPoint <= score.mPoints) {
                    maxPoint = score.mPoints;

                }
            }

            for (Score score : mScoreOnline) {
                if (maxPoint == score.mPoints) {
                    if (maxTimeLeft <= score.mTimeLeft) {
                        maxTimeLeft = score.mTimeLeft;
                    }
                }
            }

            for (Score score : mScoreOnline) {
                if (score.mPoints == maxPoint && score.mTimeLeft == maxTimeLeft) {
                    score.mWinner = true;
                } else {
                    score.mWinner = false;
                }
            }
        }
    }

    public synchronized boolean areAnyPendingScore() {
        for (Score score : mScoreOnline) {
            if (score.getStatus().equals("PENDING")) {
                for (Participant p : Game.mParticipants) {
                    if (p.getParticipantId().equals(score.getParticipant())) {
                        if (p.isConnectedToRoom() && p.getStatus()==Participant.STATUS_JOINED)
                            return true;
                        else {
                            score.setStatus("UNKNOWN");
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

}