package com.frontend.ui.screen.walk.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.frontend.ui.screen.walk.WalkFilterType
import com.frontend.ui.theme.Dimens
import com.frontend.ui.theme.IconGray
import com.frontend.ui.theme.PointGreen
import com.frontend.ui.theme.TextMain
import com.frontend.ui.theme.White

private val ScrimColor = Color.Black.copy(alpha = 0.4f)

@Composable
fun WalkFilterBottomSheet(
    activeFilters: Set<WalkFilterType>,
    onFilterToggle: (WalkFilterType) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // 스크림 (반투명 배경) — 탭하면 닫힘
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScrimColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        // 바텀시트 패널 — 터치가 스크림으로 통과하지 않도록 차단
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    color = White,
                    shape = RoundedCornerShape(
                        topStart = Dimens.RadiusLarge,
                        topEnd = Dimens.RadiusLarge
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingLarge)
                .navigationBarsPadding()
        ) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "필터 설정",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = Dimens.TextSizeLarge,
                        color = TextMain
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "닫기",
                        tint = TextMain
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingXLarge))

            // 필터 옵션 3개 (각 항목 독립 토글)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
            ) {
                FilterOption(
                    icon = Icons.Filled.LocationOn,
                    label = "장소",
                    isSelected = WalkFilterType.PLACE in activeFilters,
                    onClick = { onFilterToggle(WalkFilterType.PLACE) },
                    modifier = Modifier.weight(1f)
                )
                FilterOption(
                    icon = Icons.Filled.Pets,
                    label = "주변 강아지",
                    isSelected = WalkFilterType.NEARBY_DOG in activeFilters,
                    onClick = { onFilterToggle(WalkFilterType.NEARBY_DOG) },
                    modifier = Modifier.weight(1f)
                )
                FilterOption(
                    icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                    label = "발자국",
                    isSelected = WalkFilterType.FOOTPRINT in activeFilters,
                    onClick = { onFilterToggle(WalkFilterType.FOOTPRINT) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingXLarge))

            // 적용하기 버튼
            Button(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.ButtonHeight),
                shape = RoundedCornerShape(Dimens.RadiusMedium),
                colors = ButtonDefaults.buttonColors(containerColor = PointGreen)
            ) {
                Text(
                    text = "적용하기",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = Dimens.TextSizeMedium,
                        color = White
                    )
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingXSmall))
        }
    }
}

@Composable
private fun FilterOption(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(
                width = Dimens.SpacingXSmall / 4,
                color = if (isSelected) PointGreen else Color.Transparent,
                shape = RoundedCornerShape(Dimens.RadiusMedium)
            )
            .background(White, RoundedCornerShape(Dimens.RadiusMedium))
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.SpacingMedium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.FilterIconBoxSize)
                .background(
                    color = if (isSelected) PointGreen else IconGray.copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) White else IconGray,
                modifier = Modifier.size(Dimens.FilterIconSize)
            )
        }
        Spacer(modifier = Modifier.height(Dimens.SpacingXSmall))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = Dimens.TextSizeSmall,
                color = if (isSelected) PointGreen else TextMain
            )
        )
    }
}
