package com.example.practica10

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.widget.Toast
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun cameraScreen() {

    val context = LocalContext.current

    var listaImagenes by remember {
        mutableStateOf(listOf<Bitmap>())
    }

    var selectedIndex by remember {
        mutableStateOf<Int?>(null)
    }

    val camaraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            listaImagenes = listaImagenes + bitmap
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                camaraLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "No hay aplicación de cámara disponible", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = { Text("Cámara y Galería") },
                        colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val permissionCheck = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    )

                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        try {
                            camaraLauncher.launch(null)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No hay aplicación de cámara disponible", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Tomar Foto"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                if (listaImagenes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay fotos en la galería. ¡Toma una!")
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(listaImagenes) { index, bitmap ->
                            ImagencapturadaItem(
                                bitmap = bitmap,
                                onClick = {
                                    selectedIndex = index
                                }
                            )
                        }
                      }
                    }
                }
            }

            if (selectedIndex != null) {
                Dialog(
                    onDismissRequest = { selectedIndex = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    val pagerState = rememberPagerState(initialPage = selectedIndex!!) {
                        listaImagenes.size
                    }

                    val scales = remember {
                        mutableStateMapOf<Int, Float>()
                    }
                    val offsetsX = remember {
                        mutableStateMapOf<Int, Float>()
                    }
                    val offsetsY = remember {
                        mutableStateMapOf<Int, Float>()
                    }

                    val currentScale = scales[pagerState.currentPage] ?: 1f

                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = currentScale == 1f,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.95f))
                    ) { page ->

                        val scale = scales[page] ?: 1f
                        val offsetX = offsetsX[page] ?: 0f
                        val offsetY = offsetsY[page] ?: 0f

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            if ((scales[page] ?: 1f) == 1f) {
                                                selectedIndex = null
                                            }
                                        }
                                    )
                                }
                                .pointerInput(page) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)

                                        var pointerPressed = true

                                        do {
                                            val event = awaitPointerEvent()
                                            val activePointers = event.changes.count { it.pressed }
                                            val activeScale = scales[page] ?: 1f

                                            if (activePointers >= 2 || activeScale > 1f) {
                                                val zoom = event.calculateZoom()
                                                val pan = event.calculatePan()

                                                event.changes.forEach {
                                                    it.consume()
                                                }

                                                val newScale = (activeScale * zoom).coerceIn(1f, 4f)
                                                scales[page] = newScale

                                                if (newScale <= 1f) {
                                                    offsetsX[page] = 0f
                                                    offsetsY[page] = 0f
                                                } else {
                                                    offsetsX[page] = (offsetsX[page] ?: 0f) + pan.x
                                                    offsetsY[page] = (offsetsY[page] ?: 0f) + pan.y
                                                }
                                            }

                                            pointerPressed = event.changes.any { it.pressed }
                                        } while (pointerPressed)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = listaImagenes[page].asImageBitmap(),
                                contentDescription = "Imagen ampliada",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY
                                    )
                            )
                    }
                }
            }
        }
    }
}

@Composable
fun ImagencapturadaItem(bitmap: Bitmap, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable {
                onClick()
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(0.dp)
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Foto capturada",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
  }
