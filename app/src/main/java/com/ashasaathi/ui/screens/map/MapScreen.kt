package com.ashasaathi.ui.screens.map

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ashasaathi.ui.theme.*
import com.ashasaathi.ui.viewmodel.MapMarker
import com.ashasaathi.ui.viewmodel.MapViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    vm: MapViewModel = hiltViewModel()
) {
    val markers  by vm.markers.collectAsState()
    val centerLat by vm.centerLat.collectAsState()
    val centerLng by vm.centerLng.collectAsState()
    val loading  by vm.loading.collectAsState()

    val context = LocalContext.current
    var selectedMarker by remember { mutableStateOf<MapMarker?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Scaffold(
        containerColor = WarmBackground,
        topBar = {
            TopAppBar(
                title = { Text("क्षेत्र नक्शा / Area Map", color = Color.White) },
                navigationIcon = {
                    IconButton({ navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton({
                        mapViewRef?.controller?.animateTo(GeoPoint(centerLat, centerLng))
                    }) {
                        Icon(Icons.Default.MyLocation, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Saffron)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    buildMapView(ctx).also { mapViewRef = it }
                },
                update = { mapView ->
                    updateMapMarkers(mapView, markers) { clicked ->
                        selectedMarker = clicked
                    }
                    if (!loading && markers.isNotEmpty()) {
                        mapView.controller.setZoom(14.0)
                        mapView.controller.setCenter(GeoPoint(centerLat, centerLng))
                    }
                }
            )

            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Saffron
                )
            }

            // Legend
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(RiskRed,   "उच्च जोखिम")
                LegendDot(RiskAmber, "मध्यम")
                LegendDot(RiskGreen, "सामान्य")
            }

            // Marker count chip
            if (!loading) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    color = Saffron,
                    shape = RoundedCornerShape(100)
                ) {
                    Text(
                        "${markers.size} स्थान",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Bottom sheet for selected marker
    selectedMarker?.let { m ->
        MarkerDetailSheet(
            marker = m,
            onDismiss = { selectedMarker = null }
        )
    }

    DisposableEffect(Unit) {
        onDispose { mapViewRef?.onDetach() }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(10.dp).background(color, androidx.compose.foundation.shape.CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkerDetailSheet(marker: MapMarker, onDismiss: () -> Unit) {
    val riskColor = when (marker.riskLevel) {
        "RED"    -> RiskRed
        "YELLOW" -> RiskAmber
        else     -> RiskGreen
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(14.dp).background(riskColor, androidx.compose.foundation.shape.CircleShape))
                Text(marker.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(marker.subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text(
                "📍 ${marker.lat.format(4)}, ${marker.lng.format(4)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextHint
            )
        }
    }
}

private fun Double.format(decimals: Int) = "%.${decimals}f".format(this)

private fun buildMapView(ctx: Context): MapView {
    return MapView(ctx).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        isTilesScaledToDpi = true
        controller.setZoom(14.0)
        controller.setCenter(GeoPoint(20.5937, 78.9629))
    }
}

private fun updateMapMarkers(
    mapView: MapView,
    markers: List<MapMarker>,
    onMarkerClick: (MapMarker) -> Unit
) {
    mapView.overlays.clear()
    markers.forEach { m ->
        val marker = Marker(mapView).apply {
            position = GeoPoint(m.lat, m.lng)
            title    = m.title
            snippet  = m.subtitle
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setOnMarkerClickListener { _, _ ->
                onMarkerClick(m)
                true
            }
        }
        mapView.overlays.add(marker)
    }
    mapView.invalidate()
}
