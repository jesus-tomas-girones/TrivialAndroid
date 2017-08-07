package com.trivial.upv.android.helper.singleton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by jvg63 on 22/01/2017.
 */
public class FirebaseSingleton {
    private final static String ROOT = "preguntas";
    public final static String SUBTEMAS_CHILD = "subtemas";
    public final static String PREGUNTAS_CHILD = "preguntas";

    private static FirebaseSingleton firebase = null;
    private FirebaseAuth auth = null;
    private FirebaseDatabase databaseReference = null;

    private DatabaseReference root;

    public static FirebaseSingleton getInstance() {
        if (firebase == null)
            synchronized (FirebaseSingleton.class) {
                if (firebase == null) {
                    firebase = new FirebaseSingleton();
                }
            }
        return firebase;
    }

    private FirebaseSingleton() {
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance();
        databaseReference.setPersistenceEnabled(true);
        root = databaseReference.getReference().child(ROOT);
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    public FirebaseDatabase getDatabaseReference() {
        return databaseReference;
    }

    public DatabaseReference getRoot() {
        return root;
    }
}
