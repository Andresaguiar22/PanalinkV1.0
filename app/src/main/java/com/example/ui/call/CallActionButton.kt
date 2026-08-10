package com.example.ui.call

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.bounceClick

/**
 * CallActionButton defines a highly polished circular interactive control
 * with proper touch targets, color schemes, and bouncy spring interactions.
 */
@Composable
fun CallActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Black.copy(alpha = 0.5f),
    contentColor: Color = Color.White,
    size: Dp = 56.dp,
    iconSize: Dp = 26.dp,
    label: String? = null,
    testTag: String? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        val baseButtonModifier = Modifier
            .size(size)
            .bounceClick(onClick)
            
        val buttonModifier = if (testTag != null) {
            baseButtonModifier.testTag(testTag)
        } else {
            baseButtonModifier
        }

        IconButton(
            onClick = {}, // Handled by bounceClick modifier below for beautiful physics feedback
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            modifier = buttonModifier
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )
        }

        if (label != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
    }
}
