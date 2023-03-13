package com.pokerio.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview

fun getAnimatedString() = "Jetpack Compose"

@Preview
@Composable
fun HomeScreen() {
    var expanded by remember { mutableStateOf(false) }

    Card {
        Column(Modifier.clickable { expanded = !expanded }) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "example image"
            )
            AnimatedVisibility(expanded) {
                Text(
                    modifier = Modifier.testTag("image_caption"),
                    text = getAnimatedString(),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
