package edu.uark.ahnelson.assignment3solution.MainActivity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.osmdroid.config.Configuration.*


import edu.uark.ahnelson.assignment3solution.GeoPhotoApplication
import edu.uark.ahnelson.assignment3solution.R
import edu.uark.ahnelson.assignment3solution.Util.replaceFragmentInActivity
import org.osmdroid.util.GeoPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MapsActivity : AppCompatActivity() {

    private lateinit var mapsFragment: OpenStreetMapFragment
    var currentPhotoPath:String = ""

    val takePictureResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(applicationContext, "No picture taken", Toast.LENGTH_LONG)
        }else{
            Log.d("MainActivity","Picture Taken at location $currentPhotoPath")
        }
    }

    val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("MapsActivity","Permission Granted")
            } else {
                Toast.makeText(this,"Location Permissions not granted. Location disabled on map",Toast.LENGTH_LONG).show()
            }
        }


    private val mapsViewModel: MapsViewModel by viewModels {
        MapsViewModel.ToDoListViewModelFactory((application as GeoPhotoApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)


        findViewById<FloatingActionButton>(R.id.floatingActionButton).setOnClickListener{
            takeNewPhoto()
        }

        //Get preferences for tile cache
        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        //Check for location permissions
        checkForLocationPermission()

        //Get access to mapsFragment object
        mapsFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView)
                    as OpenStreetMapFragment? ?:OpenStreetMapFragment.newInstance().also{
                        replaceFragmentInActivity(it,R.id.fragmentContainerView)
        }

        //Begin observing data changes
        mapsViewModel.allGeoPhoto.observe(this){
            geoPhotos->
            geoPhotos.let {
                for(photo in geoPhotos){
                    val latitude = photo.value.latitude
                    val longitude = photo.value.longitude
                    val id = photo.value.id
                    var geoPoint:GeoPoint? = null

                    if(latitude!=null){
                        if(longitude!= null){
                            geoPoint = GeoPoint(latitude,longitude)
                        }
                    }
                    if(id != null && geoPoint!= null){
                        mapsFragment.addMarker(geoPoint,id)
                    }
                }
            }
        }
    }

    private fun createFilePath(): String {
        // Create an image file name
        val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )
        // Save a file: path for use with ACTION_VIEW intent
        return image.absolutePath
    }

    private fun takeNewPhoto(){
        val picIntent = Intent().setAction(MediaStore.ACTION_IMAGE_CAPTURE)
        if (picIntent.resolveActivity(packageManager) != null){
            val filepath: String = createFilePath()
            val myFile: File = File(filepath)
            currentPhotoPath = filepath
            val photoUri = FileProvider.getUriForFile(this,"edu.uark.ahnelson.assignment3solution.fileprovider",myFile)
            picIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoUri)
            takePictureResultLauncher.launch(picIntent)
        }
    }

    private fun checkForLocationPermission(){
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                //getLastKnownLocation()
                //registerLocationUpdateCallbacks()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

}