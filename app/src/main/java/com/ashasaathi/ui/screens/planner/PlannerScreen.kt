package com.ashasaathi.ui.screens.planner

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ashasaathi.data.model.Patient
import com.ashasaathi.ui.components.RiskBadge
import com.ashasaathi.ui.screens.home.AppBottomBar
import com.ashasaathi.ui.navigation.Route
import com.ashasaathi.ui.strings.appStrings
import com.ashasaathi.ui.theme.*
import com.ashasaathi.ui.viewmodel.PlannerViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    navController: NavController,
    vm: PlannerViewModel = hiltViewModel()
) {
    val patients by vm.prioritizedPatients.collectAsState()
    val loading  by vm.loading.collectAsState()
    var showMap  by remember { mutableStateOf(false) }
    val s = appStrings()

    Scaffold(
        containerColor = WarmBackground,
        bottomBar = { AppBottomBar(navController) },
        topBar = {
            TopAppBar(
                title = { Text(s.plannerTitle, color = Color.White) },
                actions = {
                    IconButton({ showMap = !showMap }) {
                        Icon(
                            if (showMap) Icons.Default.List else Icons.Default.Map,
                            null,
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Saffron)
            )
        }
    ) { padding ->
        if (showMap) {
            OSMMapView(
                patients = patients,
                onMarkerClick = { navController.navigate(Route.patientDetail(it.patientId)) },
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        } else {
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Saffron)
                }
            } else if (patients.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(s.homeAllGood, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            } else {
                LazyColumn(
                    Modifier.padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            s.plannerSubtitle.format(patients.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    itemsIndexed(patients, key = { _, p -> p.patientId }) { index, patient ->
                        PlannerCard(
                            patient = patient,
                            index = index + 1,
                            onVisit = { navController.navigate(Route.visitForm(patient.patientId)) },
                            onMarkDone = { vm.markVisited(patient.patientId) },
                            alreadyVisited = vm.isVisited(patient.patientId)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlannerCard(
    patient: Patient,
    index: Int,
    onVisit: () -> Unit,
    onMarkDone: () -> Unit,
    alreadyVisited: Boolean
) {
    val s = appStrings()
    val riskColor = when (patient.currentRiskLevel) {
        "RED" -> RiskRed; "YELLOW" -> RiskAmber; else -> RiskGreen
    }
    var offsetX by remember { mutableFloatStateOf(0f) }
    val bg by animateColorAsState(
        if (alreadyVisited) RiskGreenSurface else Color.White,
        label = "cardBg"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(if (alreadyVisited) 0.dp else 2.dp)
    ) {
        Row(
            Modifier
                .height(IntrinsicSize.Min)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX > 150f && !alreadyVisited) onMarkDone()
                            offsetX = 0f
                        }
                    ) { _, delta -> offsetX += delta }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number badge
            Box(
                Modifier.width(48.dp).fillMaxHeight().background(riskColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    index.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                Modifier.weight(1f).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(patient.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (alreadyVisited) {
                        Text(s.plannerVisitDone, style = MaterialTheme.typography.labelSmall, color = RiskGreen)
                    } else {
                        RiskBadge(patient.currentRiskLevel)
                    }
                }
                patient.village?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            if (!alreadyVisited) {
                IconButton(onClick = onVisit) {
                    Icon(Icons.Default.PlayArrow, "Start Visit", tint = Saffron)
                }
            }
        }
    }
}

@Composable
private fun OSMMapView(
    patients: List<Patient>,
    onMarkerClick: (Patient) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            Configuration.getInstance().userAgentValue = ctx.packageName
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(14.0)
            }
        },
        update = { mapView ->
            mapView.overlays.clear()
            patients.forEach { patient ->
                if (patient.location.latitude != 0.0) {
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(patient.location.latitude, patient.location.longitude)
                        title = patient.name
                        snippet = patient.currentRiskLevel
                        setOnMarkerClickListener { _, _ -> onMarkerClick(patient); true }
                    }
                    mapView.overlays.add(marker)
                }
            }
            patients.firstOrNull()?.let { first ->
                if (first.location.latitude != 0.0) {
                    mapView.controller.setCenter(GeoPoint(first.location.latitude, first.location.longitude))
                }
            }
            mapView.invalidate()
        },
        modifier = modifier
    )
}
