package com.hexa.launcher

import android.app.Activity
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppInfo(val title:String,val pkg:String,val icon:android.graphics.drawable.Drawable)

class MainActivity:Activity(){
    private val pref by lazy{getSharedPreferences("hexa_settings",MODE_PRIVATE)}
    private var allApps:List<AppInfo> = emptyList()
    private lateinit var root:LinearLayout
    private lateinit var listBox:LinearLayout
    private var page="home"
    private var touchX=0f
    private var touchY=0f
    private val dark get()=pref.getBoolean("dark",true)
    private val accent get()=pref.getInt("accent",0xFF5476E8.toInt())
    private val bg get()=if(dark) 0xFF101522.toInt() else 0xFFF3F5FA.toInt()
    private val surface get()=if(dark) 0xFF1B2434.toInt() else Color.WHITE
    private val fg get()=if(dark) Color.WHITE else 0xFF172033.toInt()
    private val muted get()=if(dark) 0xFF9BA8BC.toInt() else 0xFF68748A.toInt()
    private val hidden get()=pref.getStringSet("hidden",emptySet())?:emptySet()
    private val columns get()=pref.getInt("columns",4)

    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);loadApps();home()}
    private fun loadApps(){
        val i=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        allApps=packageManager.queryIntentActivities(i,PackageManager.MATCH_ALL).mapNotNull{
            val p=it.activityInfo.packageName
            if(p==packageName)null else AppInfo(it.loadLabel(packageManager).toString(),p,it.loadIcon(packageManager))
        }.distinctBy{it.pkg}.sortedBy{it.title.lowercase(Locale.getDefault())}
    }
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private fun shape(c:Int,r:Int,stroke:Int?=null)=GradientDrawable().apply{
        setColor(c);cornerRadius=dp(r).toFloat();if(stroke!=null)setStroke(dp(1),stroke)
    }
    private fun t(s:String,size:Float,color:Int=fg,bold:Boolean=false)=TextView(this).apply{
        text=s;textSize=size;setTextColor(color);gravity=Gravity.CENTER_VERTICAL
        if(bold)typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)
    }
    private fun base(){
        window.statusBarColor=bg;window.navigationBarColor=bg
        root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(16),dp(20),dp(12));setBackgroundColor(bg)}
        setContentView(root)
    }
    private fun header(title:String,sub:String?=null,back:Boolean=false){
        if(back){val b=t("‹",32f,fg);b.setOnClickListener{home()};root.addView(b,LinearLayout.LayoutParams(-1,dp(42)))}
        root.addView(t(title,31f,fg,true),LinearLayout.LayoutParams(-1,dp(43)))
        if(sub!=null)root.addView(t(sub,13f,muted),LinearLayout.LayoutParams(-1,dp(25)))
    }
    private fun home(){
        page="home";base()
        val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        top.addView(t(SimpleDateFormat("EEEE, d MMMM",Locale.getDefault()).format(Date()),13f,muted),LinearLayout.LayoutParams(0,dp(30),1f))
        val gear=t("⚙",22f,fg);gear.setOnClickListener{settings()};top.addView(gear)
        root.addView(top)
        root.addView(t(SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date()),57f,fg,true),LinearLayout.LayoutParams(-1,dp(75)))
        root.addView(t("Make room for what matters.",14f,muted))
        val gap=View(this);root.addView(gap,LinearLayout.LayoutParams(1,0,0.55f))
        val card=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(19),dp(20),dp(19))
            background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(accent,0xFF394B88.toInt(),0xFF242E50.toInt())).apply{cornerRadius=dp(28).toFloat()}
            elevation=dp(5).toFloat()
        }
        card.addView(t("HEXA SPACE",11f,0xFFE8EDFF.toInt(),true))
        card.addView(t("Your phone,\\nyour rhythm.",26f,Color.WHITE,true),LinearLayout.LayoutParams(-1,dp(72)))
        card.addView(t("A calmer, cleaner home for Android.",12f,0xFFE0E7FF.toInt()))
        root.addView(card,LinearLayout.LayoutParams(-1,dp(145)))
        val quick=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        quick.addView(chip("▦","Widgets"){settings()},LinearLayout.LayoutParams(0,dp(65),1f))
        quick.addView(chip("⌕","Search"){drawer()},LinearLayout.LayoutParams(0,dp(65),1f).apply{leftMargin=dp(8)})
        quick.addView(chip("◈","Edge panel"){edgePanel()},LinearLayout.LayoutParams(0,dp(65),1f).apply{leftMargin=dp(8)})
        root.addView(quick,LinearLayout.LayoutParams(-1,dp(73)).apply{topMargin=dp(12)})
        val search=EditText(this).apply{
            hint="⌕   Search apps";setHintTextColor(muted);setTextColor(fg);textSize=14f;isFocusable=false
            setPadding(dp(18),0,dp(18),0);background=shape(surface,25,0x334F65A0)
            setOnClickListener{drawer()}
        }
        root.addView(search,LinearLayout.LayoutParams(-1,dp(51)).apply{topMargin=dp(9)})
        root.addView(t("FAVOURITES",10f,muted,true),LinearLayout.LayoutParams(-1,dp(31)))
        val dock=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;setPadding(dp(4),dp(4),dp(4),dp(4));background=shape(surface,25)}
        favourites().forEach{dock.addView(iconTile(it),LinearLayout.LayoutParams(0,dp(75),1f))}
        root.addView(dock,LinearLayout.LayoutParams(-1,dp(82)))
        val hint=t("SWIPE UP FOR APPS     •     HEXA 1.1",10f,muted);hint.gravity=Gravity.CENTER
        root.addView(hint,LinearLayout.LayoutParams(-1,dp(28)))
        root.setOnTouchListener{_,e->
            when(e.action){
                MotionEvent.ACTION_DOWN->{touchX=e.x;touchY=e.y;true}
                MotionEvent.ACTION_UP->{
                    val dx=e.x-touchX;val dy=e.y-touchY
                    if(touchY>resources.displayMetrics.heightPixels*0.65f && dy < -dp(45))drawer()
                    else if(touchX>resources.displayMetrics.widthPixels-dp(35)&&dx < -dp(35))edgePanel()
                    true
                }else->true
            }
        }
    }
    private fun chip(symbol:String,name:String,action:()->Unit)=LinearLayout(this).apply{
        orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;background=shape(surface,18)
        addView(t(symbol,20f,accent,true).apply{gravity=Gravity.CENTER})
        addView(t(name,11f,fg,true).apply{gravity=Gravity.CENTER})
        setOnClickListener{action()}
    }
    private fun favourites():List<AppInfo>{
        val saved=pref.getStringSet("fav",null)
        if(saved!=null){val f=saved.mapNotNull{p->allApps.firstOrNull{it.pkg==p}};if(f.isNotEmpty())return f.take(5)}
        val keys=listOf("phone","message","chrome","camera","whatsapp")
        val first=keys.mapNotNull{k->allApps.firstOrNull{it.title.contains(k,true)}}.distinctBy{it.pkg}
        return (first+allApps.filter{it !in first}).take(5)
    }
    private fun iconTile(a:AppInfo)=LinearLayout(this).apply{
        orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER
        val iv=ImageView(this@MainActivity).apply{setImageDrawable(a.icon);scaleType=ImageView.ScaleType.FIT_CENTER}
        addView(iv,LinearLayout.LayoutParams(dp(43),dp(43)))
        addView(t(a.title,9f,fg).apply{gravity=Gravity.CENTER;maxLines=1},LinearLayout.LayoutParams(-1,dp(20)))
        setOnClickListener{launch(a)}
        setOnLongClickListener{appMenu(a);true}
    }
    private fun drawer(){
        page="drawer";base();header("Apps","Find anything on your phone")
        val input=EditText(this).apply{hint="Search installed apps";setHintTextColor(muted);setTextColor(fg);singleLine=true;setPadding(dp(15),0,dp(15),0);background=shape(surface,22)}
        root.addView(input,LinearLayout.LayoutParams(-1,dp(49)).apply{topMargin=dp(12);bottomMargin=dp(10)})
        val scroll=ScrollView(this);listBox=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};scroll.addView(listBox)
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        renderApps("")
        input.addTextChangedListener(object:android.text.TextWatcher{
            override fun beforeTextChanged(s:CharSequence?,st:Int,c:Int,a:Int){}
            override fun onTextChanged(s:CharSequence?,st:Int,b:Int,c:Int){renderApps(s?.toString().orEmpty())}
            override fun afterTextChanged(e:android.text.Editable?){}
        })
    }
    private fun renderApps(q:String){
        listBox.removeAllViews()
        val found=allApps.filter{it.pkg !in hidden && it.title.contains(q,true)}
        found.chunked(columns).forEach{group->
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
            group.forEach{row.addView(iconTile(it),LinearLayout.LayoutParams(0,dp(91),1f))}
            repeat(columns-group.size){row.addView(View(this),LinearLayout.LayoutParams(0,dp(91),1f))}
            listBox.addView(row)
        }
        if(found.isEmpty())listBox.addView(t("No apps found",14f,muted))
    }
    private fun appMenu(a:AppInfo){
        val opts=arrayOf("Open","Add to favourites","Hide app","App info")
        android.app.AlertDialog.Builder(this).setTitle(a.title).setItems(opts){_,n->
            when(n){
                0->launch(a)
                1->{val s=pref.getStringSet("fav",null)?.toMutableSet()?:favourites().map{it.pkg}.toMutableSet();s.add(a.pkg);pref.edit().putStringSet("fav",s).apply();Toast.makeText(this,"Favourite saved",Toast.LENGTH_SHORT).show()}
                2->{val s=hidden.toMutableSet();s.add(a.pkg);pref.edit().putStringSet("hidden",s).apply();if(page=="drawer")drawer()}
                3->{startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply{data=android.net.Uri.parse("package:${a.pkg}")})}
            }
        }.show()
    }
    private fun settings(){
        page="settings";base();header("Settings","Make HEXA yours",true)
        setting("Display","Dark or light appearance"){val e=pref.edit();e.putBoolean("dark",!dark).apply();settings()}
        setting("Accent colour","Choose HEXA blue, violet, mint or rose"){chooseAccent()}
        setting("Home screen grid","Choose 3, 4 or 5 app columns"){chooseGrid()}
        setting("Hidden apps","Manage apps hidden from the app drawer"){manageHidden()}
        setting("Wallpaper","Open Android wallpaper picker"){wallpaper()}
        setting("Default home app","Choose HEXA as your default launcher"){try{startActivity(Intent(Settings.ACTION_HOME_SETTINGS))}catch(_:Exception){startActivity(Intent(Settings.ACTION_SETTINGS))}}
        setting("Reset HEXA settings","Restore default launcher preferences"){android.app.AlertDialog.Builder(this).setMessage("Reset appearance, favourites and hidden apps?").setNegativeButton("Cancel",null).setPositiveButton("Reset"){_,_->pref.edit().clear().apply();settings()}.show()}
        setting("About HEXA","HEXA Launcher 1.1 • One UI-inspired, independently designed"){android.app.AlertDialog.Builder(this).setTitle("HEXA Launcher").setMessage("A third-party Android launcher. It changes the home screen and app drawer, not Android's system interface.").setPositiveButton("OK",null).show()}
    }
    private fun setting(title:String,desc:String,action:()->Unit){
        val v=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(11),dp(16),dp(11));background=shape(surface,18)
            addView(t(title,15f,fg,true));addView(t(desc,11f,muted));setOnClickListener{action()}}
        root.addView(v,LinearLayout.LayoutParams(-1,dp(66)).apply{topMargin=dp(9)})
    }
    private fun chooseAccent(){
        val names=arrayOf("HEXA Blue","Violet","Mint","Rose")
        val colors=intArrayOf(0xFF5476E8.toInt(),0xFF9B75E8.toInt(),0xFF35B99A.toInt(),0xFFE87598.toInt())
        android.app.AlertDialog.Builder(this).setTitle("Accent colour").setItems(names){_,i->pref.edit().putInt("accent",colors[i]).apply();settings()}.show()
    }
    private fun chooseGrid(){
        val values=arrayOf("3 columns","4 columns","5 columns")
        android.app.AlertDialog.Builder(this).setTitle("App grid").setItems(values){_,i->pref.edit().putInt("columns",i+3).apply();settings()}.show()
    }
    private fun manageHidden(){
        val hiddenApps=allApps.filter{it.pkg in hidden}
        if(hiddenApps.isEmpty()){Toast.makeText(this,"No hidden apps",Toast.LENGTH_SHORT).show();return}
        android.app.AlertDialog.Builder(this).setTitle("Tap an app to unhide").setItems(hiddenApps.map{it.title}.toTypedArray()){_,i->
            val s=hidden.toMutableSet();s.remove(hiddenApps[i].pkg);pref.edit().putStringSet("hidden",s).apply();manageHidden()
        }.setNegativeButton("Close",null).show()
    }
    private fun edgePanel(){
        page="edge";base();header("Edge panel","Quick access, one swipe away",true)
        favourites().forEach{a->setting(a.title,"Launch app"){launch(a)}}
        setting("All apps","Open the complete app drawer"){drawer()}
        setting("HEXA settings","Customize appearance and layout"){settings()}
    }
    private fun wallpaper(){try{startActivity(Intent(Intent.ACTION_SET_WALLPAPER))}catch(_:Exception){startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))}}
    private fun launch(a:AppInfo){packageManager.getLaunchIntentForPackage(a.pkg)?.let{startActivity(it)}}
    @Deprecated("Deprecated")
    override fun onBackPressed(){if(page!="home")home()else super.onBackPressed()}
}
