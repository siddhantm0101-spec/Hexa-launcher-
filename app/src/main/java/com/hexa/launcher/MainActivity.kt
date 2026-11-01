package com.hexa.launcher

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.MotionEvent
import android.widget.*
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
    private lateinit var appList: LinearLayout
    private var page = "home"

    private var startX = 0f
    private var startY = 0f

    private val dark get() = pref.getBoolean("dark", true)
    private val accent get() = pref.getInt("accent", 0xFF6685FF.toInt())
    private val columns get() = pref.getInt("columns", 4).coerceIn(3, 6)
    private val iconSize get() = pref.getInt("icon_size", 46).coerceIn(32, 64)
    private val showLabels get() = pref.getBoolean("labels", true)
    private val clock24 get() = pref.getBoolean("clock24", true)
    private val wallpaperDim get() = pref.getInt("wallpaper_dim", 30).coerceIn(0, 85)

    private val bg get() =
        if (dark) 0xFF05070D.toInt() else 0xFFF2F4FA.toInt()

    private val surface get() =
        if (dark) 0xFF151E30.toInt() else Color.WHITE

    private val surface2 get() =
        if (dark) 0xFF1C2940.toInt() else 0xFFE8EDF7.toInt()

    private val fg get() =
        if (dark) Color.WHITE else 0xFF172033.toInt()

    private val muted get() =
        if (dark) 0xFFA0ADC3.toInt() else 0xFF65728A.toInt()

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
            val pkg = it.activityInfo.packageName
            if (pkg == packageName) null
            else AppInfo(
                it.loadLabel(packageManager).toString(),
                pkg,
                it.loadIcon(packageManager)
            )
        }.distinctBy { it.pkg }
            .sortedBy { it.title.lowercase(Locale.getDefault()) }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun shape(
        color: Int,
        radius: Int,
        stroke: Int? = null
    ) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun gradient(
        colors: IntArray,
        radius: Int
    ) = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        colors
    ).apply {
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), 0x35FFFFFF)
    }

    private fun txt(
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

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(20), dp(15), dp(20), dp(12)
            )
            setBackgroundColor(bg)
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(root)
        }

        setContentView(scroll)
    }

    private fun header(
        title: String,
        subtitle: String? = null,
        back: Boolean = false
    ) {
        if (back) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val backText = txt("‹", 34f, accent)
            backText.gravity = Gravity.CENTER
            backText.setOnClickListener { home() }

            row.addView(backText, LinearLayout.LayoutParams(dp(42), dp(43)))
            row.addView(
                txt(title, 27f, fg, true),
                LinearLayout.LayoutParams(0, dp(43), 1f)
            )
            root.addView(row)
        } else {
            root.addView(
                txt(title, 29f, fg, true),
                LinearLayout.LayoutParams(-1, dp(43))
            )
        }

        if (subtitle != null) {
            root.addView(
                txt(subtitle, 12f, muted),
                LinearLayout.LayoutParams(-1, dp(25))
            )
        }
    }

    private fun section(title: String) {
        root.addView(
            txt(title.uppercase(Locale.getDefault()), 10f, muted, true),
            LinearLayout.LayoutParams(-1, dp(35))
        )
    }

    private fun spacer(height: Int) {
        root.addView(
            View(this),
            LinearLayout.LayoutParams(1, dp(height))
        )
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
            txt(date, 14f, muted),
            LinearLayout.LayoutParams(0, dp(42), 1f)
        )

        val settingsButton = txt("⚙", 25f, fg)
        settingsButton.gravity = Gravity.CENTER
        settingsButton.setOnClickListener { settings() }

        top.addView(
            settingsButton,
            LinearLayout.LayoutParams(dp(45), dp(42))
        )

        root.addView(top)

        val clockPattern = if (clock24) "HH:mm" else "hh:mm"

        val clock = SimpleDateFormat(
            clockPattern,
            Locale.getDefault()
        ).format(Date())

        root.addView(
            txt(clock, 59f, fg, true),
            LinearLayout.LayoutParams(-1, dp(79))
        )

        val hour = SimpleDateFormat(
            "HH",
            Locale.getDefault()
        ).format(Date()).toInt()

        val greeting = when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Welcome back"
        }

        root.addView(
            txt("$greeting. Make room for what matters.", 14f, muted)
        )

        val batteryIntent = registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val battery = batteryIntent?.getIntExtra("level", -1) ?: -1
        val batteryStatus = if (battery >= 0) "$battery%" else "--"

        val batteryCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(13), 0, dp(13), 0)
            background = shape(surface, 15)
        }

        batteryCard.addView(txt("◉", 17f, accent))
        batteryCard.addView(
            txt("  Battery  $batteryStatus", 12f, muted)
        )

        root.addView(
            batteryCard,
            LinearLayout.LayoutParams(-1, dp(37)).apply {
                topMargin = dp(12)
            }
        )

        spacer(20)

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(21), dp(19), dp(21), dp(18))
            background = gradient(
                intArrayOf(
                    accent,
                    0xFF253E71.toInt(),
                    0xFF111A30.toInt()
                ),
                29
            )
            elevation = dp(5).toFloat()
        }

        hero.addView(
            txt("H E X A     /     SPACE", 11f, 0xFFE4EBFF.toInt(), true)
        )

        hero.addView(
            txt(
                "Your phone,\nyour world.",
                28f,
                Color.WHITE,
                true
            ),
            LinearLayout.LayoutParams(-1, dp(83))
        )

        hero.addView(
            txt(
                "A smarter, calmer Android experience.",
                12f,
                0xFFE1E8FF.toInt()
            )
        )

        root.addView(
            hero,
            LinearLayout.LayoutParams(-1, dp(158))
        )

        spacer(13)

        val shortcuts = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        shortcuts.addView(
            shortcut("▧", "Wallpaper") { wallpaper() },
            LinearLayout.LayoutParams(0, dp(75), 1f)
        )

        shortcuts.addView(
            shortcut("⌕", "All apps") { drawer() },
            LinearLayout.LayoutParams(0, dp(75), 1f).apply {
                leftMargin = dp(8)
            }
        )

        shortcuts.addView(
            shortcut("◈", "Edge panel") { edgePanel() },
            LinearLayout.LayoutParams(0, dp(75), 1f).apply {
                leftMargin = dp(8)
            }
        )

        root.addView(shortcuts)

        val search = EditText(this).apply {
            hint = "⌕   Search installed apps"
            setHintTextColor(muted)
            setTextColor(fg)
            textSize = 15f
            setSingleLine(true)
            isFocusable = false
            setPadding(dp(17), 0, dp(17), 0)
            background = shape(surface, 24, 0x334F65A0)

            setOnClickListener { drawer() }
        }

        root.addView(
            search,
            LinearLayout.LayoutParams(-1, dp(54)).apply {
                topMargin = dp(14)
            }
        )

        val favTitle = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        favTitle.addView(
            txt("FAVOURITES", 10f, muted, true),
            LinearLayout.LayoutParams(0, dp(38), 1f)
        )

        val edit = txt("EDIT", 10f, accent, true)
        edit.setOnClickListener { manageFavourites() }
        favTitle.addView(edit)

        root.addView(favTitle)

        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(5), dp(4), dp(5))
            background = shape(surface, 26, 0x224F65A0)
        }

        favourites().take(5).forEach {
            dock.addView(
                iconTile(it),
                LinearLayout.LayoutParams(0, dp(82), 1f)
            )
        }

        root.addView(
            dock,
            LinearLayout.LayoutParams(-1, dp(88))
        )

        val footer = txt(
            "SWIPE UP FOR APPS     •     HEXA 3.0",
            10f,
            muted
        )
        footer.gravity = Gravity.CENTER

        root.addView(
            footer,
            LinearLayout.LayoutParams(-1, dp(35))
        )

        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val dx = event.x - startX
                    val dy = event.y - startY

                    if (startY > dp(100) && dy < -dp(65)) {
                        drawer()
                    } else if (
                        startX > resources.displayMetrics.widthPixels * 0.75f &&
                        dx < -dp(50)
                    ) {
                        edgePanel()
                    }

                    true
                }

                else -> true
            }
        }
    }

    private fun shortcut(
        symbol: String,
        name: String,
        action: () -> Unit
    ) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        background = shape(surface, 19)

        addView(
            txt(symbol, 22f, accent, true).apply {
                gravity = Gravity.CENTER
            }
        )

        addView(
            txt(name, 11f, fg, true).apply {
                gravity = Gravity.CENTER
            }
        )

        setOnClickListener { action() }
    }

    private fun favourites(): List<AppInfo> {
        val saved = pref.getStringSet("fav", null)

        if (saved != null) {
            val list = saved.mapNotNull { pkg ->
                allApps.firstOrNull { it.pkg == pkg }
            }
            if (list.isNotEmpty()) return list
        }

        val preferred = listOf(
            "phone", "message", "chrome",
            "camera", "whatsapp"
        )

        return preferred.mapNotNull { key ->
            allApps.firstOrNull {
                it.title.contains(key, true)
            }
        }.distinctBy { it.pkg }.take(5)
    }

    private fun iconTile(app: AppInfo) =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER

            val image = ImageView(this@MainActivity).apply {
                setImageDrawable(app.icon)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }

            addView(
                image,
                LinearLayout.LayoutParams(
                    dp(iconSize),
                    dp(iconSize)
                )
            )

            if (showLabels) {
                addView(
                    txt(app.title, 9f, fg).apply {
                        gravity = Gravity.CENTER
                        maxLines = 1
                    },
                    LinearLayout.LayoutParams(-1, dp(20))
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

        header("Apps", "Your complete app library", true)

        val input = EditText(this).apply {
            hint = "⌕   Search installed apps"
            setHintTextColor(muted)
            setTextColor(fg)
            setSingleLine(true)
            textSize = 15f
            setPadding(dp(16), 0, dp(16), 0)
            background = shape(surface, 22)
        }

        root.addView(
            input,
            LinearLayout.LayoutParams(-1, dp(51)).apply {
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

        val found = allApps.filter {
            it.pkg !in hidden && it.title.contains(query, true)
        }

        found.chunked(columns).forEach { group ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            group.forEach {
                row.addView(
                    iconTile(it),
                    LinearLayout.LayoutParams(0, dp(99), 1f)
                )
            }

            repeat(columns - group.size) {
                row.addView(
                    View(this),
                    LinearLayout.LayoutParams(0, dp(99), 1f)
                )
            }

            appList.addView(row)
        }

        if (found.isEmpty()) {
            appList.addView(
                txt("No apps found", 14f, muted)
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
                            this,
                            "Favourite added",
                            Toast.LENGTH_SHORT
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
            }
            .show()
    }

    private fun settings() {
        page = "settings"
        base()

        header("Settings", "Make HEXA yours", true)

        section("Appearance")

        setting("AMOLED / Light mode", "Switch between dark and light") {
            pref.edit().putBoolean("dark", !dark).apply()
            settings()
        }

        setting("Accent colour", "Blue, violet, mint, rose, cyan or amber") {
            chooseAccent()
        }

        setting("Wallpaper", "Choose a photo from your Gallery") {
            wallpaper()
        }

        setting("Wallpaper darkness", "Control wallpaper visibility") {
            chooseWallpaperDim()
        }

        section("Home screen")

        setting("App grid", "Choose 3, 4, 5 or 6 columns") {
            chooseGrid()
        }

        setting("Icon size", "Small, normal, large or extra large") {
            chooseIconSize()
        }

        setting("App labels", "Show or hide application names") {
            pref.edit().putBoolean("labels", !showLabels).apply()
            settings()
        }

        setting("Clock format", "Switch between 12 and 24 hour") {
            pref.edit().putBoolean("clock24", !clock24).apply()
            settings()
        }

        section("Apps and shortcuts")

        setting("Favourite apps", "Manage your home screen dock") {
            manageFavourites()
        }

        setting("Hidden apps", "Restore apps hidden from the drawer") {
            manageHidden()
        }

        setting("Edge panel", "Open quick access panel") {
            edgePanel()
        }

        section("Android settings")

        setting("Quick settings", "Open Android system settings") {
            openSystem(Settings.ACTION_SETTINGS)
        }

        setting("Display", "Brightness and display options") {
            openSystem(Settings.ACTION_DISPLAY_SETTINGS)
        }

        setting("Sound", "Volume and ringtone settings") {
            openSystem(Settings.ACTION_SOUND_SETTINGS)
        }

        setting("Battery", "Battery and power settings") {
            openSystem(Intent.ACTION_POWER_USAGE_SUMMARY)
        }

        setting("Default launcher", "Set HEXA as your home app") {
            openSystem(Settings.ACTION_HOME_SETTINGS)
        }

        section("System")

        setting("Reset wallpaper", "Remove your selected Gallery image") {
            pref.edit().remove("wallpaper_uri").apply()
            home()
        }

        setting("Reset HEXA", "Restore default launcher settings") {
            android.app.AlertDialog.Builder(this)
                .setTitle("Reset HEXA?")
                .setMessage("All custom launcher preferences will be reset.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reset") { _, _ ->
                    pref.edit().clear().apply()
                    home()
                }
                .show()
        }

        setting("About HEXA", "HEXA Launcher 3.0 • Hybrid Edition") {
            android.app.AlertDialog.Builder(this)
                .setTitle("HEXA 3.0")
                .setMessage(
                    "HEXA Launcher\n" +
                    "Version 3.0 Hybrid\n\n" +
                    "One UI-inspired usability with HEXA identity.\n\n" +
                    "A third-party Android launcher."
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
            background = shape(surface, 18, 0x183F5A90)

            addView(txt(title, 14f, fg, true))
            addView(txt(description, 11f, muted))

            setOnClickListener { action() }
        }

        root.addView(
            item,
            LinearLayout.LayoutParams(-1, dp(66)).apply {
                topMargin = dp(8)
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
            0xFF6685FF.toInt(),
            0xFF9B75E8.toInt(),
            0xFF35B99A.toInt(),
            0xFFE87598.toInt(),
            0xFF00BBD4.toInt(),
            0xFFFFAA32.toInt()
        )

        android.app.AlertDialog.Builder(this)
            .setTitle("Choose accent colour")
            .setItems(names) { _, index ->
                pref.edit().putInt("accent", colors[index]).apply()
                settings()
            }
            .show()
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
            }
            .show()
    }

    private fun chooseIconSize() {
        val options = arrayOf(
            "Small",
            "Normal",
            "Large",
            "Extra large"
        )

        val values = intArrayOf(36, 44, 52, 60)

        android.app.AlertDialog.Builder(this)
            .setTitle("Icon size")
            .setItems(options) { _, index ->
                pref.edit().putInt("icon_size", values[index]).apply()
                settings()
            }
            .show()
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
            .setTitle("Wallpaper darkness")
            .setItems(options) { _, index ->
                pref.edit()
                    .putInt("wallpaper_dim", values[index])
                    .apply()
                home()
            }
            .show()
    }

    private fun manageHidden() {
        val apps = allApps.filter { it.pkg in hidden }

        if (apps.isEmpty()) {
            Toast.makeText(
                this,
                "No hidden apps",
                Toast.LENGTH_SHORT
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
        val apps = allApps.filter {
            it.pkg in favourites().map { app -> app.pkg }
        }

        if (apps.isEmpty()) {
            Toast.makeText(
                this,
                "No favourite apps",
                Toast.LENGTH_SHORT
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

        header("HEXA Edge", "Quick access at your fingertips", true)

        section("Favourites")

        favourites().forEach { app ->
            setting(app.title, "Open application") {
                launch(app)
            }
        }

        section("Shortcuts")

        setting("All apps", "Open app drawer") {
            drawer()
        }

        setting("HEXA settings", "Customize your launcher") {
            settings()
        }

        setting("Android settings", "Open system settings") {
            openSystem(Settings.ACTION_SETTINGS)
        }

        setting("Wallpaper", "Change your home wallpaper") {
            wallpaper()
        }
    }

    private fun wallpaper() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }

        try {
            startActivityForResult(intent, 3001)
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

        if (requestCode == 3001 && resultCode == RESULT_OK) {
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
                "Wallpaper saved",
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
