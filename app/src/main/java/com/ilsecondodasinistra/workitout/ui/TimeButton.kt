package com.ilsecondodasinistra.workitout.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TimeButton(
    text: String,
    time: String,
    buttonColor: Color,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    enabled: Boolean = true, // Optional parameter to enable/disable the button
) {
    Row {
        Button(
            onClick = { if (enabled) onClick() }, // Only call onClick if enabled
            modifier =
                Modifier
                    .padding(start = 40.dp) // Padding to align with the icon
                    .width(180.dp) // Fixed width for consistent sizing
                    .height(100.dp),
            // Fixed height
            colors = if (enabled) ButtonDefaults.buttonColors(containerColor = buttonColor) else ButtonDefaults.buttonColors(containerColor = Color.Gray),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 4.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = text, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = time, fontSize = 16.sp, color = Color.White.copy(alpha = 0.8f))
            }
        }
        // The icon of a pencil, tappable in order to edit the time
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Edit,
            contentDescription = "Edit Time",
            modifier =
                Modifier
                    .align(Alignment.CenterVertically)
                    .padding(start = 16.dp)
                    .width(32.dp)
                    .height(32.dp)
                    .clickable {
                        if (enabled) {
                            onEditClick()
                        }
                    },
            tint = Color(0xFF9A4616),
        )
    }
}

@Preview
@Composable
fun TimeButtonPreview() {
    TimeButton(
        text = "Workout",
        time = "30 min",
        buttonColor = Color(0xFF6200EE), // Example color
        onClick = { /* Do something */ },
        onEditClick = { /* Do something */ }
    )
}