package com.tupaz.ui.gating

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tupaz.R
import com.tupaz.data.firebase.AppConfigState
import com.tupaz.ui.theme.VercelBackground
import com.tupaz.ui.theme.VercelBorder
import com.tupaz.ui.theme.VercelBorderHighlight
import com.tupaz.ui.theme.VercelCardSurface
import com.tupaz.ui.theme.VercelSurface
import com.tupaz.ui.theme.VercelTextMuted
import com.tupaz.ui.theme.VercelTextPrimary
import com.tupaz.ui.theme.VercelTextSecondary

@Composable
fun StartupGatingScreen(
    state: AppConfigState,
    onContinueToApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        containerColor = VercelBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, VercelBorderHighlight, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = VercelSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Premium Logo
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(VercelCardSurface)
                            .border(1.dp, VercelBorderHighlight, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Tupaz Logo",
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    when (state) {
                        is AppConfigState.Loading -> {
                            Text(
                                text = "Initializing Tupaz Engine",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = VercelTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            CircularProgressIndicator(
                                color = VercelTextPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        is AppConfigState.BetaClosed -> {
                            StatusBadge(label = "Private Beta Closed", badgeColor = Color(0xFFFF453A))

                            Text(
                                text = "TUPAZ is currently closed",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    color = VercelTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = VercelTextSecondary,
                                    fontSize = 14.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                        }

                        is AppConfigState.Maintenance -> {
                            StatusBadge(label = "Scheduled Maintenance", badgeColor = Color(0xFFFF9F0A))

                            Text(
                                text = "TUPAZ is under maintenance",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    color = VercelTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = VercelTextSecondary,
                                    fontSize = 14.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                        }

                        is AppConfigState.UpdateRequired -> {
                            StatusBadge(label = "Mandatory Update v${state.latestVersion}", badgeColor = Color(0xFF0A84FF))

                            Text(
                                text = "Update Required",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    color = VercelTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = VercelTextSecondary,
                                    fontSize = 14.sp
                                ),
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick = { launchUpdateDestination(context, state.updateUrl) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Update Now", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        is AppConfigState.UpdateAvailable -> {
                            StatusBadge(label = "New Version v${state.latestVersion}", badgeColor = Color(0xFF30D158))

                            Text(
                                text = "Update Available",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    color = VercelTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = VercelTextSecondary,
                                    fontSize = 14.sp
                                ),
                                textAlign = TextAlign.Center
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { launchUpdateDestination(context, state.updateUrl) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text("Update Now", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }

                                OutlinedButton(
                                    onClick = onContinueToApp,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = VercelTextSecondary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                ) {
                                    Text("Continue to Tupaz", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        is AppConfigState.Ready -> {
                            // Automatically continues
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String, badgeColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(VercelCardSurface)
            .border(1.dp, VercelBorder, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(badgeColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = VercelTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun launchUpdateDestination(context: Context, updateUrl: String) {
    val trimmed = updateUrl.trim()
    if (trimmed.isEmpty()) {
        android.util.Log.e("[Tupaz-Firebase]", "updateUrl is empty")
        Toast.makeText(context, "Update link is currently unavailable.", Toast.LENGTH_SHORT).show()
        return
    }

    val uri = try { Uri.parse(trimmed) } catch (_: Throwable) { null }
    val scheme = uri?.scheme?.lowercase()
    if (uri == null || (scheme != "http" && scheme != "https" && scheme != "market")) {
        android.util.Log.e("[Tupaz-Firebase]", "Invalid updateUrl scheme: '$updateUrl'")
        Toast.makeText(context, "Invalid update link configured.", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Throwable) {
        android.util.Log.e("[Tupaz-Firebase]", "Failed to launch update URL: ${e.message}", e)
        Toast.makeText(context, "Unable to open update link.", Toast.LENGTH_SHORT).show()
    }
}
