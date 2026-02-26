package com.example.gamehub.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamehub.R;
import com.example.gamehub.auth.SessionManager;
import com.example.gamehub.model.Game;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

/**
 * Adaptador del RecyclerView que muestra las tarjetas de juegos en el grid.
 */
public class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameViewHolder> {

    private final List<Game> games;
    private final String username;
    private int lastAnimatedPosition = -1;

    public GameAdapter(List<Game> games, String username) {
        this.games = games;
        this.username = username;
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_game_card, parent, false);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        Game game = games.get(position);
        holder.bind(game, username);

        // Animación de entrada escalonada
        if (position > lastAnimatedPosition) {
            holder.itemView.setAlpha(0f);
            holder.itemView.setTranslationY(60f);
            holder.itemView.setScaleX(0.9f);
            holder.itemView.setScaleY(0.9f);
            holder.itemView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(400)
                    .setStartDelay((long) position * 80)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
            lastAnimatedPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return games.size();
    }

    /**
     * Actualiza la lista de juegos y refresca el adaptador.
     */
    public void updateGames(List<Game> newGames) {
        games.clear();
        games.addAll(newGames);
        lastAnimatedPosition = -1;
        notifyDataSetChanged();
    }

    // ─────────────────────────────────────────────

    static class GameViewHolder extends RecyclerView.ViewHolder {

        private final ShapeableImageView ivGameIcon;
        private final TextView tvGameName;
        private final TextView tvGameDescription;

        public GameViewHolder(@NonNull View itemView) {
            super(itemView);
            ivGameIcon = itemView.findViewById(R.id.ivGameIcon);
            tvGameName = itemView.findViewById(R.id.tvGameName);
            tvGameDescription = itemView.findViewById(R.id.tvGameDescription);
        }

        public void bind(Game game, String username) {
            ivGameIcon.setImageResource(game.getIconRes());
            tvGameName.setText(game.getName());
            tvGameDescription.setText(game.getDescription());

            // Al hacer click, abre la Activity del juego pasando el usuario
            itemView.setOnClickListener(v -> {
                Context context = v.getContext();
                if (game.getActivityClass() != null) {
                    // Registrar inicio de partida para tracking de tiempo
                    SessionManager sm = new SessionManager(context);
                    sm.markGameStarted();
                    sm.incrementGamesPlayed();

                    Intent intent = new Intent(context, game.getActivityClass());
                    if (username != null) {
                        intent.putExtra("extra_username", username);
                    }
                    context.startActivity(intent);
                }
            });
        }
    }
}

