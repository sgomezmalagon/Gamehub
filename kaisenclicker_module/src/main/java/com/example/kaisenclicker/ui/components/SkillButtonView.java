package com.example.kaisenclicker.ui.components;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import com.example.kaisenclicker.R;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

public class SkillButtonView extends FrameLayout {
    private ImageView ivSkillIcon;
    private ImageView cooldownOverlay;
    private TextView tvCooldown;
    private boolean isOnCooldown = false;
    private CountDownTimer cooldownTimer;
    private String skillDescription = null;
    private PopupWindow tooltipWindow = null;

    public SkillButtonView(Context context) {
        super(context);
        init(context);
    }

    public SkillButtonView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.skill_button, this, true);
        ivSkillIcon = findViewById(R.id.iv_skill_icon);
        cooldownOverlay = findViewById(R.id.cooldown_overlay);
        tvCooldown = findViewById(R.id.tv_cooldown);
        // Forzar recorte circular en el icono
        ivSkillIcon.setClipToOutline(true);
        ivSkillIcon.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                int size = Math.min(view.getWidth(), view.getHeight());
                outline.setOval(0, 0, size, size);
            }
        });
    }

    public void setSkillIcon(@DrawableRes int iconRes) {
        ivSkillIcon.setImageResource(iconRes);
    }

    public void setOnSkillClickListener(final OnClickListener listener) {
        this.setOnClickListener(v -> {
            if (!isOnCooldown) {
                animateActivation();
                listener.onClick(v);
            }
        });
    }

    public void setSkillDescription(String description) {
        this.skillDescription = description;
    }

    public void setOnSkillLongClickListener() {
        this.setOnLongClickListener(v -> {
            if (skillDescription != null && !skillDescription.isEmpty()) {
                showSkillTooltip();
                return true;
            }
            return false;
        });
        // Ocultar tooltip al soltar el dedo o salir del botón
        this.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP || event.getAction() == android.view.MotionEvent.ACTION_CANCEL || event.getAction() == android.view.MotionEvent.ACTION_OUTSIDE) {
                hideSkillTooltip();
            }
            return false;
        });
    }

    private void showSkillTooltip() {
        if (tooltipWindow != null && tooltipWindow.isShowing()) return;
        View tooltipView = LayoutInflater.from(getContext()).inflate(R.layout.tooltip_skill_description, null);
        TextView tvTooltip = tooltipView.findViewById(R.id.tv_tooltip_text);
        tvTooltip.setText(skillDescription);
        tooltipWindow = new PopupWindow(tooltipView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
        tooltipWindow.setElevation(12f);
        tooltipWindow.setOutsideTouchable(true);
        tooltipWindow.setFocusable(false);
        // Calcular posición: mostrar encima y centrado respecto al botón
        int[] location = new int[2];
        this.getLocationOnScreen(location);
        int x = location[0] + this.getWidth() / 2;
        int y = location[1] - this.getHeight() / 2;
        tooltipView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        int tooltipWidth = tooltipView.getMeasuredWidth();
        int tooltipHeight = tooltipView.getMeasuredHeight();
        tooltipWindow.showAtLocation(this, android.view.Gravity.NO_GRAVITY, x - tooltipWidth / 2, y - tooltipHeight);
    }

    private void hideSkillTooltip() {
        if (tooltipWindow != null && tooltipWindow.isShowing()) {
            tooltipWindow.dismiss();
        }
        tooltipWindow = null;
    }

    public void startCooldown(long durationMs) {
        isOnCooldown = true;
        cooldownOverlay.setVisibility(View.VISIBLE);
        cooldownOverlay.setAlpha(0.7f);
        tvCooldown.setVisibility(View.VISIBLE);
        // Escala de grises y opacidad al icono
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0f);
        ivSkillIcon.setColorFilter(new ColorMatrixColorFilter(matrix));
        ivSkillIcon.setAlpha(0.5f);
        if (cooldownTimer != null) cooldownTimer.cancel();
        cooldownTimer = new CountDownTimer(durationMs, 50) {
            public void onTick(long millisUntilFinished) {
                float percent = millisUntilFinished / (float) durationMs;
                cooldownOverlay.setAlpha(0.7f * percent);
                // Mostrar décimas
                double seconds = millisUntilFinished / 1000.0;
                tvCooldown.setText(String.format("%.1fs", seconds));
            }
            public void onFinish() {
                cooldownOverlay.setVisibility(View.GONE);
                tvCooldown.setVisibility(View.GONE);
                isOnCooldown = false;
                // Restaurar color y opacidad
                ivSkillIcon.setColorFilter(null);
                ivSkillIcon.setAlpha(1.0f);
            }
        };
        cooldownTimer.start();
    }

    private void animateActivation() {
        ScaleAnimation anim = new ScaleAnimation(
                1f, 1.15f, 1f, 1.15f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(80);
        anim.setRepeatCount(1);
        anim.setRepeatMode(ScaleAnimation.REVERSE);
        this.startAnimation(anim);
    }

    public boolean isOnCooldown() {
        return isOnCooldown;
    }
}
