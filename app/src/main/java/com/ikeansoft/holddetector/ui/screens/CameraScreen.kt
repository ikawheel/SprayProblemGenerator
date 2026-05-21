package com.ikeansoft.holddetector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ikeansoft.holddetector.R
import com.ikeansoft.holddetector.ui.AppSubtleSurfaceColor
import com.ikeansoft.holddetector.ui.AppTextColor
import com.ikeansoft.holddetector.ui.components.AppButton

// 壁画像の取得方法を選ぶ画面です。
@Composable
fun CameraFullscreenScreen(
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 画面全体は通常の入力画面として構成します。
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // 主コンテンツは中央寄せにして、選択肢をわかりやすく並べます。
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            // タイトルを表示します。
            Text(
                text = stringResource(R.string.camera_title),
                color = AppTextColor,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // 取得方法をまとめたパネルです。
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .background(AppSubtleSurfaceColor, RoundedCornerShape(20.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 標準カメラ起動ボタンです。
                AppButton(
                    onClick = onTakePhoto,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.camera_take_photo))
                }

                // 端末内の画像を選ぶボタンです。
                AppButton(
                    onClick = onPickPhoto,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.camera_pick_photo))
                }
            }

            // 下側に少し余白を入れて窮屈さを減らします。
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
