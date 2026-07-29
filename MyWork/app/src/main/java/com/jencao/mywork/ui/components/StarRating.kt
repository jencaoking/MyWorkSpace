package com.jencao.mywork.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** 通用星级评分（点击设置，再次点击同星取消）。 */
@Composable
fun StarRating(
    rating: Int,
    max: Int = 5,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        for (i in 1..max) {
            IconButton(onClick = { onRatingChange(if (rating == i) 0 else i) }) {
                Icon(
                    imageVector = if (i <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "$i 星",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
