package com.frontend.ui.screen.walk.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frontend.domain.model.DangerReason
import com.frontend.ui.theme.PointGreen

private const val MAX_CUSTOM_REASON_LENGTH = 50

@Composable
fun DangerReportModal(
    selectedReason: DangerReason?,
    customReason: String,
    isLoading: Boolean,
    error: String? = null,
    onReasonSelect: (DangerReason) -> Unit,
    onCustomReasonChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    // 반투명 딤 배경
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clickable(enabled = false, onClick = {}),  // 카드 클릭 시 딤 닫힘 방지
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // 제목
                Text(
                    text = "위험 구역 신고",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = "위험 사유를 선택해주세요",
                    fontSize = 13.sp,
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 사유 선택 그리드 (2열)
                val pairedReasons = listOf(
                    DangerReason.FOOT_TRAP to DangerReason.HAZARDOUS_MATERIAL,
                    DangerReason.SNAKE to DangerReason.TICK
                )

                pairedReasons.forEach { (left, right) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReasonChip(
                            reason = left,
                            isSelected = selectedReason == left,
                            onClick = { onReasonSelect(left) },
                            modifier = Modifier.weight(1f)
                        )
                        ReasonChip(
                            reason = right,
                            isSelected = selectedReason == right,
                            onClick = { onReasonSelect(right) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // "기타" 단독 행
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReasonChip(
                        reason = DangerReason.OTHER,
                        isSelected = selectedReason == DangerReason.OTHER,
                        onClick = { onReasonSelect(DangerReason.OTHER) },
                        modifier = Modifier.weight(1f)
                    )

                    // "기타" 선택 시 직접 입력 필드 (최대 50자)
                    OutlinedTextField(
                        value = customReason,
                        onValueChange = { input ->
                            if (input.length <= MAX_CUSTOM_REASON_LENGTH) {
                                onCustomReasonChange(input)
                            }
                        },
                        enabled = selectedReason == DangerReason.OTHER,
                        placeholder = {
                            Text(
                                text = "직접 입력",
                                fontSize = 13.sp,
                                color = Color(0xFFBBBBBB)
                            )
                        },
                        supportingText = {
                            if (selectedReason == DangerReason.OTHER) {
                                Text(
                                    text = "${customReason.length}/$MAX_CUSTOM_REASON_LENGTH",
                                    fontSize = 11.sp,
                                    color = if (customReason.length >= MAX_CUSTOM_REASON_LENGTH)
                                        Color(0xFFE53935) else Color(0xFFAAAAAA)
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = false,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 에러 메시지
                if (error != null) {
                    Text(
                        text = error,
                        fontSize = 12.sp,
                        color = Color(0xFFE53935),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 제출 버튼
                val canSubmit = selectedReason != null &&
                        (selectedReason != DangerReason.OTHER || customReason.isNotBlank()) &&
                        !isLoading

                Button(
                    onClick = onSubmit,
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PointGreen,
                        disabledContainerColor = Color(0xFFCCCCCC)
                    )
                ) {
                    Text(
                        text = if (isLoading) "제출 중..." else "제출하기",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ReasonChip(
    reason: DangerReason,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) PointGreen.copy(alpha = 0.1f) else Color(0xFFF5F5F5))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) PointGreen else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(10.dp)
            )
            .semantics {
                role = Role.Button
                contentDescription = "${reason.label} 선택${if (isSelected) " (선택됨)" else ""}"
            }
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // 선택 인디케이터 원
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) PointGreen else Color(0xFFCCCCCC))
            )
            Text(
                text = reason.label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) PointGreen else Color(0xFF555555),
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}
