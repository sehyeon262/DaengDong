package com.frontend.ui.screen.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.frontend.ui.theme.*

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var keepLogin by remember { mutableStateOf(false) }

    // 로그인 성공 시 홈으로 이동
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // 타이틀
            Text(
                text = "반가워요!",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "댕동여지도와 함께 오늘의 산책을 시작해보세요",
                fontSize = 14.sp,
                color = TextBrown
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    // 이메일 입력
                    Text(text = "이메일", fontSize = 14.sp, color = TextMain, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    TextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("example@email.com", color = TextGray) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = IconGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = InputBackground,
                            focusedContainerColor = InputBackground,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 비밀번호 입력
                    Text(text = "비밀번호", fontSize = 14.sp, color = TextMain, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("••••••••", color = TextGray) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = IconGray) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = InputBackground,
                            focusedContainerColor = InputBackground,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 로그인 유지 / 비밀번호 찾기
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = keepLogin,
                                onCheckedChange = { keepLogin = it },
                                colors = CheckboxDefaults.colors(checkedColor = PointGreen)
                            )
                            Text(text = "로그인 유지", fontSize = 13.sp, color = TextGray)
                        }
                        TextButton(onClick = { /* 비밀번호 찾기 */ }) {
                            Text(text = "비밀번호 찾기", fontSize = 13.sp, color = PointGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 에러 메시지
                    state.error?.let {
                        Text(
                            text = it,
                            color = Color.Red,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // 로그인 버튼
                    Button(
                        onClick = { viewModel.login(email, password, keepLogin) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PointGreen),
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(text = "로그인", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 회원가입 안내
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "아직 계정이 없으신가요?  ", fontSize = 14.sp, color = TextGray)
                TextButton(onClick = { /* 회원가입 */ }, contentPadding = PaddingValues(0.dp)) {
                    Text(text = "회원가입", fontSize = 14.sp, color = PointGreen, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}