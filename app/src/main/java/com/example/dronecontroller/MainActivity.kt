package com.example.dronecontroller

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.example.dronecontroller.ui.theme.DroneControllerTheme
import com.example.dronecontroller.ui.theme.JoystickSize
import com.manalkaff.jetstick.JoyStick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.ConnectException
import kotlin.math.absoluteValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DroneControllerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GreetingWithIp(this)
                }
            }
        }
    }
}

@SuppressLint("CoroutineCreationDuringComposition", "ShowToast")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreetingWithIp(context: Context, modifier: Modifier = Modifier) {
    if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) {
        var text by remember { mutableStateOf(BASE_URL) }
        var clickable by remember { mutableStateOf(true) }

        Column(
            Modifier
                .fillMaxSize()
                .padding(top = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    Modifier.padding(4.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 20.sp)
                )
                Button(
                    onClick = {
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                Toast.makeText(context, "Подключение", Toast.LENGTH_LONG).show()
                                clickable = false
                                val result = RetrofitInstance.api.getMpuAcceleration()
                                if (result.isSuccessful) {
                                    BASE_URL = text
                                    Toast.makeText(context, "Подключено успешно", Toast.LENGTH_LONG)
                                        .show()
                                    (context as Activity).requestedOrientation =
                                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

                                } else {
                                    Toast.makeText(
                                        context,
                                        "Что-то пошло не так",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                            } catch (e: Exception) {
                                Log.e("MyTag", "Error: ${e}")
                                Toast.makeText(context, "Что-то пошло не так", Toast.LENGTH_SHORT)
                                    .show()
                                clickable = true
                            }
                        }
                    },
                    enabled = clickable,
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text(text = "Enter", color = if (clickable) Color.Green else Color.Red)
                }
            }
        }
    } else {
        (context as Activity).window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        var acceleration by remember {
            mutableStateOf(Acceleration())
        }
        Row(Modifier.fillMaxSize()) {
//            JoyStick()
            Column(Modifier.padding(10.dp)) {
                Text(text = "X: ${acceleration.xAc}")
                Text(text = "Y: ${acceleration.yAc}")
                Text(text = "Z: ${acceleration.zAc}")
//                Text(text = "XZ angle: ${acceleration.xzAngle}")
//                Text(text = "YZ angle: ${acceleration.yzAngle}")
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.joystick_background),
                        contentDescription = "Joystick Background",
                        Modifier.size(JoystickSize),
                    )
                    Image(
                        painter = painterResource(id = R.drawable.joystick_ball),
                        contentDescription = "Joystick Ball",
                        Modifier
                            .size(JoystickSize / 5)
                            .offset(
                                -acceleration.xAc* ((JoystickSize / 2) - JoystickSize / 10),
                                acceleration.yAc * ((JoystickSize / 2) - JoystickSize / 10)
                            )
                    )

                }
            }
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    while (true) {
                        Log.d("MyTag", "repeat")
                        try {
                            val result = RetrofitInstance.api.getMpuAcceleration()
                            if (result.isSuccessful) {
                                acceleration = result.body()!!
                            }
                        } catch (e: Exception) {
                            Log.e("MyTag", "Error: ${e.message}")
                            if (e is ConnectException) {
                                (context as Activity).requestedOrientation =
                                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                Toast.makeText(context, "Подключение прервано", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                        delay(100)
                    }
                }
            }

            Button(onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        RetrofitInstance.api.mpuCalibrate()
                    } catch (e: Exception) {
                        Log.e("MyTag", "Error: ${e.message}")
                    }
                }
            }) {
                Image(
                    painter = painterResource(id = R.drawable.calibration_image),
                    contentDescription = "Calibrate sensor"
                )
            }
            Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.End) {
                var sliderState by remember { mutableStateOf(0f) }
                var oldStableState by remember { mutableStateOf(0) }
                Slider(
                    value = sliderState, onValueChange = {
                        sliderState = it
                        if (((sliderState * 100).toInt() - oldStableState).absoluteValue > 1) {
                            oldStableState = (sliderState * 100).toInt()
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    RetrofitInstance.api.setMotorPower(oldStableState)
                                } catch (e: Exception) {
                                    Log.e("MyTag", "Error: ${e.message}")
                                }
                            }
                            Log.d("MyTag", "Power: $oldStableState")
                        }
                    },
                    onValueChangeFinished = {
                        sliderState = 0f
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                RetrofitInstance.api.setMotorPower(0)
                            } catch (e: Exception) {
                                Log.e("MyTag", "Error: ${e.message}")
                            }
                        }
                    },
                    modifier = modifier
                        .graphicsLayer {
                            rotationZ = 270f
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(
                                Constraints(
                                    minWidth = constraints.minHeight,
                                    maxWidth = constraints.maxHeight,
                                    minHeight = constraints.minWidth,
                                    maxHeight = constraints.maxHeight,
                                )
                            )
                            layout(placeable.height, placeable.width) {
                                placeable.place(-placeable.width, 0)
                            }
                        }
                        .padding(vertical = 20.dp, horizontal = 40.dp)
//                    .width(120.dp)
                        .height(80.dp)
                )
            }
        }
    }
}
