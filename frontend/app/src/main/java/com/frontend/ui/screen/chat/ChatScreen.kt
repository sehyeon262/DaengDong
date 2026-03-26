package com.frontend.ui.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frontend.ui.theme.PointGreen

private val ChatBg        = Color(0xFFF4FBF0)   // 연한 민트그린 배경
private val MyBubble      = PointGreen           // 내 말풍선
private val OtherBubble   = Color.White          // 상대 말풍선
private val InputBarBg    = Color.White
private val TopBarBg      = Color.White

@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    // 새 메시지가 추가되면 맨 아래로 스크롤
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),   // Scaffold이 inset 관리 안 함
        topBar = {
            Surface(
                color = TopBarBg,
                shadowElevation = 2.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // status bar 높이만큼 투명 여백 — Surface 배경으로 채워짐
                    Spacer(modifier = Modifier.fillMaxWidth().statusBarsPadding())
                    // 실제 툴바 (status bar와 독립적으로 52dp 확보)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "뒤로"
                            )
                        }
                        Text("🐾 ")
                        Text(
                            text = "산책 채팅",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                        )
                    }
                }
            }
        },
        containerColor = ChatBg,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .imePadding(),   // IME = 키보드+navBar 전체 높이 → 키보드 바로 위에 위치
        ) {
            // ── 채팅방 CLOSED 배너 ───────────────────────────────────────────
            if (state.isClosed) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFEBEE))
                        .padding(vertical = 3.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "🐕 산책이 종료되어 채팅방이 닫혔습니다.",
                        fontSize = 13.sp,
                        color = Color(0xFFC62828),
                    )
                }
            }

            // ── 메시지 목록 ──────────────────────────────────────────────────
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = PointGreen)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        horizontal = 14.dp,
                        vertical = 14.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = state.messages,
                        key = { it.messageId },
                    ) { message ->
                        val isMe = message.senderId == state.myMemberId
                        MessageBubble(
                            content = message.content,
                            sentAt = message.sentAt,
                            isMe = isMe,
                            maxWidth = screenWidth * 0.72f,
                        )
                    }
                }
            }

            // ── 입력창 ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(InputBarBg)
                    .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.inputText,
                    onValueChange = viewModel::onInputChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = if (state.isClosed) "채팅방이 종료됐습니다" else "메시지를 입력하세요 🐾",
                            fontSize = 14.sp,
                            color = Color(0xFFBDBDBD),
                        )
                    },
                    enabled = !state.isClosed,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PointGreen,
                        unfocusedBorderColor = Color(0xFFDCEDD6),
                        focusedContainerColor = Color(0xFFF8FFF5),
                        unfocusedContainerColor = Color(0xFFF8FFF5),
                    ),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { viewModel.sendMessage() }),
                    singleLine = false,
                )

                Spacer(modifier = Modifier.size(8.dp))

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (state.inputText.isNotBlank() && !state.isClosed)
                                PointGreen else Color(0xFFE0E0E0)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(
                        onClick = viewModel::sendMessage,
                        enabled = state.inputText.isNotBlank() && !state.isClosed,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "전송",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    content: String,
    sentAt: String,
    isMe: Boolean,
    maxWidth: androidx.compose.ui.unit.Dp,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (isMe) {
            // 시간 — 말풍선 왼쪽
            if (sentAt.isNotEmpty()) {
                Text(
                    text = formatSentAt(sentAt),
                    fontSize = 11.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier
                        .padding(end = 6.dp, bottom = 4.dp)
                        .align(Alignment.Bottom),
                )
            }
            MyBubbleBox(content = content, maxWidth = maxWidth)
        } else {
            OtherBubbleBox(content = content, maxWidth = maxWidth)
            // 시간 — 말풍선 오른쪽
            if (sentAt.isNotEmpty()) {
                Text(
                    text = formatSentAt(sentAt),
                    fontSize = 11.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier
                        .padding(start = 6.dp, bottom = 4.dp)
                        .align(Alignment.Bottom),
                )
            }
        }
    }
}

@Composable
private fun MyBubbleBox(content: String, maxWidth: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .widthIn(max = maxWidth)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(MyBubble)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = content,
            color = Color.White,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
    }
}

@Composable
private fun OtherBubbleBox(content: String, maxWidth: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .widthIn(max = maxWidth)
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(OtherBubble)
            .shadow(1.dp, RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = content,
            color = Color(0xFF333333),
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
    }
}

/** "2024-01-15T11:28:00" → "11:28" */
private fun formatSentAt(sentAt: String): String {
    return try {
        val timePart = sentAt.substringAfter("T").substringBefore(".")
        val parts = timePart.split(":")
        "${parts[0]}:${parts[1]}"
    } catch (_: Exception) {
        sentAt
    }
}
