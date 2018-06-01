package com.example.piotrek.myapplication

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_inventory_parts.*
import android.graphics.Bitmap
import android.os.AsyncTask
import android.view.*
import java.net.URL
import android.content.Intent
import android.app.Activity
import android.net.Uri
import android.os.Environment
import java.io.*
import android.provider.DocumentsContract
import android.R.attr.mimeType
import android.content.ContentResolver
import org.w3c.dom.Document
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult


class InventoryPartsActivity : AppCompatActivity() {

    private var selectedItem: Int = -1
    private var Items = mutableListOf<MutableMap<String, String>>()
    private var Images = mutableMapOf<String, Bitmap>()
    private var Layouts = mutableListOf<Pair<String, LinearLayout>>()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.inventory_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        when (item.itemId) {
            R.id.exportButton -> {
//                Toast.makeText(this, "You clicked me.", Toast.LENGTH_SHORT).show()
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.setType("text/xml")
                intent.putExtra(Intent.EXTRA_TITLE, "test")
                startActivityForResult(intent, 444)
            }
        }
        return true
    }

    private fun writeXML(uri: Uri) {
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.newDocument()

        val rootElement = doc.createElement("INVENTORY")

        Items.forEach {
            if (it["QuantityInStore"]!!.toInt() < it["QuantityInSet"]!!.toInt()) {
                val neededQuantity = it["QuantityInSet"]!!.toInt() - it["QuantityInStore"]!!.toInt()
                rootElement.appendChild(createItemXML(doc, it["TypeCode"].orEmpty(), it["ItemID"].orEmpty(), it["ColorCode"].orEmpty(), neededQuantity.toString()))
            }
        }

        doc.appendChild(rootElement)
        val transformer = TransformerFactory.newInstance().newTransformer()

        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        val cr = contentResolver
        val ins = cr.openOutputStream(uri)
        transformer.transform(DOMSource(doc), StreamResult(ins))
        ins.close()

    }

    private fun createItemXML(doc: Document, itemType: String, itemID: String, color: String, quantity: String): Element {
        val item = doc.createElement("ITEM")

        item.appendChild(createElementXML(doc, "ITEMTYPE", itemType))
        item.appendChild(createElementXML(doc, "ITEMID", itemID))
        item.appendChild(createElementXML(doc, "COLOR", color))
        item.appendChild(createElementXML(doc, "QTYFILLED", quantity))

        return item
    }

    private fun createElementXML(doc: Document, fieldName: String, fieldValue: String): Element {
        val element = doc.createElement(fieldName)
        element.appendChild(doc.createTextNode(fieldValue))
        return element
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int,
                                         resultData: Intent?) {

        if (requestCode == 444 && resultCode == Activity.RESULT_OK) {
            Log.i("PATH1", "CREATED")

            if (resultData != null) {
                val uri = resultData.getData();
                Log.i("PAHT1", "Uri: " + uri.toString());
                writeXML(uri)

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("DESTROYED", "destroyed")
        var db = DataBaseHelper(this)
        db.openDataBase()
        Items.forEach {
            var args = ContentValues()
            args.put("QuantityInStore", it["QuantityInStore"])
            db.writableDatabase.update("InventoriesParts", args, "_id == ?", arrayOf(it["_id"]))
        }
        db.close()
    }

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory_parts)
        waitLabel.isEnabled = false

        val bundle = intent.extras
        val id = bundle.getString("inventoryKey")
        title = "Project ID: $id"
        AsyncDownload(this).execute(id)

    }

    private fun CreateItems() {
        Layouts.clear()
        var j = 0
        for (i in Items.indices) {
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.HORIZONTAL
            layout.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            layout.setPadding(0, 50, 0, 50)
            layout.background = ContextCompat.getDrawable(this, R.drawable.border)
            val quantityLabel = TextView(this)
            quantityLabel.text = Items[i]["QuantityInStore"] + "/" + Items[i]["QuantityInSet"]
            quantityLabel.gravity = Gravity.CENTER
            quantityLabel.layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 0.3f)
            layout.addView(quantityLabel)


            val colorLabel = TextView(this)
            colorLabel.text = Items[i]["Color"]
            colorLabel.gravity = Gravity.CENTER
            colorLabel.layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 0.2f)
            layout.addView(colorLabel)


            val codeLabel = TextView(this)
            codeLabel.text = Items[i]["ItemName"]
            codeLabel.gravity = Gravity.CENTER
            codeLabel.layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f)
            layout.addView(codeLabel)

            if (i == selectedItem) {

                if (Images[Items[i]["ItemKey"]] != null) {
                    imageView.setImageBitmap(Images[Items[i]["ItemKey"]])
                } else {
                    imageView.setImageBitmap(null)
                }
            } else
                layout.setOnClickListener {
                    selectedItem = i
//                quantityChangeLabel.text = Items[i]["QuantityInStore"] + "/" + Items[i]["QuantityInSet"]
//                Toast.makeText(this, "You clicked me.", Toast.LENGTH_SHORT).show()
                    DrawItems()
                }
            itemsLayout.addView(layout)
            Layouts.add(Layouts.size, Pair(j.toString(), layout))
            j += 1
        }
    }

    private fun DrawItems() {
        Layouts.forEach {
            it.second.setBackgroundResource(R.drawable.rounded_buton)
            if (it.first == selectedItem.toString()) {
                it.second.setBackgroundColor(Color.parseColor("#6CA6CD"))
                if (Images[Items[it.first.toInt()]["ItemKey"]] != null) {
                    imageView.setImageBitmap(Images[Items[it.first.toInt()]["ItemKey"]])
                } else {
                    imageView.setImageBitmap(null)
                }
            }
        }
    }

    inner class AsyncDownload : AsyncTask<String, String, Boolean> {

        private var mContext: Context

        constructor(context: Context) : super() {
            mContext = context
        }

        override fun onPreExecute() {
            super.onPreExecute()
            waitLabel.isEnabled = true
            waitLabel.text = "Fetching Resources"
        }

        override fun doInBackground(vararg p0: String?): Boolean {
            val id = p0[0]
            val db = DataBaseHelper(mContext)
            db.openDataBase()
            val cursor = db.readableDatabase.query("InventoriesParts", arrayOf("_id, ItemID, TypeID, ColorID, QuantityInSet, QuantityInStore"), "InventoryID == ?", arrayOf(id), null, null, null)
            if (cursor.moveToFirst()) {
                var i = 0
                do {

                    val tmp = mutableMapOf<String, String>()
                    tmp["QuantityInStore"] = cursor.getString(cursor.getColumnIndex("QuantityInStore"))
                    tmp["QuantityInSet"] = cursor.getString(cursor.getColumnIndex("QuantityInSet"))
                    tmp["_id"] = cursor.getString(cursor.getColumnIndex("_id"))

                    val TypeID = cursor.getString(cursor.getColumnIndex("TypeID"))
                    val cursorType = db.readableDatabase.query("ItemTypes", arrayOf("Code"), "_id == ?", arrayOf(TypeID), null, null, null)
                    cursorType.moveToFirst()
                    tmp["TypeCode"] = cursorType.getString(cursorType.getColumnIndex("Code"))
                    cursorType.close()

                    val ColorID = cursor.getString(cursor.getColumnIndex("ColorID"))
                    val cursorColor = db.readableDatabase.query("Colors", arrayOf("Name, Code"), "_id == ?", arrayOf(ColorID), null, null, null)
                    cursorColor.moveToFirst()
                    tmp["Color"] = cursorColor.getString(cursorColor.getColumnIndex("Name"))
                    tmp["ColorCode"] = cursorColor.getString(cursorColor.getColumnIndex("Code"))
                    cursorColor.close()
                    val ItemID = cursor.getString(cursor.getColumnIndex("ItemID"))
                    tmp["ItemID"] = ItemID
                    val cursorItemID = db.readableDatabase.query("Parts", arrayOf("Code, Name"), "_id == ?", arrayOf(ItemID), null, null, null)
                    cursorItemID.moveToFirst()
                    tmp["ItemName"] = cursorItemID.getString(cursorItemID.getColumnIndex("Name"))
                    tmp["Code"] = cursorItemID.getString(cursorItemID.getColumnIndex("Code"))

                    cursorItemID.close()

                    tmp["ItemKey"] = i.toString()

                    val cursorImage = db.readableDatabase.query("Codes", arrayOf("Image"), "ItemID == ? and ColorID = ?", arrayOf(ItemID, ColorID), null, null, null)
                    Log.i("PART", "ITEMID $ItemID, COLORID $ColorID")
                    if (cursorImage.count > 0) {
                        cursorImage.moveToFirst()
                        if (cursorImage.getBlob(cursorImage.getColumnIndex("Image")) != null) {
                            val image = cursorImage.getBlob(cursorImage.getColumnIndex("Image"))
                            val bmp = BitmapFactory.decodeByteArray(image, 0, image.size)
                            val scaledBmp = Bitmap.createScaledBitmap(bmp, 250, 250, false)
                            Images[i.toString()] = scaledBmp
                        }

                    } else {
                        var inputTmp: Any
                        inputTmp = try {
                            URL("http://img.bricklink.com/P/" + tmp["ColorCode"] + "/" + tmp["Code"] + ".gif").content
                        } catch (e: Exception) {
                            URL("https://www.bricklink.com/PL/" + tmp["Code"] + ".jpg").content
                        }

                        val input = inputTmp as InputStream
                        val image = input.readBytes()
                            if (image.isNotEmpty()) {
                                val bmp = BitmapFactory.decodeByteArray(image, 0, image.size)
                                if (bmp != null) {
                                    val scaledBmp = Bitmap.createScaledBitmap(bmp, 250, 250, false)
                                    if (scaledBmp != null)
                                        Images[i.toString()] = scaledBmp
                                }
                            }
                    }
                    i += 1
                    Items.add(tmp)
                } while (cursor.moveToNext())
            }
            cursor.close()
            db.close()
            fun selector(p: MutableMap<String, String>): Double? = (p["QuantityInStore"]!!.toDouble() / p["QuantityInSet"]!!.toDouble())
            Items.sortBy { selector(it) }
            return true
        }

        override fun onPostExecute(result: Boolean?) {
            super.onPostExecute(result)
            waitLabel.isEnabled = true
            waitLabel.text = ""
            if (result == true) {
                val a = mContext as InventoryPartsActivity
                a.CreateItems()
                a.DrawItems()
            } else {
                val a = mContext as InventoryPartsActivity
                a.finish()
            }
        }
    }


    fun OnPlusClicked(v: View) {
        if (selectedItem != -1) {
            val inStore = Items[selectedItem]["QuantityInStore"]!!.toInt()
            val inSet = Items[selectedItem]["QuantityInSet"]!!.toInt()
            if (inStore < inSet) {
                Items[selectedItem]["QuantityInStore"] = (inStore + 1).toString()
//                quantityChangeLabel.text = Items[selectedItem]["QuantityInStore"] + "/" + Items[selectedItem]["QuantityInSet"]
                UpdateQuantity()
                DrawItems()
            }
        }
    }

    fun OnMinusClicked(v: View) {
        if (selectedItem != -1) {
            val inStore = Items[selectedItem]["QuantityInStore"]!!.toInt()
            if (inStore > 0) {
                Items[selectedItem]["QuantityInStore"] = (inStore - 1).toString()
//                quantityChangeLabel.text = Items[selectedItem]["QuantityInStore"] + "/" + Items[selectedItem]["QuantityInSet"]
                UpdateQuantity()
                DrawItems()
            }
        }
    }

    private fun UpdateQuantity() {
        Layouts.forEach {
            if (it.first == selectedItem.toString()) {
                it.second.removeAllViews()

                val i = it.first.toInt()

                val quantityLabel = TextView(this)
                quantityLabel.text = Items[i]["QuantityInStore"] + "/" + Items[i]["QuantityInSet"]
                quantityLabel.gravity = Gravity.CENTER
                quantityLabel.layoutParams = LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 0.3f)
                it.second.addView(quantityLabel)


                val colorLabel = TextView(this)
                colorLabel.text = Items[i]["Color"]
                colorLabel.gravity = Gravity.CENTER
                colorLabel.layoutParams = LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 0.2f)
                it.second.addView(colorLabel)


                val codeLabel = TextView(this)
                codeLabel.text = Items[i]["ItemName"]
                codeLabel.gravity = Gravity.CENTER
                codeLabel.layoutParams = LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f)
                it.second.addView(codeLabel)

            }
        }
    }

}