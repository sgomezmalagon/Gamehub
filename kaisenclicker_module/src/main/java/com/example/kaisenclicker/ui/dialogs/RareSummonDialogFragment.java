package com.example.kaisenclicker.ui.dialogs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.kaisenclicker.R;

public class RareSummonDialogFragment extends DialogFragment {

    private static final String ARG_AMOUNT = "arg_amount";
    private int amount = 0;

    public static RareSummonDialogFragment newInstance(int cursedEnergyAmount) {
        RareSummonDialogFragment f = new RareSummonDialogFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_AMOUNT, cursedEnergyAmount);
        f.setArguments(b);
        f.setCancelable(false);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            amount = getArguments().getInt(ARG_AMOUNT, 0);
        }
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_rare_summon, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final ImageView iv = view.findViewById(R.id.ivRareSummon);
        final TextView tv = view.findViewById(R.id.tvCursedEnergy);

        ObjectAnimator alphaIn = ObjectAnimator.ofFloat(iv, View.ALPHA, 0f, 1f);
        alphaIn.setDuration(300);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(iv, View.SCALE_X, 0.6f, 1.2f, 1f);
        scaleX.setDuration(800);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(iv, View.SCALE_Y, 0.6f, 1.2f, 1f);
        scaleY.setDuration(800);
        ObjectAnimator rotate = ObjectAnimator.ofFloat(iv, View.ROTATION, -6f, 6f, 0f);
        rotate.setDuration(800);

        AnimatorSet appearSet = new AnimatorSet();
        appearSet.setInterpolator(new BounceInterpolator());
        appearSet.playTogether(alphaIn, scaleX, scaleY, rotate);
        appearSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                tv.setVisibility(View.VISIBLE);
                animateCursedEnergy(tv, amount);
                iv.animate().alpha(0f).setDuration(400).start();
            }
        });
        appearSet.setStartDelay(150);
        appearSet.start();
    }

    private void animateCursedEnergy(final TextView target, int finalAmount) {
        ValueAnimator anim = ValueAnimator.ofInt(0, finalAmount);
        anim.setDuration(900);
        anim.setInterpolator(new android.view.animation.DecelerateInterpolator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int v = (int) animation.getAnimatedValue();
                target.setText(v + " ☠");
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                dismissAllowingStateLoss();
            }
        });

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(target, View.SCALE_X, 0.9f, 1.08f, 1f);
        scaleX.setDuration(900);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 0.9f, 1.08f, 1f);
        scaleY.setDuration(900);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(anim, scaleX, scaleY);
        set.start();
    }
}

