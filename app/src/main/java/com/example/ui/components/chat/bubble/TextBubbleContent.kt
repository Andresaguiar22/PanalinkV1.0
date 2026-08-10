package com.example.ui.components.chat.bubble

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextBubbleContent(
    text: String,
    textSizeSp: Float = 15f,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier,
    statusIndicator: (@Composable () -> Unit)? = null
) {
    if (statusIndicator != null) {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = textSizeSp.sp,
                modifier = Modifier
                    .testTag("message_text")
                    .padding(end = 6.dp)
            )
            statusIndicator()
        }
    } else {
        Text(
            text = text,
            color = textColor,
            fontSize = textSizeSp.sp,
            modifier = modifier.testTag("message_text")
        )
    }
}

