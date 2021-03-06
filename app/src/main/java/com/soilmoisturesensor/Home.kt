package com.soilmoisturesensor

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import kotlin.collections.ArrayList

/**
 * Home activity which is basically the dashboard of the application
 * @author Ehsan Kabir
 */
class Home : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    RecyclerAdapter.onItemClickListener {

    private lateinit var drawerlayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var adapter: RecyclerAdapter
    private lateinit var intentForUnique: Intent
    private lateinit var intentForSetting: Intent
    private lateinit var mAuth: FirebaseAuth

    var muid = ""
    var mtoken = ""
    private val TAG = "";
    private var userUid = ""
    private var userEmail = ""
    private var userFirstName = ""
    private var userLastName = ""
    private var userDevices = ArrayList<String>()
    private var deviceId = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        //Spinner while the data is loading
        progressbarDashboard.visibility = View.VISIBLE

        //Creating intents to pass data to different activity
        intentForUnique = Intent(this@Home, UniqueDataActivity::class.java)
        intentForSetting = Intent(this@Home, Settings::class.java)

        //Initializing Firebase Authentication
        mAuth = FirebaseAuth.getInstance()

        //Getting the logged in user
        val mUser = FirebaseAuth.getInstance().currentUser

        //Getting token from the user
        mUser!!.getIdToken(true)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val idToken = task.result!!.token
                    muid = mUser.uid
                    mtoken = idToken.toString()
                    postRequestToGetUserData()
                    postRequestToGetDashboardData()
                } else {
                    Log.d("ERROR Creating Token", task.exception.toString());
                }
            }

        //Setting up dashboard cards
        dashboardItem_list.layoutManager = LinearLayoutManager(this@Home)
        adapter = RecyclerAdapter(this)

        //Setting up the app title bar to have the navigation drawer icon
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        drawerlayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.nav_view)

        val Toggle = ActionBarDrawerToggle(
            this, drawerlayout, toolbar, 0, 0
        )

        //This is to listen to which drawer element is clicked
        drawerlayout.addDrawerListener(Toggle)
        Toggle.syncState()
        navigationView.setNavigationItemSelectedListener(this)


    }

    /**
     *  Method which handles the nav bar.
     *  Directs user to different activities when clicked on different items in the nav bar
     *
     *  @author Ehsan Kabir
     */

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.Dashboard -> {
                finish()
                startActivity(Intent(applicationContext, Home::class.java))
            }

            R.id.PlantDatabase -> {
                startActivity(Intent(applicationContext, PlantDBViewEndpoint::class.java))
            }

            R.id.Logout -> {
                mAuth.signOut()
                startActivity(Intent(this, MainActivity::class.java))
                Toast.makeText(this, "Successfully Log out", Toast.LENGTH_LONG).show()
            }

            R.id.Settings -> {
                intentForSetting.putExtra("first_name", userFirstName)
                intentForSetting.putExtra("last_name", userLastName)
                intentForSetting.putExtra("email", userEmail)
                intentForSetting.putExtra("id", userUid)
                intentForSetting.putExtra("token", mtoken)
                startActivity(intentForSetting)
            }

            R.id.DeviceSetup -> {
                startActivity(Intent(this, DeviceSetup::class.java))
            }

            R.id.Notifications -> {
                val intent = Intent(this@Home, Notifications::class.java)
                intent.putExtra("DeviceName", userDevices)
                intent.putExtra("DeviceId", deviceId)
                startActivity(intent)

            }
        }
        drawerlayout.closeDrawer(GravityCompat.START)
        return true
    }

    /**
     * Method to navigate to a new activity when clicked on a device
     * in the dashboard
     *
     * @author Ehsan Kabir
     */
    override fun onItemClick(position: Int) {
        intentForUnique.putExtra("itemClicked", position)
        startActivity(intentForUnique)
    }


    /**
     * Method to add uid, token to the header and do a post request
     * @author Ehsan Kabir
     */
    private fun postRequestToGetUserData() {
        val r = JSONObject()
        r.put("uid", muid)
        r.put("token", mtoken)
        SendJsonDataToGetUserData().execute(r.toString());
    }

    /**
     * Inner Class to get User data  by calling our
     * endpoint "https://www.ecoders.ca/login"
     *
     * @author Ehsan Kabir
     */
    inner class SendJsonDataToGetUserData :
        AsyncTask<String?, String?, String?>() {

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
        }

        override fun doInBackground(vararg params: String?): String? {
            val JsonDATA = params[0]!!
            var urlConnection: HttpURLConnection? = null
            var reader: BufferedReader? = null
            try {
                val url = URL("https://ecoders.nikhilkapadia.com/login");
                urlConnection = url.openConnection() as HttpURLConnection;
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Authorization", mtoken);
                urlConnection.setRequestProperty("Accept", "application/json");
                val writer: Writer =
                    BufferedWriter(OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8"));
                writer.write(JsonDATA);
                writer.close();
                val inputStream: InputStream = urlConnection.getInputStream();
                if (inputStream == null) {
                    return null;
                }
                reader = BufferedReader(InputStreamReader(inputStream))
                var inputLine: String? = reader.readLine()
                if (inputLine.equals("null")) {
                    return null
                } else {
                    handleUserData(inputLine)
                    return inputLine
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Connection Failed", ex);
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


    /**
     * Method to add uid and token to the header and do a post request
     * @author Ehsan Kabir
     */
    private fun postRequestToGetDashboardData() {
        val r = JSONObject()
        r.put("uid", muid)
        r.put("token", mtoken)
        //#call to async class
        SendJsonDataToServer().execute(r.toString());
    }

    /**
     * Inner class to get dashboard data by calling our
     * endpoint "https://www.ecoders.ca/getSensorData"
     *
     * @author Ehsan Kabir
     */
    inner class SendJsonDataToServer :
        AsyncTask<String?, String?, String?>() {

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            progressbarDashboard.visibility = View.GONE
            dashboardLayout.visibility = View.VISIBLE
            if (result.equals(null)) {
                val t = Toast.makeText(
                    this@Home,
                    "No devices to display\nPlease setup a device first.\nGo to device setup in the menu for walkthrough",
                    Toast.LENGTH_LONG
                )
                t.setGravity(Gravity.CENTER, 0, 0)
                t.show()
            } else {
                intentForUnique.putExtra("FirstEndpointData", result)
                intentForUnique.putExtra("userDevices", userDevices)
                var list = handleJson(result)
                adapter.submitList(list)
                dashboardItem_list.adapter = adapter
                adapter.notifyDataSetChanged();
                dashboardItem_list.smoothScrollToPosition(0);
            }
        }

        override fun doInBackground(vararg params: String?): String? {
            val JsonDATA = params[0]!!
            var urlConnection: HttpURLConnection? = null
            var reader: BufferedReader? = null

            try {
                val url = URL("https://ecoders.nikhilkapadia.com/getSensorData");
                urlConnection = url.openConnection() as HttpURLConnection;
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Authorization", mtoken);
                urlConnection.setRequestProperty("Accept", "application/json");
                val writer: Writer =
                    BufferedWriter(OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8"));
                writer.write(JsonDATA);
                writer.close();
                val inputStream: InputStream = urlConnection.getInputStream();
                if (inputStream == null) {
                    return null;
                }
                reader = BufferedReader(InputStreamReader(inputStream))
                var inputLine: String? = reader.readLine()
                if (inputLine.equals("null")) {
                    return null
                } else {
                    return inputLine
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Connection Failed", ex);
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

    /**
     * This converts json String to a Array list
     * In this case, it is converting the data retrieved from the
     * endpoint /getSensorData and converting it to an arraylist
     * of SensorData
     *
     * @return ArrayList<SensorData>
     * @author Ehsan Kabir
     */
    private fun handleJson(jsonString: String?): ArrayList<SensorData> {
        val jsonArray = JSONArray(jsonString)
        val list = ArrayList<SensorData>()
        var x = 0
        while (x < jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(x)
            list.add(
                SensorData(
                    jsonObject.getInt("deviceId"),
                    userDevices[x],
                    jsonObject.getInt("battery"),
                    jsonObject.getString("dateTime"),
                    jsonObject.getInt("airValue"),
                    jsonObject.getInt("waterValue"),
                    jsonObject.getInt("soilMoistureValue"),
                    jsonObject.getInt("soilMoisturePercent")
                )
            )
            x++
        }
        return list
    }

    /**
     * This converts json String to a Array list
     * In this case, it is converting the data retrieved from the
     * endpoint /login and extracting user information and the
     * devices under his account
     *
     * @author Ehsan Kabir
     */
    private fun handleUserData(jsonString: String?) {
        var out1 = jsonString!!.replace("{", "")
        var out2 = out1.replace("}", "")
        var out3 = out2.replace("]", "")
        var out4 = out3.replace(":", ",")
        var out5 = out4.replace("\"", "")
        var out6 = out5.replace("\"", "")
        var out7 = out6.split(",").toTypedArray()

        userUid = out7[1].replace("\"", "")
        userEmail = out7[3].replace("\"", "")
        userFirstName = out7[5].replace("\"", "")
        userLastName = out7[7].replace("\"", "")


        var x = 12
        while (x < out7.size) {
            userDevices.add(out7[x].replace("\"", ""))
            x = x + 4
        }

        var y = 10
        while (y < out7.size) {
            deviceId.add(out7[y].replace("\"", ""))
            y = y + 4
        }


        //Setting the name of the user on the Top of navigation menu
        val name = navigationView.getHeaderView(0)
            .findViewById(R.id.textView_dashboard_header_name) as TextView
        name.text = userFirstName + " " + userLastName
    }

}
