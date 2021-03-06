package com.soilmoisturesensor

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_sign_up.*
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL


/**
 * Sign up activity
 * @author Manpreet sandhu
 */
class SignUp : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private val TAG = "";
    var muid = ""
    var memail = ""
    var mfirstName = ""
    var mlastName = ""
    var mtoken = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val displaymetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displaymetrics)
        val width = displaymetrics.widthPixels
        val buttonWidth = width / 2

        ok.width = (buttonWidth)
        cancel.width = buttonWidth

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance()

        ok.setOnClickListener(View.OnClickListener { view ->
            register()

        })

        cancel.setOnClickListener {

            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    /**
     * SignUp method
     * @author Manpreet Sandhu
     */
    private fun register() {

        val mName = fullName.text.toString()
        val mEmail = emailAddress.text.toString()
        val mPwd = password1.text.toString()
        val mPwd1 = password2.text.toString()

        if (!mName.isEmpty() && !mEmail.isEmpty() && !mPwd.isEmpty()) {

            if (!Patterns.EMAIL_ADDRESS.matcher(mEmail).matches()) {
                Toast.makeText(
                    this,
                    "Email address not in correct format",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                if (mPwd.equals(mPwd1)) {

                    if (mPwd.length < 6) {
                        Toast.makeText(
                            this,
                            "Password needs to be at least 6 characters",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {

                        mAuth.createUserWithEmailAndPassword(mEmail, mPwd)
                            .addOnCompleteListener(this, OnCompleteListener { task ->

                                if (task.isSuccessful) {

                                    val mUser = FirebaseAuth.getInstance().currentUser
                                    mUser!!.getIdToken(true)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                val idToken = task.result!!.token
                                                muid = mUser.uid

                                                memail =
                                                    mEmail  //sub this out to just mEmail when sending

                                                val firstSpace = mName.indexOf(" ")
                                                mfirstName = mName.substring(0, firstSpace)
                                                //split this into first and last name in the form
                                                mlastName = mName.substring(firstSpace).trim()

                                                mtoken = idToken.toString() // can simply just pass

                                                //THE ENDPOINT IS https://www.ecoders.ca/addUser
                                                //Invoking the Senddatatoserver using POST Request Method
                                                sendDataToServer();

                                            } else {
                                                Log.d(
                                                    "ERROR Creating Token",
                                                    task.exception.toString()
                                                );
                                            }
                                        }
                                    startActivity(Intent(this, SignIn::class.java))

                                    Toast.makeText(this, "SignUp Successful", Toast.LENGTH_LONG)
                                        .show()
                                } else {
                                    Toast.makeText(this, "Error :(", Toast.LENGTH_LONG).show()

                                }
                            })
                    }
                } else {

                    Toast.makeText(this, "Password does not match", Toast.LENGTH_LONG).show()
                }
            }
        } else {

            Toast.makeText(this, "Please fill values", Toast.LENGTH_SHORT).show()
        }
    }


    /**
     * Method to add uid, token, email, firstname, lastname to the header and do a post request
     * @author Manpreet Sandhu
     */
    fun sendDataToServer() {

        //JSON OBJECT
        val r = JSONObject()
        r.put("uid", muid)
        r.put("email", memail)
        r.put("firstName", mfirstName)
        r.put("lastName", mlastName)
        r.put("token", mtoken)

        //#call to async class
        SendJsonDataToServer().execute(r.toString());

    }


    /**
     * ASYNC Task class - Inner Class to add user to DB after signup
     * endpoint https://www.ecoders.ca/addUser
     *
     * @author Manpreet Sandhu
     */
    inner class SendJsonDataToServer :
        AsyncTask<String?, String?, String?>() {

        override fun doInBackground(vararg params: String?): String? {
            var JsonResponse: String? 
            val JsonDATA = params[0]!!
            var urlConnection: HttpURLConnection? = null
            var reader: BufferedReader? = null

            try {
                val url = URL("https://ecoders.nikhilkapadia.com/addUser");
                urlConnection = url.openConnection() as HttpURLConnection;
                urlConnection.setDoOutput(true);
                // is output buffer writter
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Authorization", mtoken);
                //urlConnection.setRequestProperty("token", "token");
                urlConnection.setRequestProperty("Accept", "application/json");
                //set headers and method
                val writer: Writer =
                    BufferedWriter(OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8"));
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

                var inputLine: String = ""
                while ((inputLine.equals(reader.readLine())) != null) {
                    buffer?.append(inputLine + "\n")
                }
                if (buffer?.length === 0) {
                    // Stream was empty. No point in parsing.
                    return null
                }
                JsonResponse = buffer.toString()

                //response data
                Log.i(TAG, JsonResponse)
                try {
                    //send to post execute
                    return JsonResponse
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                return null

            } catch (ex: Exception) {

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
