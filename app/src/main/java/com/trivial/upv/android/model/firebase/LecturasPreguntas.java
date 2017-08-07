package com.trivial.upv.android.model.firebase;

import android.os.Bundle;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.trivial.upv.android.helper.singleton.FirebaseSingleton;


public class LecturasPreguntas
        implements ChildEventListener {

    protected void onCreate(Bundle savedInstanceState) {

        // ConnectFirebase
        final DatabaseReference categorias = FirebaseSingleton.getInstance().getRoot();
        Log.d("REFERENCIA", categorias.getRef().toString());
        //categorias.addChildEventListener(this);
        categorias.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Log.d("CATEGORÍA2", "ENTRO");
                //items.add(dataSnapshot);

                // 1. Lecturas por niveles
                /*for (DataSnapshot hijo : dataSnapshot.getChildren()) {
                    Log.d("CATEGORY", "\t" + hijo.getKey());

                    for (DataSnapshot nieto : hijo.getChildren()) {
                        Log.d("CATEGORY", "\t\t" + nieto.getKey());

                        for (DataSnapshot bisnieto : nieto.getChildren()) {
                            Log.d("CATEGORY", "\t\t\t" + bisnieto.getKey());

                            for (DataSnapshot preguntas : bisnieto.getChildren()) {
                                Log.d("CATEGORY", "\t\t\t\t" + preguntas.getKey());
                                if (preguntas.getValue() instanceof String)
                                    Log.d("CATEGORY", "\t\t\t\t" + (String) preguntas.getValue());
                                else if (preguntas.getValue() instanceof Long ){
                                    Log.d("CATEGORY", "\t\t\t\t" + (Long) preguntas.getValue());
                                }
                                else {
                                    List<String> value = (List<String>) preguntas.getValue();

                                    for (String elem: value) {
                                        Log.d("CATEGORY","\t\t\t\t" +  elem);
                                    }
                                }
                            }
                        }
                    }
                }*/

                // Opción 2: cargar mediente un POJO: Categoria
                for (DataSnapshot hijo : dataSnapshot.getChildren()) {
                    Log.d("CATEGORY", "\t" + hijo.getKey());

                    Categoria categoria = hijo.getValue(Categoria.class);

                    Log.d("CATEGORIAS", categoria.getTema());



                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
        Log.d("CATEGORÍA", "ENTRO");
        //items.add(dataSnapshot);
        if (dataSnapshot != null)
            Log.d("CATEGORÍA", dataSnapshot.getKey());

    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
        Log.d("CATEGORÍA", "ENTRO");
        /*String key = dataSnapshot.getKey();
        int index = keys.indexOf(key);
        if (index != -1) {
            items.set(index, dataSnapshot);
            notifyItemChanged(index, dataSnapshot.getValue(Libro.class));
        }*/
    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot) {
        Log.d("CATEGORÍA", "ENTRO");
        /*String key = dataSnapshot.getKey();
        int index = keys.indexOf(key);
        if (index != -1) {
            keys.remove(index);
            items.remove(index);
            notifyItemRemoved(index);
        }*/
    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
        Log.d("CATEGORÍA", "ENTRO");
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }
}
