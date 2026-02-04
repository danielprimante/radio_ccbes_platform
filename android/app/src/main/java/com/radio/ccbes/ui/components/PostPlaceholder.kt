package com.radio.ccbes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PostPlaceholder() {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(Color.LightGray.copy(alpha = 0.2f), CircleShape))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Box(Modifier.width(100.dp).height(12.dp).background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(4.dp))
                Box(Modifier.width(60.dp).height(10.dp).background(Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(14.dp).background(Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
        Spacer(modifier = Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth(0.7f).height(14.dp).background(Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
    }
}
