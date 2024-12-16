package com.dessalines.thumbkey.ui.components.keyboard

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dessalines.thumbkey.utils.PredictionManager

@Suppress("ktlint")
@Composable
fun PredictionBar(
    predictionManager: PredictionManager,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val suggestions by predictionManager.suggestions.collectAsState()
    Log.d("ThumbKey", "PredictionBar recomposed with suggestions: $suggestions")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color(0xFF2196F3))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (suggestions.isNotEmpty()) {
            Log.d("ThumbKey", "Rendering suggestions: $suggestions")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(suggestions) { suggestion ->
                    Text(
                        text = suggestion,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .background(
                                color = Color.White,
                                shape = MaterialTheme.shapes.small,
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clickable { onSuggestionClick(suggestion) },
                        color = Color.Black,
                    )
                }
            }
        } else {
            Text(
                text = "HAIL VECTRON",
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}
