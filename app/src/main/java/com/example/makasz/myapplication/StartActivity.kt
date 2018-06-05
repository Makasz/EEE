package com.example.makasz.myapplication

import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
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
import android.util.TypedValue

class StartActivity : AppCompatActivity() {

    private val RED: Int = Color.rgb(211, 50, 50)
    private val GREEN: Int = Color.rgb(50, 150, 50)

    private var inventories: Cursor? = null
    private lateinit var db: DataBaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

//        archivalButton.setBackgroundColor(RED)
        archivalButton.text = getString(R.string.archival_button_off)

//        archiveButton.setBackgroundColor(Color.parseColor("#3785B9"))
//        addProjectButton.setBackgroundColor(Color.parseColor("#3785B9"))

        db = DataBaseHelper(this)
        db.createDataBase()
        db.openDataBase()

        db.close()
        setTitle("Project List")
        getData()

    }

    override fun onRestart() {
        super.onRestart()
        getData()
    }

    private fun getData() {
        db = DataBaseHelper(this)
        db.openDataBase()

        inventoryLayout.removeAllViews()

        var archive: String? = null
        if (archivalButton.text == getString(R.string.archival_button_off))
            archive = "Active <> 0"

        val cursor = db.readableDatabase.query("Inventories", arrayOf("_id, Name, Active, LastAccessed"), archive, null, null, null, "LastAccessed ASC")

        if (cursor.moveToFirst()) {
            do {
                val name = cursor.getString(cursor.getColumnIndex("Name"))
                val layout = LinearLayout(this)
                layout.orientation = LinearLayout.HORIZONTAL
                layout.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        150
                        )

                val nameLabel = TextView(this)
                nameLabel.text = name
                nameLabel.setTextSize(TypedValue.COMPLEX_UNIT_IN,0.20f);
                nameLabel.gravity = Gravity.CENTER
                nameLabel.layoutParams = LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f);

                val id = cursor.getString(cursor.getColumnIndex("_id"))
                val active = cursor.getString(cursor.getColumnIndex("Active"))

                val archiveButton = Button(this)
                if (active != "0") {
                    archiveButton.text = getString(R.string.restore)
                } else {
                    archiveButton.text = getString(R.string.activate)
                }
                archiveButton.setBackgroundResource(R.drawable.rounded_buton)
                archiveButton.layoutParams = LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT - 40, 0.25f)
                archiveButton.setOnClickListener { archiveButtonOnClick(id, active) }

                val openProjectButton = Button(this)
                openProjectButton.text = getString(R.string.open)
                openProjectButton.setBackgroundResource(R.drawable.rounded_buton)
                openProjectButton.layoutParams = LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 0.25f)
                openProjectButton.setOnClickListener { detailsButtonOnClick(id) }


                layout.addView(nameLabel)
                layout.addView(archiveButton)
                layout.addView(openProjectButton)
                inventoryLayout.addView(layout)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
    }

    private fun archiveButtonOnClick(id: String, active: String) {
        db = DataBaseHelper(this)
        db.openDataBase()
        val args = ContentValues()
        if (active != "0")
            args.put("Active", 0)
        else
            args.put("Active", 1)
        db.writableDatabase.update("Inventories", args, "_id == ?", arrayOf(id))
        db.close()
        getData()
    }

    fun archivalSwitch(view: View) {
        if (archivalButton.text == getString(R.string.archival_button_off)) {
            setTitle("Project List")
            archivalButton.text = getString(R.string.archival_button_on)
            archivalButton.setBackgroundColor(Color.parseColor("#2B719F"))
            getData()
        } else {
            setTitle("Project List (Archived Visible)")
            archivalButton.text = getString(R.string.archival_button_off)
            archivalButton.setBackgroundColor(Color.parseColor("#708898"))
            getData()
        }
        archivalButton.setBackgroundResource(R.drawable.rounded_buton)
    }

    fun addProject(view: View) {
        val intent = Intent(this, CreateProjectActivity::class.java)
        startActivity(intent)
    }

    private fun detailsButtonOnClick(id: String) {
        Log.i("ID", id)
        val intent = Intent(this, InventoryPartsActivity::class.java)
        val bundle = Bundle()
        bundle.putString("inventoryKey", id)
        intent.putExtras(bundle)
        startActivity(intent)
    }

}
