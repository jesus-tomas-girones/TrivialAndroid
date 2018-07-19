package com.trivial.upv.android.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.trivial.upv.android.R;

//JTG.S
public class HelpDialogFragment extends DialogFragment {
   @NonNull
   @Override
   public Dialog onCreateDialog(Bundle savedInstanceState) {
      LayoutInflater inflater = getActivity().getLayoutInflater();
      View view = inflater.inflate(R.layout.dialog_help, null);
      TextView textView = (TextView) view.findViewById(R.id.text1);
      textView.setMovementMethod(LinkMovementMethod.getInstance());
      textView = (TextView) view.findViewById(R.id.text2);
      textView.setMovementMethod(LinkMovementMethod.getInstance());
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setView(view)
              .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int id) {
                 }
              })
              .setNegativeButton("Sobre el Master de Android", new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int id) {
                    Uri uri = Uri.parse("http://www.androidcurso.com");
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                 }
              });
      return builder.create();
   }
}
//JTG.E