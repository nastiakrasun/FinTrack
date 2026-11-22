package com.fintrack.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.app.ui.theme.FinTrackTheme

@Composable
fun FinTrackHomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "FinTrack",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Personal Finance Manager",
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = "Take control of your money",
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 24.dp)
        )

        Button(
            onClick = { },
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Text("Get Started")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FinTrackPreview() {
    FinTrackTheme {
        FinTrackHomeScreen()
    }
}
