package com.ssccscanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssccscanner.ui.library.LibraryScreen
import com.ssccscanner.ui.scan.ScanScreen
import com.ssccscanner.ui.theme.PlexSans
import com.ssccscanner.ui.theme.Tokens

enum class AppTab { Scan, Library, Damage }

/**
 * Single persistent app shell — three tabs, no routing, matching the handoff's
 * state model.
 */
@Composable
fun AppRoot() {
    var activeTab by rememberSaveable { mutableStateOf(AppTab.Scan) }
    val toast = remember { ToastController() }
    val documentsViewModel: DocumentsViewModel = viewModel()
    val damageViewModel: com.ssccscanner.ui.damage.DamageViewModel = viewModel()
    val totalScans by documentsViewModel.totalScanCount.collectAsState()
    val damageCount by damageViewModel.count.collectAsState()

    // "Report damage" from the Scan/Library tabs jumps straight to the report.
    val damageOpenRequest by damageViewModel.openRequest.collectAsState()
    LaunchedEffect(damageOpenRequest) {
        if (damageOpenRequest != null) activeTab = AppTab.Damage
    }

    CompositionLocalProvider(LocalToast provides toast) {
        Box(modifier = Modifier.fillMaxSize().background(Tokens.Surface)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (activeTab) {
                        AppTab.Scan -> ScanScreen(
                            documentsViewModel = documentsViewModel,
                            onReportDamage = damageViewModel::reportDamage,
                        )
                        AppTab.Library -> LibraryScreen(
                            viewModel = documentsViewModel,
                            onReportDamage = damageViewModel::reportDamage,
                        )
                        AppTab.Damage -> com.ssccscanner.ui.damage.DamageScreen(viewModel = damageViewModel)
                    }
                }
                BottomNav(
                    activeTab = activeTab,
                    onSelect = { activeTab = it },
                    libraryBadgeCount = totalScans,
                    damageBadgeCount = damageCount,
                )
            }
            ToastHost(toast)
        }
    }
}

@Composable
fun BottomNav(
    activeTab: AppTab,
    onSelect: (AppTab) -> Unit,
    libraryBadgeCount: Int,
    damageBadgeCount: Int = 0,
) {
    Column(modifier = Modifier.fillMaxWidth().background(Tokens.Panel).navigationBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Tokens.ink(0.12f)))
        Row(modifier = Modifier.fillMaxWidth()) {
            NavItem(
                label = "Scan",
                icon = AppIcons.Camera,
                selected = activeTab == AppTab.Scan,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(AppTab.Scan) },
            )
            NavItem(
                label = "Library",
                icon = AppIcons.Document,
                selected = activeTab == AppTab.Library,
                modifier = Modifier.weight(1f),
                badgeCount = libraryBadgeCount,
                onClick = { onSelect(AppTab.Library) },
            )
            NavItem(
                label = "Damage",
                icon = AppIcons.Alert,
                selected = activeTab == AppTab.Damage,
                modifier = Modifier.weight(1f),
                badgeCount = damageBadgeCount,
                onClick = { onSelect(AppTab.Damage) },
            )
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    onClick: () -> Unit,
) {
    val tint = if (selected) Tokens.Accent else Tokens.ink(0.4f)
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(start = 14.dp)
                        .background(Tokens.Accent, shape = RoundedCornerShape(100.dp))
                        .padding(horizontal = 4.dp),
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        color = Tokens.OnAccent,
                        fontSize = 8.sp,
                        fontFamily = PlexSans,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Text(
            text = label,
            color = tint,
            fontFamily = PlexSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.5.sp,
        )
    }
}
