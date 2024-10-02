package com.example.konserve

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.konserve.databinding.LocationFragmentBinding
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import org.json.JSONObject

class LocationFragment : Fragment() {

    private lateinit var binding: LocationFragmentBinding
    private lateinit var mapView: MapView
    private lateinit var pointAnnotationManager: PointAnnotationManager

    private val geoJsonString = """
    {
    "features": [
        {
            "geometry": {
                "coordinates": [
                    36.877799278963,
                    -1.22673611761586
                ],
                "type": "Point"
            },
            "properties": {
                "description": "Metal/Aluminium<br>Cardboard/Paper/Tetrapak<br>Plastic Containers<br>PET bottles<br><br>Location: Entry roundabout",
                "name": "Garden City Mall 4-in-1\n"
            },
            "type": "Feature"
        },
        {
            "geometry": {
                "coordinates": [
                    36.7023952882332,
                    -1.3123735078513
                ],
                "type": "Point"
            },
            "properties": {
                "description": "Metal/Aluminium<br>Cardboard/Paper/Tetrapak<br>Plastic Containers<br>PET bottles<br><br>Location: Right-hand side from exit",
                "name": "The Hub Karen 4-in-1\n"
            },
            "type": "Feature"
        },
        {
            "geometry": {
                "coordinates": [
                    36.7884312472338,
                    -1.28738310977764
                ],
                "type": "Point"
            },
            "properties": {
                "description": "Metal/Aluminium<br>Cardboard/Paper/Tetrapak<br>Plastic Containers<br>PET bottles<br><br>Location: Main pedestrian entry",
                "name": "Yaya Centre 4-in-1\n"
            },
            "type": "Feature"
        },
        {
            "geometry": {
                "coordinates": [
                    36.8019204790111,
                    -1.25397075499649
                ],
                "type": "Point"
            },
            "properties": {
                "description": "Metal/Aluminium<br>Cardboard/Paper/Tetrapak<br>Plastic Containers<br>PET bottles<br><br>Location: Lower Kabete Entry",
                "name": "Sarit Centre 4-in-1\n"
            },
            "type": "Feature"
        },
        {
            "geometry": {
                "coordinates": [
                    36.7133590336962,
                    -1.32453425019841
                ],
                "type": "Point"
            },
            "properties": {
                "description": "Metal/Aluminium<br>Cardboard/Paper/Tetrapak<br>Plastic Containers<br>PET bottles<br><br>Location: Main parking",
                "name": "Waterfront Karen 4-in-1\n"
            },
            "type": "Feature"
        },
        {
            "geometry": {
                "coordinates": [
                    36.8317678334769,
                    -1.2013373372329
                ],
                "type": "Point"
            },
            "properties": {
                "description": "Metal/Aluminium<br>Cardboard/Paper/Tetrapak<br>Plastic Containers<br>PET bottles<br><br>Location: Pedestrian entry",
                "name": "Nairobi Farmers Market 4-in-1\n"
            },
            "type": "Feature"
        },
        {
            "geometry": {
                "coordinates": [
                    36.8341484563918,
                    -1.30737300360881
                ],
                "type": "Point"
            },
            "properties": {
                "description": "Metal/Aluminium<br>Cardboard/Paper/Tetrapak<br>Plastic Containers<br>PET bottles<br><br>Location: Outside Artcaffe",
                "name": "Capital Centre 4-in-1\n"
            },
            "type": "Feature"
        },
        {
            "geometry": {
                "coordinates": [
                    36.7840723336259,
                    -1.28467719122485
                ],
                "type": "Point"
            },
            "properties": {
                "description": "Metal/Aluminium<br>Cardboard/Paper/Tetrapak<br>Plastic Containers<br>PET bottles",
                "name": "Shell Arging 4-in-1\n"
            },
            "type": "Feature"
        },
        {
            "geometry": {
                "coordinates": [
                    36.8380232107866,
                    -1.22241646644833
                ],
                "type": "Point"
            },
            "properties": {
                "description": "Metal/Aluminium<br>Cardboard/Paper/Tetrapak<br>Plastic Containers<br>PET bottles<br><br>Location: Next to exit",
                "name": "Ciata Mall,4-in-1 \n"
            },
            "type": "Feature"
        },
        {
            "geometry": {
                "coordinates": [
                    36.7932719107611,
                    -1.20350440095644
                ],
                "type": "Point"
            },
            "properties": {
                "description": "Metal/Aluminium<br>Cardboard/Paper/Tetrapak<br>Plastic Containers<br>PET bottles<br><br>Location: Basement 1 opposite escalator ramp",
                "name": "Two Rivers Mall 4-in-1\n"
            },
            "type": "Feature"
        },
        {
            "geometry": {
                "coordinates": [
                    36.7649754791631,
                    -1.33733336464252
                ],
                "type": "Point"
            },
            "properties": {
                "description": "Metal/Aluminium<br>Cardboard/Paper/Tetrapak<br>Plastic Containers<br>PET bottles<br><br>Location: Left of entry",
                "name": "Galleria Mall, 4-in-1\n"
            },
            "type": "Feature"
        },
        {
            "geometry": {
                "coordinates": [
                    36.7913998335572,
                    -1.24697526341762
                ],
                "type": "Point"
            },
            "properties": {
                "description": "Metal/Aluminium<br>Cardboard/Paper/Tetrapak<br>Plastic Containers<br>PET bottles",
                "name": "Spring Valley Coffee 4-in-1\n"
            },
            "type": "Feature"
        },
        {
            "geometry": {
                "coordinates": [
                    36.8070081471676,
                    -1.25146725394106
                ],
                "type": "Point"
            },
            "properties": {
                "description": "Plastic Containers<br>Pet Bottles",
                "name": "The place@67, General Mathenge 2-in-1\n"
            },
            "type": "Feature"
        },
        {
            "geometry": {
                "coordinates": [
                    36.826262869902,
                    -1.25672761920116
                ],
                "type": "Point"
            },
            "properties": {
                "description": "Plastic Containers<br>Pet Bottles",
                "name": "Onn the Way, Limuru Road: 2-in-1"
            },
            "type": "Feature"
        },
        {
            "geometry": {
                "coordinates": [
                    36.8398411244551,
                    -1.25364603448047
                ],
                "type": "Point"
            },
            "properties": {
                "description": "Plastic Containers<br>Pet Bottles",
                "name": "Chandarana, Muthaiga: 2-in-1"
            },
            "type": "Feature"
        },
        {
            "geometry": {
                "coordinates": [
                    36.7944694698541,
                    -1.23246142643811
                ],
                "type": "Point"
            },
            "properties": {
                "description": "Plastic Containers<br>Pet Bottles",
                "name": "Chandarana, New Muthaiga Mall: 2-in-1"
            },
            "type": "Feature"
        },
        {
            "geometry": {
                "coordinates": [
                    36.8379903426218,
                    -1.21813367219964
                ],
                "type": "Point"
            },
            "properties": {
                "description": "Plastic Containers<br>Pet Bottles",
                "name": "Chandarana, Ridgeways Mall: 2-in-1"
            },
            "type": "Feature"
        },
        {
            "geometry": {
                "coordinates": [
                    36.7778505790099,
                    -1.25604497752791
                ],
                "type": "Point"
            },
            "properties": {
                "description": "Plastic Containers<br>Pet Bottles",
                "name": "Zucchini, ABC Place: 2-in-1"
            },
            "type": "Feature"
        },
        {
            "geometry": {
                "coordinates": [
                    36.7047811245631,
                    -1.31437920691551
                ],
                "type": "Point"
            },
            "properties": {
                "description": "Plastic Containers<br>Pet Bottles",
                "name": "Zucchini, The Hub: 2-in-1"
            },
            "type": "Feature"
        },
        {
            "geometry": {
                "coordinates": [
                    36.749977556494,
                    -1.36720099775713
                ],
                "type": "Point"
            },
            "properties": {
                "description": "Plastic Containers<br>Pet Bottles",
                "name": "Green Spoon, Langata: 2-in-1"
            },
            "type": "Feature"
        },
        {
            "geometry": {
                "coordinates": [
                    36.7561757337167,
                    -1.33671318003037
                ],
                "type": "Point"
            },
            "properties": {
                "description": "Plastic Containers<br>Pet Bottles",
                "name": "The Well, Karen: 2-in-1"
            },
            "type": "Feature"
        }
    ],
        "type": "FeatureCollection"
    }
    """

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            mapView.mapboxMap.getStyle { _ ->
                enableLocationComponent()
            }
        } else {
            Toast.makeText(context, "Location permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = LocationFragmentBinding.inflate(inflater, container, false)
        mapView = binding.mapView

        mapView.mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
            centerMapOnKenya()
            checkLocationPermissionsAndEnable()
            addRecyclingCenterPins(style)
        }

        return binding.root
    }

    private fun centerMapOnKenya() {
        val nairobiCenter = CameraOptions.Builder()
            .center(Point.fromLngLat(36.8219, -1.2921))
            .zoom(10.0)
            .build()
        mapView.mapboxMap.setCamera(nairobiCenter)
    }

    private fun checkLocationPermissionsAndEnable() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableLocationComponent()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun enableLocationComponent() {
        val locationComponentPlugin = mapView.location
        locationComponentPlugin.updateSettings {
            enabled = true
        }

        locationComponentPlugin.addOnIndicatorPositionChangedListener(
            object : OnIndicatorPositionChangedListener {
                override fun onIndicatorPositionChanged(point: Point) {
                    mapView.getMapboxMap().setCamera(
                        CameraOptions.Builder()
                            .center(point)
                            .zoom(10.0)
                            .build()
                    )
                    // Optionally, add a marker at the user's location
                    locationComponentPlugin.removeOnIndicatorPositionChangedListener(this)
                }
            }
        )
    }

    private fun addRecyclingCenterPins(style: Style) {
        val featureCollection = FeatureCollection.fromJson(geoJsonString)

        pointAnnotationManager = mapView.annotations.createPointAnnotationManager()

        fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap? {
            val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }

        val bitmap = getBitmapFromVectorDrawable(requireContext(), R.drawable.pointer_icon_foreground)
        if (bitmap != null) {
            style.addImage("custom-marker", bitmap)
        } else {
            Log.e("MapActivity", "Failed to load vector drawable as bitmap")
        }

        for (feature in featureCollection.features() ?: emptyList()) {
            val point = feature.geometry() as Point
            val name = feature.getStringProperty("name")
            val description = feature.getStringProperty("description")

            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage("custom-marker")

            val pointAnnotation = pointAnnotationManager.create(pointAnnotationOptions)

            pointAnnotationManager.addClickListener {
                if (it == pointAnnotation) {
                    showInfoWindow(name, description)
                    true
                } else {
                    false
                }
            }
        }
    }


    private fun showInfoWindow(name: String, description: String) {
        Toast.makeText(requireContext(), "$name\n$description", Toast.LENGTH_LONG).show()
    }
}