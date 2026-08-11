package com.tupaz.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tupaz.ui.theme.VercelBackground
import com.tupaz.ui.theme.VercelBorder
import com.tupaz.ui.theme.VercelBorderHighlight
import com.tupaz.ui.theme.VercelCardSurface
import com.tupaz.ui.theme.VercelSurface
import com.tupaz.ui.theme.VercelTextMuted
import com.tupaz.ui.theme.VercelTextPrimary
import com.tupaz.ui.theme.VercelTextSecondary

private val ProfileCardShape = RoundedCornerShape(16.dp)
private val InputFieldShape = RoundedCornerShape(8.dp)
private val ButtonShape = RoundedCornerShape(8.dp)

@Composable
fun ProfileSetupScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileSetupViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val onNameChange = remember(viewModel) { { input: String -> viewModel.onNameChanged(input) } }
    val onAgeChange = remember(viewModel) { { input: String -> viewModel.onAgeChanged(input) } }
    val onSubmit = remember(viewModel, onContinue) { { viewModel.submitProfile(onContinue) } }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = VercelTextPrimary,
        unfocusedBorderColor = VercelBorder,
        focusedContainerColor = VercelCardSurface,
        unfocusedContainerColor = VercelCardSurface,
        cursorColor = VercelTextPrimary,
        focusedTextColor = VercelTextPrimary,
        unfocusedTextColor = VercelTextPrimary,
        errorBorderColor = Color.Red
    )

    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = Color.White,
        contentColor = Color.Black,
        disabledContainerColor = Color(0xFF222226),
        disabledContentColor = Color(0xFF666666)
    )

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
                    .border(1.dp, VercelBorderHighlight, ProfileCardShape),
                shape = ProfileCardShape,
                colors = CardDefaults.cardColors(containerColor = VercelSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Welcome to Tupaz",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = VercelTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    )

                    Text(
                        text = "Let's set up your profile.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = VercelTextSecondary,
                            fontSize = 14.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Name Input
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Name",
                            color = VercelTextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = onNameChange,
                            placeholder = { Text("Your name", color = VercelTextMuted, fontSize = 14.sp) },
                            singleLine = true,
                            isError = uiState.nameError != null,
                            colors = textFieldColors,
                            shape = InputFieldShape,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (uiState.nameError != null && uiState.name.isNotEmpty()) {
                            Text(
                                text = uiState.nameError!!,
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Age Input
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Age",
                            color = VercelTextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = uiState.age,
                            onValueChange = onAgeChange,
                            placeholder = { Text("Your age", color = VercelTextMuted, fontSize = 14.sp) },
                            singleLine = true,
                            isError = uiState.ageError != null,
                            colors = textFieldColors,
                            shape = InputFieldShape,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (uiState.isValid && !uiState.isSubmitting) {
                                        onSubmit()
                                    }
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (uiState.ageError != null && uiState.age.isNotEmpty()) {
                            Text(
                                text = uiState.ageError!!,
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Save Button
                    val isButtonEnabled = uiState.isValid && !uiState.isSubmitting
                    Button(
                        onClick = onSubmit,
                        enabled = isButtonEnabled,
                        shape = ButtonShape,
                        colors = buttonColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = "Save",
                                color = if (isButtonEnabled) Color.Black else Color(0xFF666666),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Text(
                        text = "Your profile is used to personalize your Tupaz experience.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = VercelTextMuted,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
