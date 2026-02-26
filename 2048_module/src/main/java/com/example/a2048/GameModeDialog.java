package com.example.a2048;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import com.google.android.material.card.MaterialCardView;

public class GameModeDialog {

    public interface ModeListener {
        void onModeChosen(MainActivity.Mode mode);
    }

    public static void show(Context ctx, final ModeListener listener) {
        // Create a plain Dialog so we control the look entirely
        Dialog dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(ctx).inflate(R.layout.dialog_mode_selector, null);
        dialog.setContentView(view);
        dialog.setCancelable(false);

        // Transparent background so our rounded corners show
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        MaterialCardView cardNormal = view.findViewById(R.id.card_normal);
        MaterialCardView cardBlitz = view.findViewById(R.id.card_blitz);
        View btnCancel = view.findViewById(R.id.btn_cancel_mode);

        cardNormal.setOnClickListener(v -> {
            if (listener != null) listener.onModeChosen(MainActivity.Mode.NORMAL);
            dialog.dismiss();
        });

        cardBlitz.setOnClickListener(v -> {
            if (listener != null) listener.onModeChosen(MainActivity.Mode.BLITZ);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}

