package com.trivial.upv.android.fragment;

import android.app.Dialog;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;

import com.trivial.upv.android.R;
import com.trivial.upv.android.activity.RouletteActivity;

public class CustomDialogFragment extends DialogFragment {
    /**
     * The system calls this to get the DialogFragment's layout, regardless
     * of whether it's being displayed as a dialog or an embedded fragment.
     */
    private static RouletteActivity.FinishCounterEvent conCuentaAtras = null;

    private static ProgressBar progressBar = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout to use as dialog or embedded fragment
        View view = inflater.inflate(R.layout.custom_dialog_fragment, container, false);
        progressBar = (ProgressBar) view.findViewById(R.id.progress_dialog);
        ((ProgressBar) view.findViewById(R.id.progress_dialog)).getIndeterminateDrawable().setColorFilter(getContext().getResources().getColor(R.color.topeka_primary), PorterDuff.Mode.SRC_IN);
        return view;
    }

    /**
     * The system calls this only when creating the layout in a dialog.
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // The only reason you might override this method when using onCreateView() is
        // to modify any dialog characteristics. For example, the dialog includes a
        // title by default, but your custom layout might not need it. So here you can
        // remove the dialog title, but you must call the superclass to get the Dialog.
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        return dialog;
    }

    public static CustomDialogFragment newFragment = null;

    public static void showDialog(FragmentManager fragmentManager, RouletteActivity.FinishCounterEvent conCuentaAtrasIn) {
//        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        if (newFragment != null) {
            newFragment.dismiss();
            newFragment = null;
        }

        conCuentaAtras = conCuentaAtrasIn;

        newFragment = new CustomDialogFragment();
        newFragment.show(fragmentManager, "dialog");
        newFragment.setCancelable(false);

        if (conCuentaAtras != null) {
            final MyCounter timer = new MyCounter(3000, 1000);
            timer.start();
        }

//        if (mIsLargeLayout) {
//            // The device is using a large layout, so show the fragment as a dialog
//            newFragment.show(fragmentManager, "dialog");
//        } else {
//        // The device is smaller, so show the fragment fullscreen
//        FragmentTransaction transaction = fragmentManager.beginTransaction();
//        // For a little polish, specify a transition animation
//        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
//        // To make it fullscreen, use the 'content' root view as the container
//        // for the fragment, which is always the root view for the activity
//        transaction.add(android.R.id.content, newFragment)
//                .addToBackStack(null).commit();
    }
//    }

    public static void dismissDialog() {
        if (newFragment != null) {
            newFragment.dismiss();
            newFragment = null;
        }
    }

    private static class MyCounter extends CountDownTimer {

        public MyCounter(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            //Lo que quieras hacer al finalizar
            if (newFragment != null) {
                newFragment.dismiss();
                if (conCuentaAtras != null) {
                    conCuentaAtras.onFinishedCount();
                }
            }
        }

        @Override
        public void onTick(long millisUntilFinished) {
            //texto a mostrar en cuenta regresiva en un textview
//            if (progressBar != null) {
//                progressBar.setProgress(progressBar.getProgress() + 1);
//            }
        }
    }
}
