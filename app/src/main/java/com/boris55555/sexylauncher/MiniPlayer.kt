package com.boris55555.sexylauncher

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MiniPlayer(
    mediaMetadata: MediaMetadata?,
    playbackState: PlaybackState?,
    mediaController: MediaController?,
    onClose: () -> Unit
) {
    val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
    
    // Use strictly String keys to avoid any automatic object mapping
    val title = try {
        mediaMetadata?.getString(MediaMetadata.METADATA_KEY_TITLE) 
            ?: mediaMetadata?.description?.title?.toString() 
            ?: "Playing..."
    } catch (e: Exception) { "Playing..." }

    val artist = try {
        mediaMetadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) 
            ?: mediaMetadata?.description?.subtitle?.toString() 
            ?: ""
    } catch (e: Exception) { "" }

    // Use a Box with a solid background and clip to ensure nothing leaks out
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(BorderStroke(2.dp, Color.Black), shape = RoundedCornerShape(12.dp))
            .clickable {
                try {
                    mediaController?.sessionActivity?.send()
                } catch (e: Exception) {}
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 48.dp), // More padding to avoid edge issues
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title, 
                    fontWeight = FontWeight.Bold, 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis, 
                    color = Color.Black,
                    fontSize = 18.sp
                )
                if (artist.isNotBlank()) {
                    Text(
                        text = artist, 
                        fontSize = 14.sp, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis, 
                        color = Color.Black
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { mediaController?.transportControls?.skipToPrevious() }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.Black)
                }
                IconButton(onClick = {
                    if (isPlaying) {
                        mediaController?.transportControls?.pause()
                    } else {
                        mediaController?.transportControls?.play()
                    }
                }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black
                    )
                }
                IconButton(onClick = { mediaController?.transportControls?.skipToNext() }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.Black)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Black)
                }
            }
        }
    }
}
