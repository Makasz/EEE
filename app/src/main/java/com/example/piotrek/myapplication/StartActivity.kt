package com.example.piotrek.myapplication

import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_start.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import kotlinx.android.synthetic.main.activity_add_project.*
import java.net.HttpURLConnection
import java.net.URL
import android.os.StrictMode




class StartActivity : AppCompatActivity() {

    private val RED: Int = Color.rgb(211,50,50)
    private val GREEN: Int = Color.rgb(50,150,50)

    private var inventories: Cursor? = null
    private lateinit var db : DataBaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        archivalButton.setBackgroundColor(RED)
        archivalButton.text = getString(R.string.archival_button_off)

        archiveButton.setBackgroundColor(Color.CYAN)
        addProjectButton.setBackgroundColor(Color.CYAN)

        db = DataBaseHelper(this)
        db.createDataBase()
        db.openDataBase()

        db.close()
        getData()

    }

    override fun onRestart() {
        super.onRestart()
        getData()
    }

    private fun getData()
    {
        db = DataBaseHelper(this)
        db.openDataBase()

        inventoryLayout.removeAllViews()

        var archive : String? = null
        if (archivalButton.text == getString(R.string.archival_button_off))
            archive = "Active <> 0"

        var cursor = db.readableDatabase.query("Inventories" , arrayOf("_id, Name, Active, LastAccessed"), archive, null, null, null, "LastAccessed ASC")

        if (cursor.moveToFirst()) {
            do {
                var name = cursor.getString(cursor.getColumnIndex("Name"))
                var layout = LinearLayout(this)
                layout.orientation = LinearLayout.HORIZONTAL
                layout.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT)

                var nameLabel = TextView(this)
                nameLabel.text = name
                nameLabel.gravity = Gravity.CENTER
                nameLabel.layoutParams = LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f);


                var id = cursor.getString(cursor.getColumnIndex("_id"))
                var active = cursor.getString(cursor.getColumnIndex("Active"))

                var archiveButton = Button(this)
                if (active != "0") {
                    archiveButton.text = "ARCHIVE"
                    archiveButton.setBackgroundColor(Color.RED)
                }
                else {
                    archiveButton.text = "ACTIVATE"
                    archiveButton.setBackgroundColor(Color.GREEN)
                }
                archiveButton.layoutParams = LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 0.25f)
                archiveButton.setOnClickListener { archiveButtonOnClick(id, active) }

                var detailsButton = Button(this)
                detailsButton.text = "DETAILS"
                detailsButton.layoutParams = LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 0.25f)
                detailsButton.setOnClickListener { detailsButtonOnClick(id) }


                layout.addView(nameLabel)
                layout.addView(archiveButton)
                layout.addView(detailsButton)
                inventoryLayout.addView(layout)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
    }

    private fun archiveButtonOnClick(id: String, active:String)
    {
        db = DataBaseHelper(this)
        db.openDataBase()
        var args = ContentValues()
        if (active != "0")
            args.put("Active", 0)
        else
            args.put("Active", 1)
        db.writableDatabase.update("Inventories", args, "_id == ?", arrayOf(id))
        db.close()
        getData()
    }

    fun archivalSwitch(view: View)
    {
        if (archivalButton.text == getString(R.string.archival_button_off))
        {
            archivalButton.text = getString(R.string.archival_button_on)
            archivalButton.setBackgroundColor(GREEN)
            getData()
        }
        else
        {
            archivalButton.text = getString(R.string.archival_button_off)
            archivalButton.setBackgroundColor(RED)
            getData()
        }
    }

    fun addProject(view:View)
    {
        val intent = Intent(this, CreateProjectActivity::class.java)
        startActivity(intent)
    }
    private fun detailsButtonOnClick(id:String)
    {
        Log.i("ID", id)
        var intent = Intent(this, InventoryPartsActivity::class.java)
        var bundle = Bundle()
        bundle.putString("inventoryKey",id)
        intent.putExtras(bundle)
        startActivity(intent)
    }

}
