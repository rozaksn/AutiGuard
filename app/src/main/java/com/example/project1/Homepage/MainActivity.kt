package com.example.project1.Homepage

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.health.connect.datatypes.units.Power
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.project1.Notification.NotificationSetup
import com.example.project1.R
import com.example.project1.databinding.ActivityMainBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private val binding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private lateinit var mMaps:GoogleMap

    private val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }
    private val notificationSetup by lazy { NotificationSetup(this) }
    private var centerLocation: LatLng? = null
    private var currentMarker: Marker? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        if (savedInstanceState == null){
        val mapFragment=supportFragmentManager.findFragmentById(R.id.fMap) as? SupportMapFragment
        mapFragment?.getMapAsync(this@MainActivity)
        }

        //notificationSetup = NotificationSetup(this)

        displayData()
        checkLocationServices()
        checkBatteryOptimizationPermission()
        checkNotificationPermission()


    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMaps = googleMap
        fetchLocationUpdate()
        setPointLocation()
    }

    private fun fetchLocationUpdate(){
        val locationRef = database.getReference("Location")
        locationRef.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (locationSnapshot in snapshot.children){
                    val latitude = locationSnapshot.child("lat").getValue().toString().toDouble()
                    val longitude = locationSnapshot.child("lng").getValue().toString().toDouble()

                    updateLocation(latitude, longitude)


                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Error", R.string.error_msg.toString())
            }

        })
    }
    private fun setPointLocation(){
        val setPointRef = database.getReference("Locations")
        setPointRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (locationSnapshot in snapshot.children){
                    val latitude = locationSnapshot.child("lat").getValue().toString().toDouble()
                    val longitude = locationSnapshot.child("lng").getValue().toString().toDouble()
                    centerLocation = LatLng(latitude, longitude)
                    setLocation(latitude,longitude)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Error", R.string.error_msg.toString())
            }

        })
    }
    private fun setLocation(lat: Double,lng: Double){
        val location = LatLng(lat,lng)
        val rangeRef = database.getReference("Sensor/MaxRange")
        rangeRef.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val radius = snapshot.getValue().toString().toDouble()
                centerLocation?.let {
                    mMaps.addCircle(CircleOptions().center(location).radius(radius)
                    .strokeColor(Color.RED).strokeWidth(5f)
                    .fillColor(Color.argb(50, 255, 0, 0)))
                }

            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Error", R.string.error_msg.toString())
            }

        })

    }
    private fun updateLocation(lat:Double,lng:Double){
        val location = LatLng(lat, lng)
        // Remove the previous marker if it exists
        currentMarker?.remove()

       currentMarker= mMaps.addMarker(MarkerOptions().position(location)
           .title(getString(R.string.now_location))
           .snippet(location.toString()))
        mMaps.moveCamera(CameraUpdateFactory.newLatLngZoom(location,17f))
        val radiusRef = database.getReference("Sensor/MaxRange")
        radiusRef.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val range = snapshot.getValue().toString().toDouble()
                centerLocation?.let {

                    // Calculate the distance
                    val distance = FloatArray(1)
                    Location.distanceBetween(lat, lng, it.latitude, it.longitude, distance)
                    if (distance[0] > range) {
                        notificationSetup.sendRangeNotification()
                        Toast.makeText(this@MainActivity, getString(R.string.out_range), Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Error", R.string.error_msg.toString())
            }

        })

    }


    private fun displayData(){
        val dataRef=database.getReference("Sensor/Loudness")
        dataRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val dbmData = snapshot.getValue().toString()
                dbmData?.let {data->
                    binding.tvdbm.text= "$dbmData dB"
                    if (data > "59"){
                        binding.tvdbm.setTextColor(getColor(R.color.red))
                        notificationSetup.sendLoudnessNotification()
                    }else{
                        binding.tvdbm.setTextColor(getColor(R.color.green))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("TAG", getString(R.string.failed_to_read_value), error.toException())
            }

        })
    }
    private fun checkLocationServices() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            Toast.makeText(this, "Location services are disabled", Toast.LENGTH_LONG).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Location services are enabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestPermission(){
        ActivityCompat.requestPermissions(
            this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),0
        )
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS),1)
    }

    private fun checkNotificationPermission(){
        if (ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ){

        }else{
            requestPermission()
        }
    }

    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // Buat Intent untuk membuka halaman pengaturan optimasi baterai.
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)

            // Tetapkan URI data untuk menyertakan nama paket aplikasi.
            //untuk memeuat sumber daya dari string yang mencakup paket nama aplikasi (packageName).
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }

    private fun checkBatteryOptimizationPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            val packageName = packageName
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

            //jika optmasi baterai diabaikan
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)){
                requestBatteryOptimization()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0){
            Toast.makeText(this, getString(R.string.notification_granted), Toast.LENGTH_SHORT).show()
        }else{
            Toast.makeText(this, getString(R.string.notification_ungranted), Toast.LENGTH_SHORT).show()
        }
        if (requestCode==1){
            if (grantResults.isNotEmpty() && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, getString(R.string.notification_granted), Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this, getString(R.string.notification_ungranted), Toast.LENGTH_SHORT).show()
            }
        }
    }

}