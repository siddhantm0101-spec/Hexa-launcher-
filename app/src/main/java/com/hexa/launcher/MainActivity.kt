
package com.hexa.launcher

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.content.IntentFilter
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import android.provider.Settings
import android.text.TextWatcher
import android.text.Editable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppInfo(
    val title: String,
    val pkg: String,
    val icon: android.graphics.drawable.Drawable
)

class MainActivity : Activity() {

    private val pref by lazy {
        getSharedPreferences("hexa_settings", MODE_PRIVATE)
    }

    private var allApps: List<AppInfo> = emptyList()
    private lateinit var root: LinearLayout
    private lateinit var wallpaperView: ImageView
    private lateinit var appList: LinearLayout

    private var page = "home"
    private var touchX = 0f
    private var touchY = 0f

    private val dark get() = pref.getBoolean("dark", true)
    private val accent get() = pref.getInt("accent", 0xFF637DFF.toInt())
    private val columns get() = pref.getInt("columns", 4).coerceIn(3, 6)
    private val iconSize get() = pref.getInt("icon_size", 46).coerceIn(32, 64)
    private val showLabels get() = pref.getBoolean("labels", true)
    private val use24Hour get() = pref.getBoolean("clock24", true)
    private val wallpaperDim get() = pref.getInt("wallpaper_dim", 35).coerceIn(0, 85)

    private val bg get() =
        if (dark) 0xFF080D18.toInt() else 0xFFF1F3F9.toInt()

    private val surface get() =
        if (dark) 0xFF172235.toInt() else Color.WHITE

    private val fg get() =
        if (dark) Color.WHITE else 0xFF172033.toInt()

    private val muted get() =
        if (dark) 0xFF9AA9C1.toInt() else 0xFF68748A.toInt()

    private val hidden get() =
        pref.getStringSet("hidden", emptySet()) ?: emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadApps()
        home()
    }

    private fun loadApps() {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)

        allApps = packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_ALL
        ).mapNotNull {
            val p = it.activityInfo.packageName
            if (p == packageName) null
            else AppInfo(
                it.loadLabel(packageManager).toString(),
                p,
                it.loadIcon(packageManager)
            )
        }.distinctBy { it.pkg }
            .sortedBy { it.title.lowercase(Locale.getDefault()) }
    }

    private fun dp(n: Int): Int =
        (n * resources.displayMetrics.density).toInt()

    private fun shape(
        color: Int,
        radius: Int,
        stroke: Int? = null
    ) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun text(
        value: String,
        size: Float,
        color: Int = fg,
        bold: Boolean = false
    ) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        gravity = Gravity.CENTER_VERTICAL
        if (bold) {
            typeface = Typeface.create(
                "sans-serif-medium",
                Typeface.NORMAL
            )
        }
    }

    private fun base() {
        window.statusBarColor = bg
        window.navigationBarColor = bg

        window.decorView.systemUiVisibility =
            if (!dark) {
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                0
            }

        val frame = FrameLayout(this)

        wallpaperView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP

            val saved = pref.getString("wallpaper_uri", null)

            if (saved != null) {
                try {
                    setImageURI(Uri.parse(saved))
                } catch (_: Exception) {
                    setBackgroundColor(bg)
                }
            } else {
                setBackgroundColor(bg)
            }
        }

        frame.addView(
            wallpaperView,
            FrameLayout.LayoutParams(-1, -1)
        )

        val overlay = View(this).apply {
            val alpha = (wallpaperDim * 255 / 100)
            setBackgroundColor(
                Color.argb(alpha, 5, 10, 22)
            )
        }

        frame.addView(
            overlay,
            FrameLayout.LayoutParams(-1, -1)
        )

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(22), dp(18), dp(22), dp(18)
            )
            setBackgroundColor(Color.TRANSPARENT)
        }

        scroll.addView(root)
        frame.addView(
            scroll,
            FrameLayout.LayoutParams(-1, -1)
        )

        setContentView(frame)

        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchX = event.x
                    touchY = event.y
                    false
                }

                MotionEvent.ACTION_UP -> {
                    val dx = event.x - touchX
                    val dy = event.y - touchY

                    if (touchY > height() * 0.45f &&
                        dy < -dp(65)
                    ) {
                        drawer()
                    } else if (
                        touchX > width() - dp(35) &&
                        dx < -dp(45)
                    ) {
                        edgePanel()
                    }
                    false
                }

                else -> false
            }
        }
    }

    private fun height() =
        resources.displayMetrics.heightPixels.toFloat()

    private fun width() =
        resources.displayMetrics.widthPixels.toFloat()

    private fun header(
        title: String,
        subtitle: String? = null,
        back: Boolean = false
    ) {
        if (back) {
            val button = text("‹", 34f)
            button.setOnClickListener { home() }
            root.addView(button, LinearLayout.LayoutParams(-1, dp(42)))
        }

        root.addView(
            text(title, 30f, fg, true),
            LinearLayout.LayoutParams(-1, dp(43))
        )

        if (subtitle != null) {
            root.addView(
                text(subtitle, 13f, muted),
                LinearLayout.LayoutParams(-1, dp(25))
            )
        }
    }

    private fun home() {
        page = "home"
        base()

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val date = SimpleDateFormat(
            "EEEE, d MMMM",
            Locale.getDefault()
        ).format(Date())

        top.addView(
            text(date, 14f, muted),
            LinearLayout.LayoutParams(0, dp(35), 1f)
        )

        val gear = text("⚙", 25f)
        gear.gravity = Gravity.CENTER
        gear.setOnClickListener { settings() }
        top.addView(gear, LinearLayout.LayoutParams(dp(42), dp(42)))

        root.addView(top)

        val clockPattern = if (use24Hour) "HH:mm" else "hh:mm a"

        val clock = SimpleDateFormat(
            clockPattern,
            Locale.getDefault()
        ).format(Date())

        root.addView(
            text(clock, 58f, fg, true),
            LinearLayout.LayoutParams(-1, dp(77))
        )

        val hour = SimpleDateFormat(
            "HH",
            Locale.getDefault()
        ).format(Date()).toInt()

        val greeting = when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Welcome to HEXA"
        }

        root.addView(text("$greeting. Make room for what matters.", 14f, muted))

        val battery = registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val level = battery?.getIntExtra("level", -1) ?: -1
        val batteryText =
            if (level >= 0) "Battery  $level%" else "Your personal space"

        root.addView(
            text("◉   $batteryText", 12f, muted),
            LinearLayout.LayoutParams(-1, dp(32))
        )

        val gap = View(this)
        root.addView(gap, LinearLayout.LayoutParams(1, dp(16)))

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(21), dp(18), dp(21), dp(18))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    accent,
                    0xFF283D72.toInt(),
                    0xFF111B31.toInt()
                )
            ).apply {
                cornerRadius = dp(29).toFloat()
                setStroke(dp(1), 0x55FFFFFF)
            }
            elevation = dp(7).toFloat()
        }

        hero.addView(
            text("H E X A   /   SPACE", 12f, Color.WHITE, true)
        )

        hero.addView(
            text(
                "Your phone,\nyour world.",
                27f,
                Color.WHITE,
                true
            ),
            LinearLayout.LayoutParams(-1, dp(82))
        )

        hero.addView(
            text(
                "A smarter and calmer Android experience.",
                12f,
                0xFFE2E8FF.toInt()
            )
        )

        root.addView(
            hero,
            LinearLayout.LayoutParams(-1, dp(160))
        )

        val quick = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        quick.addView(
            chip("▧", "Wallpaper") { wallpaper() },
            LinearLayout.LayoutParams(0, dp(70), 1f)
        )

        quick.addView(
            chip("⌕", "All apps") { drawer() },
            LinearLayout.LayoutParams(0, dp(70), 1f).apply {
                leftMargin = dp(8)
            }
        )

        quick.addView(
            chip("◈", "Edge panel") { edgePanel() },
            LinearLayout.LayoutParams(0, dp(70), 1f).apply {
                leftMargin = dp(8)
            }
        )

        root.addView(
            quick,
            LinearLayout.LayoutParams(-1, dp(78)).apply {
                topMargin = dp(13)
            }
        )

        val search = EditText(this).apply {
            hint = "⌕   Search installed apps"
            setHintTextColor(muted)
            setTextColor(fg)
            textSize = 15f
            setSingleLine(true)
            isFocusable = false
            setPadding(dp(18), 0, dp(18), 0)
            background = shape(surface, 25, 0x334F65A0)

            setOnClickListener { drawer() }
        }

        root.addView(
            search,
            LinearLayout.LayoutParams(-1, dp(53)).apply {
                topMargin = dp(12)
            }
        )

        val favHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        favHeader.addView(
            text("FAVOURITES", 11f, muted, true),
            LinearLayout.LayoutParams(0, dp(38), 1f)
        )

        val edit = text("EDIT", 10f, accent, true)
        edit.setOnClickListener { manageFavourites() }
        favHeader.addView(edit)

        root.addView(favHeader)

        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(5), dp(4), dp(5))
            background = shape(surface, 26, 0x224F65A0)
        }

        favourites().take(6).forEach {
            dock.addView(
                iconTile(it),
                LinearLayout.LayoutParams(0, dp(83), 1f)
            )
        }

        root.addView(
            dock,
            LinearLayout.LayoutParams(-1, dp(90))
        )

        val footer = text(
            "SWIPE UP FOR APPS     •     HEXA 2.2",
            10f,
            muted
        )
        footer.gravity = Gravity.CENTER

        root.addView(
            footer,
            LinearLayout.LayoutParams(-1, dp(34))
        )
    }

    private fun chip(
        symbol: String,
        name: String,
        action: () -> Unit
    ) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        background = shape(surface, 19)

        addView(
            text(symbol, 22f, accent, true).apply {
                gravity = Gravity.CENTER
            }
        )

        addView(
            text(name, 11f, fg, true).apply {
                gravity = Gravity.CENTER
            }
        )

        setOnClickListener { action() }
    }

    private fun favourites(): List<AppInfo> {
        val saved = pref.getStringSet("fav", null)

        if (saved != null) {
            val list = saved.mapNotNull { p ->
                allApps.firstOrNull { it.pkg == p }
            }
            if (list.isNotEmpty()) return list
        }

        val keys = listOf(
            "phone", "message", "chrome",
            "camera", "whatsapp"
        )

        return keys.mapNotNull { key ->
            allApps.firstOrNull {
                it.title.contains(key, true)
            }
        }.distinctBy { it.pkg }.take(6)
    }

    private fun iconTile(app: AppInfo) =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER

            val icon = ImageView(this@MainActivity).apply {
                setImageDrawable(app.icon)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }

            addView(
                icon,
                LinearLayout.LayoutParams(
                    dp(iconSize),
                    dp(iconSize)
                )
            )

            if (showLabels) {
                addView(
                    text(app.title, 9f, fg).apply {
                        gravity = Gravity.CENTER
                        maxLines = 1
                    },
                    LinearLayout.LayoutParams(-1, dp(22))
                )
            }

            setOnClickListener { launch(app) }
            setOnLongClickListener {
                appMenu(app)
                true
            }
        }

    private fun drawer() {
        page = "drawer"
        base()
        header("All apps", "Everything in one place")

        val input = EditText(this).apply {
            hint = "Search apps"
            setHintTextColor(muted)
            setTextColor(fg)
            setSingleLine(true)
            textSize = 15f
            setPadding(dp(15), 0, dp(15), 0)
            background = shape(surface, 22)
        }

        root.addView(
            input,
            LinearLayout.LayoutParams(-1, dp(50)).apply {
                topMargin = dp(12)
                bottomMargin = dp(12)
            }
        )

        val scroll = ScrollView(this)

        appList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scroll.addView(appList)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        renderApps("")

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {
                renderApps(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun renderApps(query: String) {
        appList.removeAllViews()

        val found = allApps
            .filter {
                it.pkg !in hidden &&
                    it.title.contains(query, true)
            }
            .sortedBy { it.title.lowercase(Locale.getDefault()) }

        found.chunked(columns).forEach { group ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            group.forEach {
                row.addView(
                    iconTile(it),
                    LinearLayout.LayoutParams(0, dp(100), 1f)
                )
            }

            repeat(columns - group.size) {
                row.addView(
                    View(this),
                    LinearLayout.LayoutParams(0, dp(100), 1f)
                )
            }

            appList.addView(row)
        }

        if (found.isEmpty()) {
            appList.addView(
                text("No apps found", 14f, muted)
            )
        }
    }

    private fun appMenu(app: AppInfo) {
        val options = arrayOf(
            "Open app",
            "Add to favourites",
            "Remove from favourites",
            "Hide app",
            "App information"
        )

        android.app.AlertDialog.Builder(this)
            .setTitle(app.title)
            .setItems(options) { _, index ->
                when (index) {
                    0 -> launch(app)

                    1 -> {
                        val set = favourites()
                            .map { it.pkg }.toMutableSet()
                        set.add(app.pkg)
                        pref.edit().putStringSet("fav", set).apply()
                        Toast.makeText(
                            this, "Favourite added", Toast.LENGTH_SHORT
                        ).show()
                    }

                    2 -> {
                        val set = favourites()
                            .map { it.pkg }.toMutableSet()
                        set.remove(app.pkg)
                        pref.edit().putStringSet("fav", set).apply()
                    }

                    3 -> {
                        val set = hidden.toMutableSet()
                        set.add(app.pkg)
                        pref.edit().putStringSet("hidden", set).apply()
                        drawer()
                    }

                    4 -> {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        )
                        intent.data = Uri.parse("package:${app.pkg}")
                        startActivity(intent)
                    }
                }
            }.show()
    }

    private fun settings() {
        page = "settings"
        base()
        header("Settings", "Personalize your HEXA", true)

        setting("Appearance", "Switch between AMOLED dark and light") {
            pref.edit().putBoolean("dark", !dark).apply()
            settings()
        }

        setting("Accent colour", "Choose from six HEXA colours") {
            chooseAccent()
        }

        setting("App grid", "Choose 3, 4, 5 or 6 columns") {
            chooseGrid()
        }

        setting("Icon size", "Change app icon size") {
            chooseIconSize()
        }

        setting("App labels", "Show or hide app names") {
            pref.edit().putBoolean("labels", !showLabels).apply()
            settings()
        }

        setting("Clock format", "Switch between 12 and 24 hour") {
            pref.edit().putBoolean("clock24", !use24Hour).apply()
            settings()
        }

        setting("Wallpaper", "Choose your own Gallery image") {
            wallpaper()
        }

        setting("Wallpaper darkness", "Adjust wallpaper visibility") {
            chooseWallpaperDim()
        }

        setting("Favourite apps", "Manage home screen shortcuts") {
            manageFavourites()
        }

        setting("Hidden apps", "Show or restore hidden apps") {
            manageHidden()
        }

        setting("Edge panel", "Open quick access panel") {
            edgePanel()
        }

        setting("Android Quick Settings", "Open Android settings") {
            openSystem(Settings.ACTION_SETTINGS)
        }

        setting("Display settings", "Brightness and display options") {
            openSystem(Settings.ACTION_DISPLAY_SETTINGS)
        }

        setting("Sound settings", "Ringtone and volume options") {
            openSystem(Settings.ACTION_SOUND_SETTINGS)
        }

        setting("Battery settings", "Battery usage and power options") {
            openSystem(Intent.ACTION_POWER_USAGE_SUMMARY)
        }

        setting("Default launcher", "Set HEXA as your home app") {
            openSystem(Settings.ACTION_HOME_SETTINGS)
        }

        setting("Reset wallpaper", "Remove your selected wallpaper") {
            pref.edit().remove("wallpaper_uri").apply()
            home()
        }

        setting("Reset HEXA", "Restore default HEXA preferences") {
            android.app.AlertDialog.Builder(this)
                .setMessage("Reset all HEXA preferences?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reset") { _, _ ->
                    pref.edit().clear().apply()
                    home()
                }.show()
        }

        setting("About HEXA", "HEXA Launcher 2.2") {
            android.app.AlertDialog.Builder(this)
                .setTitle("HEXA 2.2")
                .setMessage(
                    "HEXA Launcher\nVersion 2.2\n\n" +
                    "A customizable Android home screen.\n" +
                    "Made for a cleaner Android experience."
                )
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun setting(
        title: String,
        description: String,
        action: () -> Unit
    ) {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = shape(surface, 18)

            addView(text(title, 15f, fg, true))
            addView(text(description, 11f, muted))

            setOnClickListener { action() }
        }

        root.addView(
            item,
            LinearLayout.LayoutParams(-1, dp(67)).apply {
                topMargin = dp(9)
            }
        )
    }

    private fun chooseAccent() {
        val names = arrayOf(
            "HEXA Blue",
            "Violet",
            "Mint",
            "Rose",
            "Cyber Cyan",
            "Amber"
        )

        val colors = intArrayOf(
            0xFF637DFF.toInt(),
            0xFF9B75E8.toInt(),
            0xFF35B99A.toInt(),
            0xFFE87598.toInt(),
            0xFF00BBD4.toInt(),
            0xFFFFAA32.toInt()
        )

        android.app.AlertDialog.Builder(this)
            .setTitle("Choose accent")
            .setItems(names) { _, index ->
                pref.edit().putInt("accent", colors[index]).apply()
                settings()
            }.show()
    }

    private fun chooseGrid() {
        val options = arrayOf(
            "3 columns",
            "4 columns",
            "5 columns",
            "6 columns"
        )

        android.app.AlertDialog.Builder(this)
            .setTitle("App grid")
            .setItems(options) { _, index ->
                pref.edit().putInt("columns", index + 3).apply()
                settings()
            }.show()
    }

    private fun chooseIconSize() {
        val options = arrayOf(
            "Small",
            "Normal",
            "Large",
            "Extra large"
        )

        val sizes = intArrayOf(36, 44, 52, 60)

        android.app.AlertDialog.Builder(this)
            .setTitle("Icon size")
            .setItems(options) { _, index ->
                pref.edit().putInt("icon_size", sizes[index]).apply()
                settings()
            }.show()
    }

    private fun chooseWallpaperDim() {
        val options = arrayOf(
            "No dim",
            "Light",
            "Medium",
            "Dark"
        )

        val values = intArrayOf(0, 20, 40, 65)

        android.app.AlertDialog.Builder(this)
            .setTitle("Wallpaper visibility")
            .setItems(options) { _, index ->
                pref.edit()
                    .putInt("wallpaper_dim", values[index])
                    .apply()
                home()
            }.show()
    }

    private fun manageHidden() {
        val apps = allApps.filter { it.pkg in hidden }

        if (apps.isEmpty()) {
            Toast.makeText(
                this, "No hidden apps", Toast.LENGTH_SHORT
            ).show()
            return
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("Tap an app to restore it")
            .setItems(apps.map { it.title }.toTypedArray()) { _, index ->
                val set = hidden.toMutableSet()
                set.remove(apps[index].pkg)
                pref.edit().putStringSet("hidden", set).apply()
                manageHidden()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun manageFavourites() {
        val apps = allApps.filter { it.pkg in favourites().map { a -> a.pkg } }

        if (apps.isEmpty()) {
            Toast.makeText(
                this, "No favourite apps", Toast.LENGTH_SHORT
            ).show()
            return
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("Remove a favourite")
            .setItems(apps.map { it.title }.toTypedArray()) { _, index ->
                val set = favourites().map { it.pkg }.toMutableSet()
                set.remove(apps[index].pkg)
                pref.edit().putStringSet("fav", set).apply()
                home()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun edgePanel() {
        page = "edge"
        base()
        header("HEXA Edge", "Your quick access panel", true)

        favourites().forEach { app ->
            setting(app.title, "Open application") {
                launch(app)
            }
        }

        setting("All apps", "Open app drawer") {
            drawer()
        }

        setting("HEXA settings", "Customize your launcher") {
            settings()
        }

        setting("Android Settings", "Open system settings") {
            openSystem(Settings.ACTION_SETTINGS)
        }
    }

    private fun wallpaper() {
        val pick = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }

        try {
            startActivityForResult(pick, 4102)
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Gallery picker unavailable",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @Deprecated("Deprecated")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 4102 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return

            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            pref.edit()
                .putString("wallpaper_uri", uri.toString())
                .apply()

            home()

            Toast.makeText(
                this,
                "Wallpaper updated",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openSystem(action: String) {
        try {
            startActivity(Intent(action))
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun launch(app: AppInfo) {
        packageManager.getLaunchIntentForPackage(app.pkg)?.let {
            startActivity(it)
        }
    }

    @Deprecated("Deprecated")
    override fun onBackPressed() {
        if (page != "home") {
            home()
        } else {
            super.onBackPressed()
        }
    }
}
