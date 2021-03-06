package com.soilmoisturesensor

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_plant_d_b_view_endpoint.*
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

/**
 * PlantDBViewPoint activity which is basically the PlantDB Search Page of the Application
 * @author Manpreet Sandhu
 */
class PlantDBViewEndpoint : AppCompatActivity() {
    private val TAG =""

    var mtoken = ""
    var plantTxtData:String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_d_b_view_endpoint)

        this.setTitle("Plant database")

        val mUser = FirebaseAuth.getInstance().currentUser

        backToHome.setOnClickListener{
            finish()
        }

        btn.setOnClickListener{
            plantTxtData = plantTxt.text.toString().trim()
            if(plantTxtData.isEmpty()){

                Toast.makeText(this,"Warning: Enter the Plant Name", Toast.LENGTH_LONG).show()
            }
            else {
                mUser!!.getIdToken(true)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            closeKeyBoard()
                            val idToken = task.result!!.token
                            mtoken = idToken.toString()
                            postRequestToGetPlantData()
                        } else {
                            Log.d("ERROR Creating Token", task.exception.toString());
                        }
                    }

            }
        }

    }
    private fun closeKeyBoard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    /**
     * Method to add plant name,token to the header and do a post request
     * @author Manpreet Sandhu
     */
    private fun postRequestToGetPlantData() {
        val r = JSONObject()
        r.put("plantName", plantTxt.text.trim())

        SendJsonDataToServer().execute(r.toString());
    }

    /**
     * Inner Class to get Plant data  by calling our
     * endpoint https://www.ecoders.ca/getPlantData
     *
     * @author Manpreet Sandhu
     */
    inner class SendJsonDataToServer :
        AsyncTask<String?, String?, String?>(){

        var inputLine: String? = null

        @SuppressLint("WrongThread")
        override fun doInBackground(vararg params: String?): String? {
            var JsonResponse: String? = null
            val JsonDATA = params[0]!!
            var urlConnection: HttpURLConnection? = null
            var reader: BufferedReader? = null

            try {
                val url = URL("https://ecoders.nikhilkapadia.com/getPlantData");
                urlConnection = url.openConnection() as HttpURLConnection;
                urlConnection.setDoOutput(true);
                // is output buffer writter
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty ("Authorization", mtoken);
                //urlConnection.setRequestProperty("token", "token");
                urlConnection.setRequestProperty("Accept", "application/json");
                //set headers and method
                val writer: Writer = BufferedWriter(OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8"));
                writer.write(JsonDATA);
                // json data
                writer.close();
                val inputStream: InputStream = urlConnection.getInputStream();
                //input stream
                val buffer: StringBuffer? = null
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = BufferedReader(InputStreamReader(inputStream))

                inputLine = reader.readLine()
                var out = inputLine.toString()
                var out1 = out.replace("{","")
                var out2 = out1.replace("}","")
                var out4 = out2.replace(":",",")
                var out7 = out4.split(",").toTypedArray()

                //setting the Plant values to the textviews
                this@PlantDBViewEndpoint.txt1.text = "Botanical Name: "+out7[3].replace("\"", "")
                this@PlantDBViewEndpoint.txt2.text = "Common Name: "+out7[5].replace("\"", "")
                this@PlantDBViewEndpoint.txt3.text = "Plant Type: "+out7[7].replace("\"", "")
                this@PlantDBViewEndpoint.txt4.text = "Mature Size: "+out7[9].replace("\"", "")
                this@PlantDBViewEndpoint.txt5.text = "Care: "
                this@PlantDBViewEndpoint.txt6.text = "Light Level: "+out7[12].replace("\"", "")
                this@PlantDBViewEndpoint.txt7.text = "Humidity: "+out7[14].replace("\"", "")
                this@PlantDBViewEndpoint.txt8.text = "Duration: "+out7[16].replace("\"", "")
                this@PlantDBViewEndpoint.txt9.text = "Direction: "+out7[18].replace("\"", "")


                if (inputLine.equals("null")) {
                    return null
                } else {

                    return inputLine


                }

            }catch (ex:Exception){

            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (ex: Exception) {
                        Log.e(TAG, "Error closing stream", ex);
                    }
                }
            }
            return null

        }


    }



}