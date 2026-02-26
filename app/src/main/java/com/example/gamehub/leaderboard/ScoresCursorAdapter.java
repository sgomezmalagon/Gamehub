package com.example.gamehub.leaderboard;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamehub.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Adapter simple que usa Cursor y un RecyclerView.
 * Implementa swapCursor para actualizar datos.
 */
public class ScoresCursorAdapter extends RecyclerView.Adapter<ScoresCursorAdapter.VH> {
    private Cursor cursor;
    private final Context ctx;

    public ScoresCursorAdapter(Context ctx, Cursor c) {
        this.ctx = ctx;
        this.cursor = c;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_score_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        if (cursor == null || cursor.isClosed()) return;
        if (!cursor.moveToPosition(position)) return;
        long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        String player = cursor.getString(cursor.getColumnIndexOrThrow("player_name"));
        long score = cursor.getLong(cursor.getColumnIndexOrThrow("score_value"));
        long created = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));

        // game_name column (may not exist in old DBs)
        String gameName = null;
        int gameNameIdx = cursor.getColumnIndex("game_name");
        if (gameNameIdx >= 0 && !cursor.isNull(gameNameIdx)) {
            gameName = cursor.getString(gameNameIdx);
        }

        holder.bind(id, player, gameName, score, created);
    }

    @Override
    public int getItemCount() {
        if (cursor == null) return 0;
        return cursor.getCount();
    }

    public void swapCursor(Cursor c) {
        if (this.cursor == c) return;
        if (this.cursor != null) this.cursor.close();
        this.cursor = c;
        notifyDataSetChanged();
    }

    public long getIdAt(int position) {
        if (cursor == null || cursor.isClosed()) return -1;
        if (!cursor.moveToPosition(position)) return -1;
        return cursor.getLong(cursor.getColumnIndexOrThrow("id"));
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvPlayerName, tvScoreValue, tvDate;
        long boundId;
        public VH(@NonNull View itemView) {
            super(itemView);
            tvPlayerName = itemView.findViewById(R.id.tvPlayerName);
            tvScoreValue = itemView.findViewById(R.id.tvScoreValue);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
        void bind(long id, String player, String gameName, long score, long createdAt) {
            boundId = id;
            // Mostrar nombre del jugador con el juego si está disponible
            if (gameName != null && !gameName.isEmpty()) {
                tvPlayerName.setText(player + " • " + gameName);
            } else {
                tvPlayerName.setText(player);
            }
            tvScoreValue.setText(String.valueOf(score));
            String date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(createdAt * 1000L));
            tvDate.setText(date);
            // open detail on click
            itemView.setOnClickListener(v -> {
                Context c = v.getContext();
                Intent intent = new Intent(c, ScoreDetailActivity.class);
                intent.putExtra(ScoreDetailActivity.EXTRA_SCORE_ID, boundId);
                c.startActivity(intent);
            });
        }
    }
}
