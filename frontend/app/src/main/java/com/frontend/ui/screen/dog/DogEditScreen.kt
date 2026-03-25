package com.frontend.ui.screen.dog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.frontend.ui.theme.PointGreen
import com.frontend.ui.theme.TextGray
import com.frontend.ui.theme.TextMain

private val ALL_TRAITS = listOf(
    "에너지이저" to "\u26A1",
    "겁쟁이" to "\uD83D\uDE33",
    "사람 좋아" to "\uD83D\uDC36",
    "짖음 많음" to "\uD83E\uDDB4",
    "먹보" to "\uD83C\uDF57",
    "친구 좋아" to "\uD83E\uDD0D"
)

@Composable
fun DogEditScreen(
    navController: NavController,
    viewModel: DogEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) navController.popBackStack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF6EE))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = TextMain)
            }
            Text(
                text = "반려견 프로필 수정",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
        }

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PointGreen)
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item { PhotoSection() }
                    item { BasicInfoSection(state, viewModel) }
                    item { TraitsSection(state, viewModel) }
                    item {
                        state.error?.let {
                            Text(it, color = Color.Red, fontSize = 13.sp)
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                Button(
                    onClick = { viewModel.save() },
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PointGreen)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("저장하기", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(100.dp)) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9))
                    .border(2.dp, PointGreen.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Pets, null, tint = PointGreen, modifier = Modifier.size(40.dp))
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(30.dp)
                    .background(Color(0xFF5C5C5C), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.CameraAlt,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            text = "\uC0AC\uB791\uC2A4\uB7EC\uC6B4 \uBC18\uB824\uACAC\uC758 \uC0AC\uC9C4\uC744 \uB4F1\uB85D\uD574\uC8FC\uC138\uC694.",
            fontSize = 13.sp,
            color = TextGray
        )
    }
}

@Composable
private fun BasicInfoSection(state: DogEditState, viewModel: DogEditViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("\uAE30\uBCF8 \uC815\uBCF4", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextMain)

        EditField(label = "\uC774\uB984") {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = editFieldColors()
            )
        }

        EditField(label = "\uACAC\uC885") {
            OutlinedTextField(
                value = state.breed,
                onValueChange = viewModel::onBreedChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("\uACAC\uC885\uC744 \uAC80\uC0C9\uD558\uC138\uC694", color = TextGray) },
                shape = RoundedCornerShape(12.dp),
                colors = editFieldColors()
            )
        }

        EditField(label = "\uC0DD\uC77C") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BirthPartField(
                    value = state.birthYear,
                    onValueChange = viewModel::onBirthYearChange,
                    suffix = "\uB144",
                    modifier = Modifier.weight(2f),
                    maxLength = 4
                )
                BirthPartField(
                    value = state.birthMonth,
                    onValueChange = viewModel::onBirthMonthChange,
                    suffix = "\uC6D4",
                    modifier = Modifier.weight(1.5f),
                    maxLength = 2
                )
                BirthPartField(
                    value = state.birthDay,
                    onValueChange = viewModel::onBirthDayChange,
                    suffix = "\uC77C",
                    modifier = Modifier.weight(1.5f),
                    maxLength = 2
                )
            }
        }

        EditField(label = "\uC131\uBCC4") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
            ) {
                GenderButton(
                    label = "\uB0A8\uC544",
                    selected = state.gender == "MALE",
                    onClick = { viewModel.onGenderChange("MALE") },
                    modifier = Modifier.weight(1f)
                )
                GenderButton(
                    label = "\uC5EC\uC544",
                    selected = state.gender == "FEMALE",
                    onClick = { viewModel.onGenderChange("FEMALE") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        EditField(label = "\uD604\uC7AC\uCCB4\uC911") {
            OutlinedTextField(
                value = state.weight,
                onValueChange = viewModel::onWeightChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                suffix = { Text("kg", color = TextGray) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                colors = editFieldColors()
            )
        }
    }
}

@Composable
private fun TraitsSection(state: DogEditState, viewModel: DogEditViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("\uC131\uD5A5 \uD0DC\uADF8", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
            Text("\uB2E4\uC911 \uC120\uD0DD \uAC00\uB2A5", fontSize = 12.sp, color = TextGray)
        }
        ALL_TRAITS.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (trait, emoji) ->
                    val selected = state.selectedTraits.contains(trait)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (selected) PointGreen.copy(alpha = 0.15f) else Color.White
                            )
                            .border(
                                1.5.dp,
                                if (selected) PointGreen else Color(0xFFDDDDDD),
                                RoundedCornerShape(24.dp)
                            )
                            .clickable { viewModel.onTraitToggle(trait) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(emoji, fontSize = 20.sp)
                            Text(
                                text = trait,
                                fontSize = 12.sp,
                                color = if (selected) PointGreen else TextGray,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EditField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 14.sp, color = TextGray, fontWeight = FontWeight.Medium)
        content()
    }
}

@Composable
private fun BirthPartField(
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String,
    modifier: Modifier,
    maxLength: Int
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= maxLength && it.all { c -> c.isDigit() }) onValueChange(it) },
        modifier = modifier,
        singleLine = true,
        suffix = { Text(suffix, color = TextGray) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        colors = editFieldColors()
    )
}

@Composable
private fun GenderButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .background(if (selected) PointGreen else Color.White)
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else TextGray
        )
    }
}

@Composable
private fun editFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PointGreen,
    unfocusedBorderColor = Color(0xFFDDDDDD),
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)
