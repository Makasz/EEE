package com.example.piotrek.myapplication

import android.content.ContentValues
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.util.Log
import android.app.Activity
import android.content.Context
import android.os.StrictMode
import kotlinx.android.synthetic.main.activity_add_project.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URL
import android.widget.*
import java.io.*
import android.os.AsyncTask


class CreateProjectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_project)
    }

    inner class AsyncDownload: AsyncTask<String, String, Boolean> {

        private var mContext: Context
        private var itemsCount:Int = 0
        private var name:String? = null

        constructor(context: Context) : super()
        {
            mContext = context
        }

        protected fun onProgressUpdate(vararg progress: Int) {
            createButton.text = "Downloading in progress "+progress[0]
        }

        override fun onPreExecute() {
            super.onPreExecute()
            createButton.isEnabled = false
            createButton.text = "Downloading in progress"
            nameEdit.isEnabled = false
            idEdit.isEnabled = false
        }

        override fun doInBackground(vararg p0: String?): Boolean {
            var url = URL(p0[0])
            name = p0[1]

            lateinit var instream : InputStream
            try {
                var urlconn= url.openConnection()
                urlconn.connect()
                instream = urlconn.getInputStream()
            }
            catch (e:Exception)
            {
                return false
            }

            var items = parseInventory(instream)

            var db = DataBaseHelper(mContext)//TODO CHANGE
            db.openDataBase()
            var index = 0
            itemsCount = items.size
            for (i in items)
            {
                if (i.size != 8)
                {
                    throw Exception("Bad XML parsing")
                }
                if (i[5] == "N")
                {
                    var cursor = db.readableDatabase.query("Inventories" ,
                            arrayOf("_id"),
                            "Name == ?", arrayOf(name),
                            null, null, null)
                    cursor.moveToFirst()
                    var InventoryID = cursor.getString(cursor.getColumnIndex("_id"))
                    cursor.close()


                    var itemtype = i[0]
                    cursor = db.readableDatabase.query("ItemTypes" ,
                            arrayOf("_id"),
                            "Code == ?", arrayOf(itemtype),
                            null, null, null)

                    cursor.moveToFirst()
                    var TypeID = cursor.getString(cursor.getColumnIndex("_id")).toInt()
                    cursor.close()


                    var color = i[3]
                    cursor = db.readableDatabase.query("Colors" , arrayOf("_id"), "Code == ?", arrayOf(color), null, null, null)
                    cursor.moveToFirst()
                    var ColorID = cursor.getString(cursor.getColumnIndex("_id")).toInt()
                    cursor.close()


                    var itemId = i[1]
                    cursor = db.readableDatabase.query("Parts" , arrayOf("_id"), "Code == ?", arrayOf(itemId), null, null, null)
                    cursor.moveToFirst()
                    if (cursor.count == 0)
                    {
                        publishProgress(index.toString(), "Part with number $itemId and color $color doesnt exist in Database")
                        continue
                    }
                    else
                    {
                        publishProgress(index.toString())
                    }
                    index += 1

                    var ItemID = cursor.getString(cursor.getColumnIndex("_id")).toInt()
                    cursor.close()

                    var Extra = i[4]
                    var Quantity = i[2].toInt()

                    val values = ContentValues()
                    values.put("InventoryID", InventoryID)
                    values.put("TypeID", TypeID)
                    values.put("ItemID", ItemID)
                    values.put("QuantityInSet", Quantity)
                    values.put("QuantityInStore", 0)
                    values.put("ColorID", ColorID)
                    values.put("Extra", Extra)
                    db.writableDatabase.insert("InventoriesParts", null, values)
                    getImage(ItemID.toString(), ColorID.toString(), mContext)
                }
            }
            db.close()
            return true
        }

        override fun onProgressUpdate(vararg values: String?) {
            super.onProgressUpdate(*values)
            createButton.text = "Downloading in progress "+values[0]+"/"+itemsCount
            if (values.size == 2)
            {
                var toast = Toast.makeText(mContext,
                        values[1],
                        Toast.LENGTH_LONG)
                toast.show()

            }
        }

        override fun onPostExecute(result: Boolean?) {
            super.onPostExecute(result)
            if (result == true)
            {
                var a = mContext as Activity
                a.finish()
            }
            else
            {
                publishProgress("","Cannot find file")
                var db = DataBaseHelper(mContext)
                db.openDataBase()

                db.writableDatabase.delete("Inventories", "name == ?", arrayOf(name))

                db.close()
                var a = mContext as Activity
                a.finish()
            }
        }
    }

    fun createProject(view:View)
    {
        var name = nameEdit.text.toString()
        createInventory(name)
        AsyncDownload(this).execute("http://fcds.cs.put.poznan.pl/MyWeb/BL/" + idEdit.text.toString() +".xml", name)
    }

    private fun getImage(ItemID : String, ColorID: String, context: Context) {
        var db = DataBaseHelper(context)
        db.openDataBase()
        var cursor = db.readableDatabase.query("Codes",
                arrayOf("Code"),
                "ItemID == ? and ColorID == ?", arrayOf(ItemID,ColorID),
                null, null, null)

        cursor.moveToFirst()
        if (cursor.count > 0) {

            var Code = cursor.getString(cursor.getColumnIndex("Code"))
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())

            try {
                val input = URL("https://www.lego.com/service/bricks/5/2/" + Code).content as InputStream
                var image = input.readBytes()
                var size = image.size
                if (image.isNotEmpty())
                {
                    var args = ContentValues()
                    args.put("Image", image)
                    db.writableDatabase.update("Codes", args, "ItemID == ? and ColorID == ?", arrayOf(ItemID, ColorID))
                }
            }
            catch (e: Exception)
            {

            }

        }
        db.close()
    }


    public fun createInventory(name:String)
    {
        var db = DataBaseHelper(this)
        db.openDataBase()
        val values = ContentValues()
        values.put("Name", name)
        values.put("Active", 1)
        values.put("LastAccessed", 0)
        db.writableDatabase.insert("Inventories", null, values)
//TODO Check if inserted name must be unique
        db.close()
    }

    private fun parseInventory(data: InputStream): ArrayList<ArrayList<String>> {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val xpp = factory.newPullParser()

        xpp.setInput(InputStreamReader(data))

        var items = ArrayList<ArrayList<String>>()
        var started = false

        var tmp: ArrayList<String> = ArrayList()

        while (xpp.eventType != XmlPullParser.END_DOCUMENT) {
            if (xpp.eventType == XmlPullParser.START_TAG && xpp.name == "ITEM") {
                started = true
            }
            else if(xpp.eventType == XmlPullParser.END_TAG && xpp.name == "ITEM" && started) {
                items.add(tmp.clone() as ArrayList<String>)
                tmp.clear()
                started = false
            } else if(xpp.eventType == XmlPullParser.TEXT) {
                if (started && !xpp.text.contains("\n"))
                    tmp.add(xpp.text)
            }
            xpp.next()
        }
        return items
    }
}
