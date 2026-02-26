package com.example.gamehub.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamehub.R;
import com.example.gamehub.model.Challenge;

import java.util.List;

/**
 * Adaptador para mostrar la lista de desafíos/logros.
 */
public class ChallengeAdapter extends RecyclerView.Adapter<ChallengeAdapter.ChallengeViewHolder> {

    private final List<Challenge> challenges;
    private int lastAnimatedPosition = -1;

    public ChallengeAdapter(List<Challenge> challenges) {
        this.challenges = challenges;
    }

    @NonNull
    @Override
    public ChallengeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_challenge_card, parent, false);
        return new ChallengeViewHolder(view);
    }

    @SuppressLint("RecyclerView")
    @Override
    public void onBindViewHolder(@NonNull ChallengeViewHolder holder, int position) {
        int pos = holder.getBindingAdapterPosition();
        if (pos < 0 || pos >= challenges.size()) return;
        Challenge challenge = challenges.get(pos);
        holder.bind(challenge);

        // Animación de entrada escalonada
        if (pos > lastAnimatedPosition) {
            holder.itemView.setAlpha(0f);
            holder.itemView.setTranslationX(-40f);
            holder.itemView.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(350)
                    .setStartDelay((long) pos * 60)
                    .setInterpolator(new OvershootInterpolator(1.0f))
                    .start();
            lastAnimatedPosition = pos;
        }
    }

    @Override
    public int getItemCount() {
        return challenges.size();
    }

    static class ChallengeViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivChallengeIcon;
        private final TextView tvChallengeTitle;
        private final TextView tvChallengeDescription;
        private final ProgressBar progressChallenge;
        private final TextView tvChallengeProgress;
        private final TextView tvChallengeStatus;

        public ChallengeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivChallengeIcon = itemView.findViewById(R.id.ivChallengeIcon);
            tvChallengeTitle = itemView.findViewById(R.id.tvChallengeTitle);
            tvChallengeDescription = itemView.findViewById(R.id.tvChallengeDescription);
            progressChallenge = itemView.findViewById(R.id.progressChallenge);
            tvChallengeProgress = itemView.findViewById(R.id.tvChallengeProgress);
            tvChallengeStatus = itemView.findViewById(R.id.tvChallengeStatus);
        }

        void bind(Challenge challenge) {
            ivChallengeIcon.setImageResource(challenge.getIconRes());
            tvChallengeTitle.setText(challenge.getTitle());
            tvChallengeDescription.setText(challenge.getDescription());

            progressChallenge.setMax(100);
            progressChallenge.setProgress(challenge.getProgressPercent());

            tvChallengeProgress.setText(
                    itemView.getContext().getString(R.string.challenge_progress,
                            challenge.getCurrentValue(), challenge.getTargetValue()));

            if (challenge.isCompleted()) {
                tvChallengeStatus.setText(R.string.challenge_completed);
                tvChallengeStatus.setBackgroundResource(R.drawable.bg_challenge_completed);
                ivChallengeIcon.setColorFilter(
                        itemView.getContext().getColor(R.color.challenge_completed));
            } else {
                tvChallengeStatus.setText(
                        itemView.getContext().getString(R.string.challenge_points_reward,
                                challenge.getPointsReward()));
                tvChallengeStatus.setBackgroundResource(R.drawable.bg_challenge_locked);
                ivChallengeIcon.setColorFilter(
                        itemView.getContext().getColor(R.color.hub_accent));
            }
        }
    }
}

