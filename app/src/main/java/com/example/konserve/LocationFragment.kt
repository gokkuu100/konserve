package com.example.konserve

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.konserve.databinding.LocationFragmentBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location
import android.content.Context

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
            enableLocationComponent()
        } else {
            Toast.makeText(context, "Location permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = LocationFragmentBinding.inflate(inflater, container, false)
        mapView = binding.mapView

        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
            centerMapOnKenya()
            checkLocationPermission()
            addRecyclingCenterPins(style)
        }

        // Add my location button click listener
        binding.myLocationButton.setOnClickListener {
            centerOnUserLocation()
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

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableLocationComponent()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun enableLocationComponent() {
        try {
            mapView.location.updateSettings {
                enabled = true
                pulsingEnabled = true
                pulsingColor = ContextCompat.getColor(requireContext(), R.color.black)
            }
        } catch (e: Exception) {
            Log.e("LocationFragment", "Error enabling location component", e)
        }
    }

    private fun centerOnUserLocation() {
        try {
            val locationComponent = mapView.location

            // Check if location is enabled
            if (!locationComponent.enabled) {
                locationComponent.updateSettings {
                    enabled = true
                    pulsingEnabled = true
                    pulsingColor = ContextCompat.getColor(requireContext(), R.color.black)
                }
            }

            // Get current location using indicator position
            locationComponent.addOnIndicatorPositionChangedListener { point ->
                animateCamera(point)
                // We don't need to remove the listener since we're not storing it
            }

        } catch (e: Exception) {
            Log.e("LocationFragment", "Error getting location", e)
            Toast.makeText(requireContext(), "Error getting location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addRecyclingCenterPins(style: Style) {
        try {
            val featureCollection = FeatureCollection.fromJson(geoJsonString)
            pointAnnotationManager = mapView.annotations.createPointAnnotationManager()

            val recyclingMarkerBitmap = getBitmapFromVectorDrawable(
                requireContext(),
                R.drawable.ic_recycling_marker
            )

            if (recyclingMarkerBitmap != null) {
                style.addImage("recycling-marker", recyclingMarkerBitmap)
            }

            for (feature in featureCollection.features() ?: emptyList()) {
                val point = feature.geometry() as Point
                val name = feature.getStringProperty("name")
                val description = feature.getStringProperty("description")

                val pointAnnotationOptions = PointAnnotationOptions()
                    .withPoint(point)
                    .withIconImage("recycling-marker")
                    .withIconSize(1.5)

                val pointAnnotation = pointAnnotationManager.create(pointAnnotationOptions)

                pointAnnotationManager.addClickListener {
                    if (it.id == pointAnnotation.id) {
                        showInfoBottomSheet(name, description, point)
                        true
                    } else {
                        false
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LocationFragment", "Error adding recycling center pins", e)
        }
    }

    private fun showInfoBottomSheet(name: String, description: String, point: Point) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.map_info_bottom_sheet, null)

        view.findViewById<TextView>(R.id.locationName).text = name
        view.findViewById<TextView>(R.id.locationDescription).text =
            description.replace("<br>", "\n")

        view.findViewById<Button>(R.id.directionsButton).setOnClickListener {
            openDirections(point)
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    private fun openDirections(destination: Point) {
        try {
            // Use a generic maps URL that works with multiple map apps
            val uri = Uri.parse(
                "geo:0,0?q=${destination.latitude()},${destination.longitude()}"
            )
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("LocationFragment", "Error opening directions", e)
            Toast.makeText(
                requireContext(),
                "No maps application found",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap? {
        return try {
            val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            Log.e("LocationFragment", "Error creating bitmap from drawable", e)
            null
        }
    }

    private fun animateCamera(point: Point, zoom: Double = 15.0) {
        try {
            val cameraOptions = CameraOptions.Builder()
                .center(point)
                .zoom(zoom)
                .build()

            mapView.mapboxMap.flyTo(
                cameraOptions,
                MapAnimationOptions.Builder()
                    .duration(1000)
                    .build()
            )
        } catch (e: Exception) {
            Log.e("LocationFragment", "Error animating camera", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }
}