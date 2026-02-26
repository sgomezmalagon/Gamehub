# 🎮 GameHub — Centro de Juegos Android

Proyecto Android multi-módulo que funciona como un centro de juegos. Contiene un Hub principal (login, perfil, desafíos, puntuaciones) y dos juegos integrados: **Kaisen Clicker** y **2048**.

---

## MÓDULO APP — EL HUB (GameHub)

Ruta de todo el código: `app/src/main/java/com/example/gamehub/`

---

### LoginActivity.java

**Qué es:** La primera pantalla que ve el usuario al abrir la app.

**Qué hace paso a paso:**

1. Lo primero que hace en `onCreate()` es llamar a `SplashScreen.installSplashScreen(this)` — esto muestra la pantalla de carga nativa de Android durante unos instantes antes de mostrar el contenido.
2. Crea un `SessionManager` y comprueba `sessionManager.isLoggedIn()`. Si ya hay sesión activa (el usuario hizo login antes y no cerró sesión), salta directamente al Hub con `goToHub()` sin mostrar nada más.
3. Si NO hay sesión, muestra el layout `activity_login.xml` que tiene:
   - Un logo (`ic_gamehub_logo`), título "GameHub", subtítulo "Accede a tu cuenta"
   - Un campo `EditText` para el **username** (con icono de persona a la izquierda)
   - Un campo `EditText` para la **contraseña** (tipo `textPassword`, con icono de candado)
   - Un `TextView` de error (oculto por defecto, rojo)
   - Un botón "ENTRAR"
   - Un texto clickable "¿No tienes cuenta? Regístrate"
4. Cuando el usuario pulsa "ENTRAR", se ejecuta `attemptLogin()`:
   - Lee lo que escribió en los campos de usuario y contraseña
   - Si alguno está vacío → muestra error "Rellena todos los campos"
   - Llama a `userRepository.authenticateUser(username, password)` — esto busca en la tabla `users` de la base de datos SQLite si existe ese username con ese hash de contraseña
   - Si las credenciales son correctas → crea sesión con `sessionManager.createSession(username)` y va al Hub
   - Si son incorrectas → muestra error "Usuario o contraseña incorrectos" y hace una animación de shake (el botón se mueve de lado a lado rápidamente)
5. Si pulsa "¿No tienes cuenta?", abre `RegisterActivity` con transición fade.
6. `animateEntrance()` hace que toda la pantalla aparezca con fade-in (0 a 1 de opacidad en 500ms).

**Dónde está `UserRepository`:** NO está en este módulo. Está en `kaisenclicker_module/src/main/java/com/example/kaisenclicker/persistence/save/UserRepository.java`. El Hub importa esta clase del módulo Kaisen Clicker porque toda la base de datos vive allí.

---

### RegisterActivity.java

**Qué es:** Pantalla para crear una cuenta nueva.

**Qué hace paso a paso:**

1. Muestra un layout parecido al login pero con 3 campos: username, contraseña y confirmar contraseña.
2. Cuando pulsa "REGISTRARSE", ejecuta `attemptRegister()`:
   - Valida que ningún campo esté vacío
   - Valida que el username tenga al menos 3 caracteres
   - Valida que la contraseña tenga al menos 4 caracteres
   - Valida que contraseña y confirmación sean iguales
   - Llama a `userRepository.userExists(username)` para verificar que no exista ya
   - Si todo OK, llama a `userRepository.registerUser(username, password)` — esto inserta una fila en la tabla `users` de SQLite con el username en minúsculas y la contraseña hasheada con SHA-256
3. Si el registro es exitoso, muestra un Toast "¡Cuenta creada!" y redirige al Login.

---

### SessionManager.java

**Ruta:** `app/src/main/java/com/example/gamehub/auth/SessionManager.java`

**Qué es:** Clase que gestiona la sesión del usuario usando `SharedPreferences` (fichero `GameHubSession.xml` en `/data/data/com.example.gamehub/shared_prefs/`).

**Qué guarda y qué métodos tiene:**

| Método | Qué hace | Clave en SharedPreferences |
|---|---|---|
| `createSession(username)` | Marca que hay sesión activa y guarda el nombre. Si es la primera vez, guarda el timestamp de "miembro desde" | `is_logged_in`, `username`, `member_since` |
| `logout()` | Borra TODAS las SharedPreferences (cierra sesión) | — |
| `isLoggedIn()` | Devuelve `true` si hay sesión activa | `is_logged_in` |
| `getUsername()` | Devuelve el nombre del usuario logueado | `username` |
| `setStatus(status)` / `getStatus()` | Guarda/lee el estado: `"online"`, `"playing"` o `"away"` | `user_status` |
| `setPhotoUri(uri)` / `getPhotoUri()` | Guarda/lee la URI de la foto de perfil (seleccionada de la galería) | `photo_uri` |
| `getTotalPoints()` / `setTotalPoints()` / `addPoints()` | Lee/escribe/suma puntos totales del usuario | `total_points` |
| `getGamesPlayed()` / `incrementGamesPlayed()` | Lee/incrementa (+1) el contador de partidas jugadas | `games_played` |
| `getMemberSince()` | Devuelve el timestamp de cuándo se registró | `member_since` |
| `markGameStarted()` | Guarda el timestamp actual (= "ahora estoy dentro de un juego") | `game_start_time` |
| `markGameEnded()` | Calcula cuántos segundos pasaron desde `markGameStarted()`, los suma al total acumulado, y borra la marca de inicio. Tiene un tope de 24h para evitar datos corruptos | `game_start_time`, `total_time_played` |
| `getTotalTimePlayed()` | Devuelve el total de segundos jugados | `total_time_played` |

---

### MainActivity.java (Hub)

**Qué es:** La pantalla principal del Hub. Es lo que ve el usuario después del login.

**Qué hace paso a paso en `onCreate()`:**

1. Comprueba si hay sesión activa. Si no → vuelve al Login.
2. Llama a `registerGames()` — este método registra los dos juegos en el `GameRepository` (singleton):
   - Kaisen Clicker: con icono `kaisenclicker_module/res/drawable/kaisen_icon.png` y lanza `com.example.kaisenclicker.ui.activities.MainActivity`
   - 2048: con icono `2048_module/res/drawable/icon.png` y lanza `com.example.a2048.MainActivity`
3. Inicializa las vistas del layout `activity_main.xml`.
4. Configura la tarjeta de perfil, el menú y el RecyclerView.

**Lo que se ve en pantalla (de arriba a abajo):**

**Header:**
- Logo de GameHub (icono vectorial `ic_gamehub_logo.xml`) + texto "GameHub" + botón de logout (icono `ic_logout.xml`)
- Subtítulo "Bienvenido, [nombre]"
- Separador degradado morado→cyan (`bg_gradient_header.xml`)
- Texto "2 juego(s)"

**Tarjeta de perfil del usuario:**
- **Foto de perfil**: `ShapeableImageView` circular (48×48dp). Si el usuario eligió foto, se carga con `setImageURI(Uri.parse(photoUri))`. Si no, muestra `ic_profile.xml` (silueta gris). El círculo lo da `shapeAppearanceOverlay="@style/CircleImageView"` con borde morado.
- **Nombre**: lee `sessionManager.getUsername()`
- **Indicador de estado**: punto de color (8×8dp). Verde (#4CAF50) = online, amarillo (#FFAB00) = jugando, gris (#9E9E9E) = ausente + texto
- **Puntos totales**: lee `sessionManager.getTotalPoints()`
- Si pulsas la tarjeta entera → abre `ProfileActivity`

**4 botones de menú en fila horizontal:**
- **Juegos** (icono `ic_gamehub_logo`, morado): hace scroll al grid de juegos
- **Puntuaciones** (icono `ic_trophy`, cyan): abre `LeaderboardActivity`
- **Desafíos** (icono `ic_challenge`, amarillo): abre `ChallengesActivity`
- **Perfil** (icono `ic_profile`, gris): abre `ProfileActivity`

**Grid de juegos:**
- `RecyclerView` con `GridLayoutManager(2 columnas)`
- Cada juego es una tarjeta (`item_game_card.xml`) con: icono circular (80×80dp `ShapeableImageView`), nombre y descripción
- El adapter es `GameAdapter.java`
- Al hacer click en una tarjeta: guarda timestamp de inicio (`markGameStarted()`), incrementa partidas jugadas, y lanza la Activity del juego con `extra_username` en el Intent

**En `onResume()` (cuando vuelves de un juego):**
- Llama a `sessionManager.markGameEnded()` → calcula el tiempo jugado y lo acumula
- Refresca la tarjeta de perfil (por si cambiaste foto/estado)

**Botón logout:** muestra un `AlertDialog` "¿Estás seguro?" → si acepta, borra la sesión y vuelve al Login.

**Si no hay juegos registrados:** oculta el grid y muestra un estado vacío con icono de gamepad (`ic_gamepad_empty.xml`) y animación de "respiración" (pulso de tamaño infinito).

---

### GameRepository.java

**Ruta:** `app/src/main/java/com/example/gamehub/data/GameRepository.java`

**Qué es:** Singleton que almacena la lista de juegos disponibles en memoria.

- `registerGame(Game game)`: añade un juego a la lista (si su ID no existe ya)
- `getGames()`: devuelve la lista inmutable de juegos
- `getGameCount()`: devuelve cuántos juegos hay

---

### Game.java

**Ruta:** `app/src/main/java/com/example/gamehub/model/Game.java`

**Qué es:** Modelo que representa un juego. Campos:
- `id` (String): identificador único, ej `"kaisen_clicker"`
- `name` (String): nombre visible, ej `"Kaisen Clicker"`
- `iconRes` (int): recurso drawable del icono
- `description` (String): texto breve
- `activityClass` (Class<?>): la Activity que se lanza al pulsarlo

---

### GameAdapter.java

**Ruta:** `app/src/main/java/com/example/gamehub/adapter/GameAdapter.java`

**Qué es:** Adapter del RecyclerView del grid de juegos.

- En `onBindViewHolder()`: carga el icono con `setImageResource(game.getIconRes())`, el nombre y la descripción. Aplica animación de entrada escalonada (fade-in + slide-up + scale con `OvershootInterpolator`, delay de 80ms por posición).
- En `bind()`: al hacer click, registra el inicio de partida en `SessionManager`, incrementa las partidas jugadas, y lanza la Activity del juego con un Intent que incluye `extra_username`.

---

### ProfileActivity.java

**Qué es:** Pantalla de perfil del usuario.

**Lo que se ve y cómo funciona cada cosa:**

- **Botón atrás** (icono `ic_arrow_back.xml`): llama a `finish()` para cerrar la pantalla
- **Foto de perfil** (100×100dp circular): igual que en el Hub. Debajo hay un texto "Cambiar foto" que al pulsarlo abre la galería del móvil con `ActivityResultContracts.GetContent("image/*")`. Cuando eliges una foto, se intenta obtener permiso persistente con `takePersistableUriPermission()` y se guarda la URI en `SessionManager`.
- **Nombre**: lee `sessionManager.getUsername()`
- **Estado** (punto + texto, clickable): al pulsar, aparece un `AlertDialog` con 3 opciones: "En línea", "Jugando", "Ausente". Se guarda con `sessionManager.setStatus()`
- **Puntos totales**: lee `sessionManager.getTotalPoints()`, formato "X pts"
- **Partidas jugadas**: lee `sessionManager.getGamesPlayed()`
- **Tiempo jugado**: lee `sessionManager.getTotalTimePlayed()` y lo formatea como `HH:MM:SS`. Este tiempo es REAL — se mide con `markGameStarted()`/`markGameEnded()`
- **Miembro desde**: lee `sessionManager.getMemberSince()` y lo formatea como "Feb 2026"

---

### ChallengesActivity.java

**Qué es:** Pantalla con 8 desafíos/logros que el usuario puede completar.

**Cómo funciona:**

1. En `loadChallenges()` crea manualmente 8 objetos `Challenge` con los datos de progreso leídos de `SessionManager` (`getGamesPlayed()` y `getTotalPoints()`).
2. Cada `Challenge` tiene: id, título, descripción, icono, valor objetivo, valor actual y recompensa en puntos.
3. Los muestra en un `RecyclerView` con `LinearLayoutManager` y `ChallengeAdapter`.

**Los 8 desafíos:**

| Título | Objetivo | Recompensa |
|---|---|---|
| Primer Paso | Jugar 1 partida | 50 pts |
| Jugador Habitual | Jugar 5 partidas | 100 pts |
| Veterano | Jugar 10 partidas | 200 pts |
| Primeros Puntos | Acumular 100 puntos | 50 pts |
| Mil Puntos | Acumular 1.000 puntos | 150 pts |
| Maestro del GameHub | Acumular 5.000 puntos | 500 pts |
| Adicto al Juego | Jugar 25 partidas | 300 pts |
| Explorador | Jugar 2 juegos diferentes | 100 pts |

---

### Challenge.java

**Ruta:** `app/src/main/java/com/example/gamehub/model/Challenge.java`

Modelo de un desafío. Campos: `id`, `title`, `description`, `iconRes`, `targetValue`, `currentValue`, `pointsReward`, `completed` (boolean, calculado como `currentValue >= targetValue`).

Método `getProgressPercent()`: devuelve `(currentValue * 100) / targetValue` (clamped a 100).

---

### ChallengeAdapter.java

**Ruta:** `app/src/main/java/com/example/gamehub/adapter/ChallengeAdapter.java`

Adapter del RecyclerView de desafíos. En `bind()`:
- Carga el icono, título y descripción
- Pone la barra de progreso al porcentaje correspondiente
- Muestra "X/Y" como texto de progreso
- Si está completado: badge verde "¡Completado!" + icono tintado verde
- Si no: badge gris "+X pts" + icono tintado morado
- Animación de entrada: slide-in desde la izquierda con `OvershootInterpolator`

---

### LeaderboardActivity.java

**Ruta:** `app/src/main/java/com/example/gamehub/leaderboard/LeaderboardActivity.java`

**Qué es:** Pantalla de puntuaciones con dos tabs.

**Cómo funciona:**

- Tiene dos tabs de texto: "Kaisen Clicker" y "2048". Al pulsar uno, muestra su ScrollView y oculta el otro. El tab activo tiene fondo degradado (`bg_tab_active`) y texto blanco; el inactivo tiene fondo oscuro y texto gris.
- Tiene un botón "Ver historial de puntuaciones" que abre `ScoresListActivity`.

**Tab Kaisen Clicker — `loadKaisenData()`:**
Crea un `GameDataManager(this, username)` y lee directamente de la base de datos:
- `gdm.getEnemyLevel()` → muestra "Nivel del enemigo"
- `gdm.getTotalClicks()` → muestra "Clicks totales"
- `gdm.getTotalDamage()` → muestra "Daño total"
- `gdm.getEnemiesDefeated()` → muestra "Enemigos derrotados"
- `gdm.getBossesDefeated()` → muestra "Bosses derrotados"
- `gdm.getCursedEnergy()` → muestra "Energía maldita"
- `gdm.getCharacterLevel()` → muestra "Nivel del personaje"
- `gdm.getTotalPlaySeconds()` → muestra "Tiempo jugado" (formateado HH:MM:SS)

**Tab 2048 — `load2048Data()`:**
Crea un `SqlRepository(this, "kaisen_clicker_" + username + ".db")` y lee de la tabla `kv_store`:
- `repo.getInt("2048_score", 0)` → "Puntuación actual"
- `repo.getInt("2048_best_score", 0)` → "Mejor puntuación"
- `repo.getInt("2048_moves", 0)` → "Movimientos"
- `repo.getInt("2048_seconds", 0)` → "Tiempo"

Los números grandes se formatean: 1.500 → "1.5K", 2.000.000 → "2.0M".

---

### ScoresListActivity.java

**Ruta:** `app/src/main/java/com/example/gamehub/leaderboard/ScoresListActivity.java`

**Qué es:** Pantalla con el historial completo de puntuaciones con búsqueda, ordenación y borrado.

**Cómo funciona:**

1. Crea un `SqlRepository` apuntando a `kaisen_clicker_<username>.db`.
2. Tiene un campo de texto para buscar por nombre y otro para buscar por valor de puntuación + un Spinner con operadores (`=`, `>`, `<`, `>=`, `<=`).
3. Tiene 3 botones de ordenación: "Nombre" (`player_name ASC`), "Puntuación" (`score_value DESC`, por defecto), "Fecha" (`created_at DESC`). El activo se resalta con fondo degradado.
4. `refreshCursor()` construye dinámicamente una query SQL con los filtros y llama a `repo.getScoresCursor(nameFilter, scoreOp, scoreValue, currentOrderBy)` que devuelve un `Cursor`.
5. El `Cursor` se pasa al adapter con `adapter.swapCursor(cursor)`.
6. **Swipe to delete**: implementado con `ItemTouchHelper`. Al deslizar una tarjeta, pinta un fondo rojo, llama a `repo.deleteScoreById(id)`, refresca el cursor y muestra un Snackbar "Puntuación eliminada".

---

### ScoresCursorAdapter.java

**Ruta:** `app/src/main/java/com/example/gamehub/leaderboard/ScoresCursorAdapter.java`

**Qué es:** Adapter de RecyclerView que usa un `Cursor` de SQLite.

- `onBindViewHolder()`: mueve el cursor a la posición con `cursor.moveToPosition(position)` y lee las columnas `player_name`, `game_name`, `score_value` y `created_at`.
- Cada tarjeta (layout `item_score_card.xml`, que es un `CardView`) muestra: "Nombre • Juego", puntuación en morado grande, y fecha formateada.
- Al hacer click en la tarjeta → abre `ScoreDetailActivity` pasando el `id` de la puntuación.
- `swapCursor(Cursor c)`: cierra el cursor anterior y carga el nuevo, llama a `notifyDataSetChanged()`.
- `getIdAt(position)`: devuelve el `id` de la fila en esa posición (usado para el swipe delete).

---

### ScoreDetailActivity.java

**Ruta:** `app/src/main/java/com/example/gamehub/leaderboard/ScoreDetailActivity.java`

**Qué es:** Pantalla de detalle de una puntuación individual.

**Cómo funciona:**
1. Recibe el `id` de la puntuación por Intent extra.
2. Llama a `repo.getScoreById(id)` para obtener el Cursor con esa fila.
3. Lee `player_name`, `score_value`, `created_at`, `game_name`.
4. Si `game_name` es null, intenta parsear el JSON de la columna `extra` buscando `"game"`.
5. Muestra: nombre del jugador, nombre del juego, puntuación y fecha formateada.

---

---

## MÓDULO KAISEN CLICKER

Ruta de todo el código: `kaisenclicker_module/src/main/java/com/example/kaisenclicker/`

---

### ui/activities/MainActivity.java (Kaisen Clicker)

**Qué es:** La Activity principal del juego Kaisen Clicker. Se abre cuando pulsas la tarjeta "Kaisen Clicker" en el Hub.

**Qué hace paso a paso:**

1. En `onCreate()`: lee el `extra_username` del Intent (para saber qué usuario está jugando).
2. Crea un `GameDataManager(this, currentUsername)` — esto abre/crea la BD `kaisen_clicker_<usuario>.db` y las SharedPreferences `KaisenClickerData_<usuario>`.
3. Carga datos guardados: energía maldita y si tiene personajes desbloqueados.
4. Inicializa las 5 `MaterialCardView` de la barra de navegación inferior.
5. Muestra el `CampaignFragment` por defecto.

**Layout** (`activity_kaisen_main.xml`): simplísimo — un `FrameLayout` (contenedor de fragments) + un `include` de `bottom_navigation_custom.xml`.

**Barra de navegación inferior** (`bottom_navigation_custom.xml`):
Son 5 botones circulares (`MaterialCardView` con `cardCornerRadius=30dp`), fondo `#2D3E50`, borde `#4A5F8E`:

| Pos | Icono | Archivo | Abre |
|---|---|---|---|
| 1 | Flecha arriba | `ic_arrow_up` (vector) | `ShopFragment` (Tienda) |
| 2 | Cofre | `chest.png` (PNG) | `ChestFragment` (Cofres) |
| 3 | Espadas | `battle_icon.png` (PNG) | `CampaignFragment` (Combate) — por defecto |
| 4 | Personaje | `character_menu.png` (PNG) | `CharacterInventoryFragment` (Inventario) |
| 5 | Trofeo | `ic_trophy` (vector) | `StatisticsFragment` (Estadísticas) |

**`selectNavItem()`**: el botón seleccionado se agranda de 60dp a 72dp, el borde cambia a dorado (`#FFB74D`), la elevación sube a 12dp, y tiene animación de rebote (`OvershootInterpolator`). Los demás vuelven a tamaño/color normal.

**Cada botón** llama a `getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).addToBackStack(null).commit()` para mostrar el fragment correspondiente. La excepción es Campaign que primero hace `popBackStack(null, POP_BACK_STACK_INCLUSIVE)` para limpiar la pila.

**Getters expuestos a los fragments:**
- `getCursedEnergy()` / `addCursedEnergy(amount)`: la Activity mantiene la energía maldita en memoria y la guarda con `gameDataManager.saveCursedEnergy()`.
- `getGameDataManager()`: los fragments acceden a la BD a través de este método.
- `getCurrentUsername()`: devuelve el nombre de usuario.

---

### ui/fragments/CampaignFragment.java

**Qué es:** La pantalla principal del juego donde haces click para atacar enemigos. Es el archivo MÁS grande del proyecto.

**Layout** (`fragment_campaign.xml`):
- **Fondo**: imagen `shibuya.webp` (una imagen de la ciudad de Shibuya del anime)
- **Barra de vida** (`HpBarComponent`): componente custom que muestra nombre del enemigo, nivel, barra de HP con degradado verde→amarillo→rojo, y barra de armadura
- **Imagen del enemigo**: `ImageView` dentro de un `MaterialCardView` cuadrado centrado. La imagen cambia según el enemigo actual
- **Popup de daño**: `TextView` "-150" en rojo que aparece y se desvanece al atacar
- **Display de energía** (esquina superior derecha): `MaterialCardView` con icono `energy_coin.png` + cantidad de energía maldita
- **Barra de habilidades**: 4 botones `SkillButtonView` en la parte inferior

**Sistema de combate:**
- El jugador toca la imagen del enemigo → se calcula el daño → se resta del HP del enemigo
- Si HP llega a 0 → enemigo derrotado → sube el nivel → aparece nuevo enemigo con más HP → se gana energía maldita
- Cada ciertos niveles aparece un **Boss** con más HP, armadura, y mecánicas especiales

**Enemigos (imágenes en `kaisenclicker_module/res/drawable/`):**
- `yusepe.png` — enemigo básico
- `damage_yusepe.png` — versión dañada
- `choso_boss.webp` — boss Choso (primera fase)
- `choso_boss_second_phase.jpeg` — segunda fase de Choso
- `damaged_choso_boss.png` — Choso dañado
- `mahito.png` — boss Mahito
- `mahito_true_form.png` — Mahito transformado
- `damaged_mahito.png` — Mahito dañado
- `mahoraga_boss.png` — boss Mahoraga

**Datos que guarda al salir:**
- Nivel del enemigo, HP y armadura actuales, estado de fases de bosses
- Clicks totales, daño total, tiempo jugado, enemigos/bosses derrotados

---

### ui/fragments/ShopFragment.java

**Qué es:** Tienda donde gastas energía maldita para mejorar estadísticas.

**4 mejoras disponibles:**

| Mejora | Icono | Qué hace |
|---|---|---|
| Tap Damage | `clicks.png` | Aumenta el daño base por click |
| Auto Clicker | `autoclicker.png` | Genera clicks automáticos |
| Black Flash | `blackflash.png` | Aumenta probabilidad y daño de críticos |
| Energy Boost | `energy_boost.png` | Aumenta la energía ganada por enemigo |

Cada mejora tiene nivel, coste (que escala con el nivel) y un botón de compra (`buy_button.png`). Al comprar, se resta la energía maldita y se sube el nivel de la mejora.

Los niveles se guardan en la tabla `upgrades` de SQLite vía `gameDataManager.saveTapDamageLevel()`, etc.

---

### ui/fragments/ChestFragment.java

**Qué es:** Pantalla para abrir cofres que se obtienen al derrotar bosses.

**Cómo funciona:**
- Muestra la imagen del cofre (`chest.png`) y cuántos cofres tienes
- Al pulsar "Abrir", hay un 30% de probabilidad de desbloquear un personaje (Sukuna o Gojo) si aún no los tienes. Si se desbloquea, muestra el `RareSummonDialogFragment` con animación
- Si no desbloqueas personaje, recibes energía maldita (cantidad escalada según tu nivel de enemigo: base 50-200, +25% por cada 10 niveles)

---

### ui/fragments/CharacterInventoryFragment.java

**Qué es:** Pantalla de inventario de personaje con stats y habilidades.

**Lo que muestra:**
- Imagen del personaje seleccionado: Sukuna (`sukunapfp.jpg`) o Gojo (`gojo_character.png`)
- Nivel del personaje, XP, barra de progreso de XP
- Poder total calculado
- Lista de habilidades con su nivel y botón para mejorarlas

**Personajes:**
- **Ryomen Sukuna** (id=1): habilidades Cleave, Dismantle, Fuga, Expansión de Dominio
- **Satoru Gojo** (id=2): habilidades Amplificación Azul, Ritual Inverso Rojo, Vacío Púrpura, Expansión de Dominio

**Imágenes de habilidades:**
- `cleave_image.png`, `dismanteal.png`, `fuga_image.png` — habilidades de Sukuna
- `sukuna_domain.jpeg` — dominio de Sukuna
- `blue_skill.jpeg`, `red_skill.jpeg`, `hollow_purple.jpeg` — habilidades de Gojo
- `gojo_domain.jpeg` — dominio de Gojo

Los datos de personajes se guardan en la tabla `characters` y de habilidades en la tabla `skills` de SQLite.

---

### ui/fragments/StatisticsFragment.java

**Qué es:** Pantalla de estadísticas detalladas (usa DataBinding).

**Lo que muestra:** DPS medio, daño total, clicks totales, enemigos derrotados, bosses derrotados, personajes desbloqueados (con barra de progreso).

Tiene un botón de **reset** que llama a `gameDataManager.resetAllData()` y pone todas las estadísticas a 0.

---

### ui/components/HpBarComponent.java

**Qué es:** Componente custom (extiende `LinearLayout`) que dibuja la barra de vida del enemigo.

**Layout interno** (`hp_bar_component.xml`): nombre del enemigo, icono de boss (oculto por defecto), nivel, label "HEALTH", ProgressBar horizontal de 28dp, y una ProgressBar de armadura debajo.

**Funcionalidades:**
- Degradado dinámico: verde si HP > 50%, amarillo si 20-50%, rojo si < 20%
- Animaciones suaves con `ObjectAnimator` al recibir daño (600ms con `DecelerateInterpolator`)
- Barra de armadura separada (amarilla, se muestra solo en bosses)

---

### ui/components/SkillButtonView.java

**Qué es:** Componente custom (extiende `FrameLayout`) que es un botón de habilidad circular.

**Layout interno** (`skill_button.xml`): fondo circular, icono de la habilidad (48×48dp con recorte circular), overlay de cooldown oscuro, texto del cooldown.

**Funcionamiento:**
- Al pulsar una habilidad: ejecuta la acción + activa el cooldown
- Durante el cooldown: muestra un overlay oscuro semitransparente + un número que cuenta atrás los segundos restantes
- El icono se pone en escala de grises durante el cooldown
- Tooltip al mantener pulsado: muestra la descripción de la habilidad

---

### ui/components/SkillData.java

**Qué es:** Modelo simple que almacena datos de una habilidad para la UI: icono, cooldown y nivel.

`getCooldown()` calcula el cooldown real: `cooldown × 0.9^(nivel-1)` — es decir, se reduce un 10% por cada nivel.

---

### ui/dialogs/RareSummonDialogFragment.java

**Qué es:** Diálogo a pantalla completa que aparece al desbloquear un personaje abriendo un cofre.

**Animación:**
1. Imagen de invocación (`ivRareSummon`) aparece con fade-in + scale + rotación + rebote
2. Al terminar, muestra un número que cuenta de 0 hasta la cantidad de energía maldita ganada
3. La imagen se desvanece y el diálogo se cierra solo

---

### model/skill/Skill.java

**Qué es:** Modelo de una habilidad. Campos: `id`, `name`, `description`, `type` (NORMAL_1/NORMAL_2/NORMAL_3/ULTIMATE), `maxLevel`, `level`, `unlocked`, `cooldownMs`, y parámetros de sangrado (bleed): `bleedDurationMs`, `bleedTickMs`, `bleedBaseFactor`, `bleedPerLevelFactor`.

---

### model/character/CharacterSkillManager.java

**Qué es:** Inicializa las 4 habilidades del personaje en memoria:

| Habilidad | Tipo | Cooldown | Efecto |
|---|---|---|---|
| Cleave | Normal 1 | 2s | 100% + 20%/nivel + sangrado 5s |
| Dismantle | Normal 2 | 3s | 150% + 30%/nivel, reduce defensa |
| Fuga | Normal 3 | 4s | 80% + 15%/nivel, esquiva |
| Expansión de Dominio | Ultimate | 8s | 300% + 50%/nivel |

---

---

## BASE DE DATOS — Dónde está y cómo funciona

### Dónde está el código

**Todo el código de la base de datos está dentro de `kaisenclicker_module`:**

```
kaisenclicker_module/src/main/java/com/example/kaisenclicker/persistence/save/
├── AppDatabaseHelper.java   ← Crea las tablas SQLite
├── SqlRepository.java       ← Operaciones de lectura/escritura
├── GameDataManager.java     ← Capa de alto nivel (SharedPreferences + SQL)
└── UserRepository.java      ← Registro y login de usuarios
```

Aunque vive en el módulo Kaisen Clicker, es la **base de datos GLOBAL** de toda la app. El Hub y el 2048 importan estas clases porque `app/build.gradle.kts` tiene `implementation(project(":kaisenclicker_module"))`.

### Dónde se guardan los archivos en el dispositivo

Los ficheros `.db` se crean automáticamente en el almacenamiento privado de la app:

```
/data/data/com.example.gamehub/databases/
├── kaisen_clicker.db              ← Tabla 'users' (compartida, para login/registro)
├── kaisen_clicker_sergio.db       ← Todo el progreso del usuario "sergio"
├── kaisen_clicker_maria.db        ← Todo el progreso del usuario "maria"
└── ...
```

Esta carpeta es **privada** — solo la app puede acceder. Si desinstalan la app, se pierde todo. Los ficheros `SharedPreferences` están en `/data/data/com.example.gamehub/shared_prefs/`.

Para inspeccionar la BD durante el desarrollo: **Android Studio → View → Tool Windows → App Inspection → Database Inspector**.

---

### AppDatabaseHelper.java

Extiende `SQLiteOpenHelper`. Constantes: `DATABASE_NAME = "kaisen_clicker.db"`, `DATABASE_VERSION = 4`.

Tiene dos constructores:
- Sin nombre → crea `kaisen_clicker.db` (para la tabla `users`)
- Con nombre → crea `kaisen_clicker_<usuario>.db` (para todo lo demás)

**`onCreate()` crea 7 tablas:**

**1. `users`** — Usuarios registrados
```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    created_at INTEGER DEFAULT (strftime('%s','now'))
)
```

**2. `kv_store`** — Almacén clave-valor genérico (guarda cualquier dato simple)
```sql
CREATE TABLE kv_store (
    k TEXT PRIMARY KEY,       -- la clave (se usa 'k' porque 'key' es palabra reservada SQL)
    value_text TEXT,          -- para strings
    value_int INTEGER,        -- para enteros
    value_long INTEGER,       -- para longs
    value_real REAL           -- para decimales
)
```

**3. `characters`** — Personajes (2 filas por defecto: Sukuna id=1, Gojo id=2, ambos bloqueados)
```sql
CREATE TABLE characters (id INTEGER PRIMARY KEY, unlocked INTEGER DEFAULT 0, level INTEGER DEFAULT 1, xp INTEGER DEFAULT 0)
INSERT OR IGNORE INTO characters VALUES (1, 0, 1, 0)  -- Sukuna
INSERT OR IGNORE INTO characters VALUES (2, 0, 1, 0)  -- Gojo
```

**4. `upgrades`** — Mejoras de la tienda
```sql
CREATE TABLE upgrades (id TEXT PRIMARY KEY, level INTEGER DEFAULT 0, purchased INTEGER DEFAULT 0)
```

**5. `skills`** — Habilidades de personajes
```sql
CREATE TABLE skills (id TEXT PRIMARY KEY, character_id INTEGER, unlocked INTEGER DEFAULT 0, level INTEGER DEFAULT 0)
```

**6. `enemies`** — Estado del enemigo (1 fila por defecto: nivel 1, 0 derrotados)
```sql
CREATE TABLE enemies (id INTEGER PRIMARY KEY AUTOINCREMENT, enemy_level INTEGER DEFAULT 1, defeated_count INTEGER DEFAULT 0)
INSERT OR IGNORE INTO enemies (enemy_level, defeated_count) VALUES (1, 0)
```

**7. `scores`** — Puntuaciones de TODOS los juegos
```sql
CREATE TABLE scores (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    player_name TEXT NOT NULL,
    game_name TEXT,               -- "2048" o "Kaisen Clicker"
    score_value INTEGER NOT NULL,
    created_at INTEGER DEFAULT (strftime('%s','now')),
    extra TEXT                    -- JSON libre, ej: {"game":"2048"}
)
```

**`onOpen()`**: repite todos los `CREATE TABLE IF NOT EXISTS` e `INSERT OR IGNORE` como protección — si alguna tabla se corrompió, se recrea.

**`onUpgrade()`**: borra todo y vuelve a crear (pérdida de datos al cambiar versión).

---

### SqlRepository.java

Es la capa de acceso a datos. Cada método abre la BD, ejecuta la query y devuelve el resultado.

**Operaciones clave-valor (`kv_store`):**
- `putInt(key, value)` → `INSERT OR REPLACE INTO kv_store (k, value_int) VALUES (?, ?)`
- `getInt(key, default)` → `SELECT value_int FROM kv_store WHERE k = ?`
- Lo mismo para `putLong/getLong`, `putString/getString`
- `incrementIntKV(key, delta)` → Lee el valor actual, le suma delta, escribe el nuevo. Todo dentro de una **transacción** (`beginTransaction/setTransactionSuccessful/endTransaction`) para que sea atómico

Cada método tiene un **sistema de retry**: si falla, llama a `ensureKvStoreExists()` (que ejecuta `CREATE TABLE IF NOT EXISTS`) y reintenta una vez.

**Operaciones de puntuaciones (`scores`):**
- `insertScore(userId, playerName, gameName, scoreValue, extraJson)` → `INSERT INTO scores ...`
- `deleteScoreById(id)` → `DELETE FROM scores WHERE id = ?`
- `getScoresCursor(nameFilter, scoreOp, scoreValue, orderBy)` → Construye dinámicamente una query SELECT con los filtros (WHERE player_name LIKE..., AND score_value > ...) y devuelve un `Cursor`
- `getScoreById(id)` → `SELECT ... FROM scores WHERE id = ?`

**Otras operaciones:**
- `getEnemyLevel()` / `setEnemyLevel()` → tabla `enemies`, fila id=1
- `incrementEnemiesDefeated()` → transacción atómica sobre `enemies.defeated_count`
- `upsertCharacter(id, unlocked, level, xp)` → `INSERT OR REPLACE INTO characters ...`
- `getAllCharacters()` → `SELECT * FROM characters`, devuelve `Map<Integer, CharacterRecord>`
- `setUpgradeLevel(id, level)` / `getUpgradeLevel(id)` → tabla `upgrades`
- `upsertSkill(skillId, characterId, unlocked, level)` → tabla `skills`
- `getSkillLevel(skillId, characterId)` → busca primero por characterId, si no hay cae a global
- `recordBossDefeat(level, bossId)` → crea tabla `bosses` si no existe e inserta fila con timestamp
- `setCurrentEnemyState()` / `getCurrentEnemyLevel()` / etc. → tabla `enemy_state` (creada dinámicamente)

---

### GameDataManager.java

Es la **interfaz principal** que usan todos los fragments y activities para leer/escribir datos. Combina **SharedPreferences** y **SqlRepository**.

**Constructor:**
```java
new GameDataManager(context, "sergio")
```
1. Crea SharedPreferences con nombre `KaisenClickerData_sergio`
2. Crea SqlRepository apuntando a `kaisen_clicker_sergio.db`
3. Ejecuta la migración de SharedPreferences a SQL si es la primera vez
4. Carga los niveles de habilidades al SkillManager en memoria
5. Hace un health check escribiendo y leyendo un valor de test

**Sistema de doble escritura — AL ESCRIBIR:** siempre escribe en AMBOS sitios:
```java
public void saveCursedEnergy(int energy) {
    prefs.edit().putInt("cursed_energy", energy).apply();     // SharedPreferences
    try { repository.putInt("cursed_energy", energy); } catch (Exception ignored) {}  // SQLite
}
```

**AL LEER:** si la migración a SQL está completa, lee de SQLite; si no, de SharedPreferences:
```java
public int getCursedEnergy() {
    if (useSql()) return repository.getInt("cursed_energy", prefs.getInt("cursed_energy", 0));
    return prefs.getInt("cursed_energy", 0);
}
```

**Claves que usa en `kv_store` del Kaisen Clicker:**
`cursed_energy`, `character_unlocked`, `enemy_level`, `total_clicks`, `total_damage`, `total_play_seconds`, `enemies_defeated`, `bosses_defeated`, `characters_unlocked_count`, `chest_count`, `peak_dps`, `ulti_progress`, `selected_character_id`, `character_level`, `character_xp`, `current_enemy_hp`, `current_enemy_armor`, `choso_second_phase`, `mahito_transformed`, `current_enemy_id`

**Claves que usa en `kv_store` del 2048** (escritas por el módulo 2048):
`2048_score`, `2048_best_score`, `2048_moves`, `2048_seconds`, `2048_grid`

---

### UserRepository.java

Usa SIEMPRE la BD por defecto `kaisen_clicker.db` (sin usuario), porque la tabla `users` es compartida.

- `registerUser(username, password)`: convierte username a minúsculas, hashea la contraseña con SHA-256, inserta en la tabla `users`
- `authenticateUser(username, password)`: hashea la contraseña, busca en la BD si existe esa combinación
- `userExists(username)`: comprueba si el username ya existe
- `hashPassword(password)`: usa `MessageDigest.getInstance("SHA-256")` para generar el hash hexadecimal. La contraseña **nunca se guarda en texto plano**

---

---

## MÓDULO 2048 (resumen breve)

Ruta: `2048_module/src/main/java/com/example/a2048/`

- **MainActivity.java**: Activity principal. Tablero 4×4 (`GridLayout`), puntuación, temporizador, gestos swipe, modos Normal/Blitz. Guarda datos en SharedPreferences (`2048_SaveGame_<usuario>`) Y en la BD global (`kaisen_clicker_<usuario>.db`) con las claves `2048_*`. Cuando hay nuevo récord, inserta en la tabla `scores` con `game_name = "2048"`.
- **GameEngine.java**: Motor lógico puro (matriz `int[4][4]`). `moveLeft/Right/Up/Down()`, `compressAndMerge()`, `spawnRandom()` (90% un 2, 10% un 4).
- **GameModeDialog.java**: Diálogo para elegir Normal (sin límite) o Blitz (5 min).
- **OnSwipeTouchListener.java**: Detector de gestos (umbral: 100px distancia, 100px/s velocidad).

