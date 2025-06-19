package com.ilsecondodasinistra.workitout.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    small = RoundedCornerShape(8.dp),   // M3 default is 4.dp, but can be customized
    medium = RoundedCornerShape(12.dp), // M3 default is 12.dp
    large = RoundedCornerShape(16.dp)   // M3 default is 0.dp or specific component shapes
)