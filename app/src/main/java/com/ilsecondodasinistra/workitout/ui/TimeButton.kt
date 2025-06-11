package com.ilsecondodasinistra.workitout.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TimeButton(
    text: String,
    time: String,
    buttonColor: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier =
            Modifier
                .width(160.dp) // Fixed width for consistent sizing
                .height(100.dp),
        // Fixed height
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 4.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = text, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = time, fontSize = 16.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}