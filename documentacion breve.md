# Documentación breve — GameHub (resumen técnico y ubicaciones)

Este documento breve describe de forma clara y directa la estructura del proyecto, qué hace cada módulo (Hub, Kaisen Clicker y 2048), dónde están los ficheros importantes (BD, imágenes) y cómo se guardan/leen las puntuaciones y el tiempo jugado.

---

## 1. Resumen rápido
- GameHub es un centro de juegos Android multi-módulo con un Hub (login, perfil, menús, puntuaciones, desafíos) y dos juegos integrados: **Kaisen Clicker** y **2048**.
- La base de datos SQLite y las capas de persistencia residen en el módulo `kaisenclicker_module`. Esa BD es la *global* de la app y almacena datos tanto de Kaisen Clicker como de 2048 (scores y claves `2048_*`).

---

## 2. Estructura principal y rutas (resumen)
- Hub (módulo `app`): `app/src/main/java/com/example/gamehub/`
  - LoginActivity, RegisterActivity, MainActivity (Hub), ProfileActivity, LeaderboardActivity, ScoresListActivity, ChallengesActivity, SessionManager, GameRepository, adapters y modelos.
- Kaisen Clicker: `kaisenclicker_module/src/main/java/com/example/kaisenclicker/`
  - `ui/activities/`, `ui/fragments/`, `model/`, `persistence/save/` (AppDatabaseHelper, SqlRepository, GameDataManager, UserRepository), `res/drawable/` (imágenes de enemigos, cofres, personajes, iconos de tienda).
- 2048: `2048_module/src/main/java/com/example/a2048/`
  - MainActivity (tablero), GameEngine, guardado de partida, y escritura de claves `2048_*` en la BD global.

---

## 3. Qué hace cada módulo (breve y directo)

### Hub (`app`)
- Login/Register: usan `UserRepository` (módulo Kaisen) para autenticar/crear usuarios (tabla `users` en la BD global).
- `SessionManager`: guarda sesión y datos de perfil en `SharedPreferences` (`GameHubSession.xml`). Ofrece: crear/terminar sesión, foto URI, estado, puntos totales, marcar inicio/fin de partida para contar tiempo jugado.
- MainActivity (Hub): muestra tarjeta de perfil (foto circular), grid de juegos, botones (Juegos, Puntuaciones, Desafíos, Perfil). Registra juegos en `GameRepository` y lanza las Activities de los juegos.
- Leaderboard / Scores: muestran historial de puntuaciones leyendo de la tabla `scores` de la BD global. Implementa búsqueda, ordenación y swipe-to-delete usando `Cursor` + `RecyclerView`.

### Kaisen Clicker
- Juego clicker con Activities/Fragments (Campaign, Shop, Chest, CharacterInventory, Statistics).
- Persistencia: `GameDataManager` (integra `SharedPreferences` y `SqlRepository`) y la BD `kaisen_clicker_<username>.db`.
- Guarda: estado del enemigo, clicks totales, daño total, cofres, personajes, mejoras, tiempo jugado del usuario, derrotas de bosses.
- Imágenes: muchas en `kaisenclicker_module/res/drawable/` (enemigos, cofres, retratos de personajes). Se cargan en ImageView/ShapeableImageView por recurso o por URI (foto de perfil).

### 2048
- Implementación típica del juego 4×4 con `GameEngine` puro.
- Guardado: guarda el estado en `SharedPreferences` de 2048 (savegame) y también escribe las claves `2048_score`, `2048_best_score`, `2048_moves`, `2048_seconds`, `2048_grid` en la BD global `kaisen_clicker_<username>.db` (para que el Hub pueda leerlas desde la misma base de datos).
- Cuando se obtiene un nuevo récord, inserta una fila en la tabla `scores` con `game_name = "2048"`.

---

## 4. Base de datos — ubicación y tablas clave

### ¿Dónde están los ficheros de BD en el dispositivo?
Los archivos `.db` se crean en la ruta de datos privada de la aplicación (en el dispositivo/emulador):

```
/data/data/com.example.gamehub/databases/
  ├─ kaisen_clicker.db                 ← BD principal (tabla `users` compartida)
  ├─ kaisen_clicker_<username>.db      ← BD por usuario que contiene progreso y claves `kv_store`
```

> Nota: la carpeta es privada y se pierde al desinstalar la app. Para inspeccionar: Android Studio → App Inspection → Database Inspector.

### Tablas importantes (creadas por `AppDatabaseHelper`)
- `users`: id, username, password_hash, created_at (usada por `UserRepository` para login/registro).
- `kv_store`: almacén clave-valor genérico (k, value_text, value_int, value_long, value_real). Aquí se guardan la mayoría de variables simples tanto de Kaisen como de 2048 (por ejemplo, `2048_score`).
- `characters`: estado de personajes del clicker (sukuna, gojo, etc.).
- `upgrades`: mejoras de la tienda.
- `skills`: habilidades y niveles.
- `enemies`: estado general de enemigos.
- `scores`: historial de puntuaciones de todos los juegos.

#### `scores` (esquema relevante)
```
CREATE TABLE scores (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER,
  player_name TEXT NOT NULL,
  game_name TEXT,
  score_value INTEGER NOT NULL,
  created_at INTEGER DEFAULT (strftime('%s','now')),
  extra TEXT
)
```
- `game_name` distingue `"Kaisen Clicker"` de `"2048"`.
- `extra` puede contener JSON con metadatos.

---

## 5. ¿De dónde se leen las puntuaciones para las pantallas de puntuaciones?
- La pantalla de Scores/Leaderboard usa `SqlRepository` sobre la BD global `kaisen_clicker_<username>.db` y lee la tabla `scores` mediante `Cursor`.
- Tanto las puntuaciones de Kaisen Clicker como las de 2048 se almacenan y se leen desde la misma BD global. Por tanto, la información visible en la pantalla de puntuaciones proviene de la BD situada en `kaisenclicker_module`.

---

## 6. Guardado de 2048: BD y savegame
- El módulo 2048 mantiene un `savegame` local (SharedPreferences o archivo propio) para restaurar el tablero completo y la sesión.
- Además, para centralizar las estadísticas en el Hub, 2048 escribe las claves resumen (`2048_score`, `2048_best_score`, `2048_moves`, `2048_seconds`) en la BD global (`kv_store`).
- Resultado: la puntuación del 2048 queda persistida tanto en el savegame (para reanudar la partida) como en la BD (para mostrar en el Hub/Leaderboards).

---

## 7. Dónde y cómo se cargan las imágenes (perfil, iconos, gifs, enemigos)
- Iconos de juegos y recursos (bundled): `app/src/main/res/drawable/` y `.../res/mipmap/`. Cargados con `setImageResource(R.drawable.xxx)` o `Glide/Picasso` si están presentes.
- Imágenes de Kaisen Clicker: `kaisenclicker_module/src/main/res/drawable/` (enemigos, bosses, cofres, personajes). Se referencian desde los fragments/activities del módulo Kaisen.
- Foto de perfil del usuario: seleccionada por el usuario desde galería → URI guardada en `SessionManager` (SharedPreferences) → cargada con `setImageURI(Uri.parse(photoUri))` en `ShapeableImageView`. Para que la imagen se ajuste perfectamente al círculo, la UI usa `ShapeableImageView` con `shapeAppearanceOverlay="@style/CircleImageView"` y `scaleType="centerCrop"`.
- GIFs/animaciones: para documentación conviene exportar a un frame estático en PDF; en la app pueden usarse con `Glide.with(context).asGif()` o `ImageView` que soporte `AnimatedImageDrawable` (según API).

---

## 8. Tiempo jugado (cómo se cuenta)
- El Hub y los juegos usan dos llamadas clave en `SessionManager`:
  - `markGameStarted()` guarda un timestamp al entrar a cualquier juego.
  - `markGameEnded()` calcula la diferencia con el timestamp y suma los segundos al acumulado `total_time_played` en `SharedPreferences`.
- `GameDataManager` en Kaisen Clicker también guarda `total_play_seconds` para el juego si se desea un contador por juego.
- Al volver al Hub se llama a `markGameEnded()` en `onResume()` para contabilizar el tiempo real jugado.

---

## 9. Dónde está el código de la base de datos en el proyecto (archivos clave)
- `kaisenclicker_module/src/main/java/com/example/kaisenclicker/persistence/save/AppDatabaseHelper.java`
- `kaisenclicker_module/src/main/java/com/example/kaisenclicker/persistence/save/SqlRepository.java`
- `kaisenclicker_module/src/main/java/com/example/kaisenclicker/persistence/save/GameDataManager.java`
- `kaisenclicker_module/src/main/java/com/example/kaisenclicker/persistence/save/UserRepository.java`

---

## 10. Recomendaciones / cosas por revisar (práctico)
- Asegúrate de que todas las escrituras de 2048 a la BD se hacen usando la instancia `SqlRepository` apuntando a `kaisen_clicker_<username>.db` para mantener todo centralizado.
- Verifica que `SessionManager.markGameStarted()` y `markGameEnded()` se llamen siempre al iniciar/terminar un juego (incluir también `onPause()`/`onStop()` si hace falta para sesiones interrumpidas).
- Para que la foto de perfil quede siempre exactamente recortada al círculo, usar `ShapeableImageView` + `android:scaleType="centerCrop"` y persistir permisos URI (takePersistableUriPermission).
- Si necesitas exportar la documentación en LaTeX/PDF, puedo generar `documentacion_breve.tex` a partir de este archivo.

---

## 11. Estado: lo que ya existe en tu repo (resumen)
- La persistencia SQL y repositorio (AppDatabaseHelper/SqlRepository) ya están implementados en `kaisenclicker_module`.
- El Hub ya integra `UserRepository` y lee/escribe datos de la BD global.
- 2048 ya escribe claves `2048_*` en la BD global (según la estructura existente) y además mantiene su propio savegame.

---


