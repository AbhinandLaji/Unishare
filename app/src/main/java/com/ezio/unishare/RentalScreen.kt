package com.ezio.unishare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun RentalScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Rentals Page", style = MaterialTheme.typography.headlineMedium)
        // You can add more UI elements for your rentals page here
    }
}

@Preview(showBackground = true)
@Composable
fun RentalScreenPreview() {
    MaterialTheme { // Use your app's theme if available, otherwise MaterialTheme
        RentalScreen()
    }
}
