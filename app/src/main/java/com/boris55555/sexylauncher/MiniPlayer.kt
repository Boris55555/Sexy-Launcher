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
import androidx.compose.foundation.layout.size
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
            .padding(horizontal = 8.dp, vertical = 4.dp) // Smaller padding
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(BorderStroke(2.dp, Color.Black), shape = RoundedCornerShape(8.dp))
            .clickable {
                try {
                    mediaController?.sessionActivity?.send()
                } catch (e: Exception) {}
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title, 
                    fontWeight = FontWeight.Bold, 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis, 
                    color = Color.Black,
                    fontSize = 14.sp // Smaller font
                )
                if (artist.isNotBlank()) {
                    Text(
                        text = artist, 
                        fontSize = 11.sp, // Smaller font
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis, 
                        color = Color.Black
                    )
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { mediaController?.transportControls?.skipToPrevious() },
                    modifier = Modifier.size(32.dp) // Smaller buttons
                ) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.Black, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            mediaController?.transportControls?.pause()
                        } else {
                            mediaController?.transportControls?.play()
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(
                    onClick = { mediaController?.transportControls?.skipToNext() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.Black, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Black, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
