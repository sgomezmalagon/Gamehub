package com.example.gamehub.leaderboard;

import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamehub.BaseActivity;
import com.example.gamehub.R;
import com.example.gamehub.auth.SessionManager;
import com.example.kaisenclicker.persistence.save.SqlRepository;
import com.google.android.material.snackbar.Snackbar;

/**
 * Pantalla de historial de puntuaciones con:
 * - Lista RecyclerView + CardView usando Cursors
 * - Búsqueda por nombre o valor de score (>, <, =, >=, <=)
 * - Eliminar score al hacer Swipe (ItemTouchHelper)
 * - Ordenar por nombre, puntuación o fecha
 * - Click en CardView → ScoreDetailActivity
 */
public class ScoresListActivity extends BaseActivity {

    private SqlRepository repo;
    private ScoresCursorAdapter adapter;
    private RecyclerView rvScores;
    private TextView tvEmptyScores;

    // Filtros
    private EditText etSearchName;
    private Spinner spinnerOperator;
    private EditText etSearchScore;

    // Ordenación actual
    private String currentOrderBy = "score_value DESC";

    // Botones de orden
    private TextView btnSortName, btnSortScore, btnSortDate;

    private static final String[] OPERATORS = {"=", ">", "<", ">=", "<="};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scores_list);

        // Inicializar repositorio con BD per-user
        SessionManager session = new SessionManager(this);
        String username = session.getUsername();
        String dbName = (username != null && !username.isEmpty())
                ? "kaisen_clicker_" + username + ".db"
                : "kaisen_clicker.db";
        repo = new SqlRepository(this, dbName);

        initViews();
        setupOperatorSpinner();
        setupListeners();
        setupRecyclerView();
        setupSwipeToDelete();
        refreshCursor();
        animateEntrance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCursor();
    }

    private void initViews() {
        rvScores = findViewById(R.id.rvScores);
        tvEmptyScores = findViewById(R.id.tvEmptyScores);
        etSearchName = findViewById(R.id.etSearchName);
        spinnerOperator = findViewById(R.id.spinnerOperator);
        etSearchScore = findViewById(R.id.etSearchScore);
        btnSortName = findViewById(R.id.btnSortName);
        btnSortScore = findViewById(R.id.btnSortScore);
        btnSortDate = findViewById(R.id.btnSortDate);

        // Botón atrás
        ImageView btnBack = findViewById(R.id.btnBackScoresList);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupOperatorSpinner() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, OPERATORS);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOperator.setAdapter(spinnerAdapter);
        spinnerOperator.setSelection(0); // "=" por defecto
    }

    private void setupListeners() {
        // Buscar
        TextView btnSearch = findViewById(R.id.btnSearch);
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> refreshCursor());
        }

        // Limpiar
        TextView btnClear = findViewById(R.id.btnClear);
        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                etSearchName.setText("");
                etSearchScore.setText("");
                spinnerOperator.setSelection(0);
                currentOrderBy = "score_value DESC";
                updateSortButtons(btnSortScore);
                refreshCursor();
            });
        }

        // Ordenar por nombre
        btnSortName.setOnClickListener(v -> {
            currentOrderBy = "player_name ASC";
            updateSortButtons(btnSortName);
            refreshCursor();
        });

        // Ordenar por puntuación
        btnSortScore.setOnClickListener(v -> {
            currentOrderBy = "score_value DESC";
            updateSortButtons(btnSortScore);
            refreshCursor();
        });

        // Ordenar por fecha
        btnSortDate.setOnClickListener(v -> {
            currentOrderBy = "created_at DESC";
            updateSortButtons(btnSortDate);
            refreshCursor();
        });
    }

    private void updateSortButtons(TextView activeBtn) {
        // Reset all
        for (TextView btn : new TextView[]{btnSortName, btnSortScore, btnSortDate}) {
            btn.setTextColor(getColor(R.color.hub_text_hint));
            btn.setBackgroundResource(R.drawable.bg_menu_option);
        }
        // Highlight active
        activeBtn.setTextColor(getColor(R.color.white));
        activeBtn.setBackgroundResource(R.drawable.bg_tab_active);
    }

    private void setupRecyclerView() {
        rvScores.setLayoutManager(new LinearLayoutManager(this));
        rvScores.setHasFixedSize(true);
        adapter = new ScoresCursorAdapter(this, null);
        rvScores.setAdapter(adapter);
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                long scoreId = adapter.getIdAt(position);

                if (scoreId > 0) {
                    // Borrar de la BD
                    repo.deleteScoreById(scoreId);
                    // Refrescar cursor
                    refreshCursor();

                    // Snackbar informativo
                    Snackbar.make(rvScores,
                            R.string.score_deleted,
                            Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    Paint paint = new Paint();
                    paint.setColor(0xFFFF5252); // rojo
                    float cornerRadius = 16f;
                    if (dX > 0) {
                        // Swipe derecha
                        RectF bg = new RectF(itemView.getLeft(), itemView.getTop(),
                                itemView.getLeft() + dX, itemView.getBottom());
                        c.drawRoundRect(bg, cornerRadius, cornerRadius, paint);
                    } else if (dX < 0) {
                        // Swipe izquierda
                        RectF bg = new RectF(itemView.getRight() + dX, itemView.getTop(),
                                itemView.getRight(), itemView.getBottom());
                        c.drawRoundRect(bg, cornerRadius, cornerRadius, paint);
                    }
                    float alpha = 1f - Math.abs(dX) / (float) itemView.getWidth();
                    itemView.setAlpha(Math.max(0.2f, alpha));
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(rvScores);
    }

    /**
     * Recarga el cursor con los filtros y orden actuales.
     */
    private void refreshCursor() {
        // Nombre
        String nameFilter = etSearchName.getText().toString().trim();
        if (nameFilter.isEmpty()) nameFilter = null;

        // Score operator + value
        String scoreOp = null;
        Long scoreValue = null;
        String scoreText = etSearchScore.getText().toString().trim();
        if (!scoreText.isEmpty()) {
            try {
                scoreValue = Long.parseLong(scoreText);
                scoreOp = OPERATORS[spinnerOperator.getSelectedItemPosition()];
            } catch (NumberFormatException ignored) {
                // valor no numérico, ignorar filtro de score
            }
        }

        Cursor cursor = repo.getScoresCursor(nameFilter, scoreOp, scoreValue, currentOrderBy);
        adapter.swapCursor(cursor);

        // Mostrar/ocultar estado vacío
        int count = (cursor != null) ? cursor.getCount() : 0;
        if (count == 0) {
            rvScores.setVisibility(View.GONE);
            tvEmptyScores.setVisibility(View.VISIBLE);
        } else {
            rvScores.setVisibility(View.VISIBLE);
            tvEmptyScores.setVisibility(View.GONE);
        }
    }

    private void animateEntrance() {
        View root = findViewById(android.R.id.content);
        root.setAlpha(0f);
        root.animate()
                .alpha(1f)
                .setDuration(400)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cerrar cursor al destruir
        if (adapter != null) {
            adapter.swapCursor(null);
        }
    }
}

