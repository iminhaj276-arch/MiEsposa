package com.miesposa.sadia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miesposa.sadia.core.permissions.SadiaPermission
import com.miesposa.sadia.ui.theme.*

data class PermissionRow(
    val permission: SadiaPermission,
    val isGranted: Boolean,
    val whyNeeded: String
)

@Composable
fun PermissionCenterScreen(
    rows: List<PermissionRow>,
    onAllowTapped: (SadiaPermission) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = SadiaBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SadiaSurface)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "ফিরে যাও", tint = SadiaTextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Sadia Permission Center", color = SadiaTextPrimary, fontSize = 18.sp)
                    Text("প্রতিটা permission নিজে থেকে Allow করো", color = SadiaTextSecondary, fontSize = 12.sp)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(rows) { row -> PermissionCard(row, onAllowTapped) }
        }
    }
}

@Composable
private fun PermissionCard(row: PermissionRow, onAllowTapped: (SadiaPermission) -> Unit) {
    Surface(
        color = SadiaCard,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (row.isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (row.isGranted) Color(0xFF4CAF50) else SadiaGlow
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(row.permission.label, color = SadiaTextPrimary, fontSize = 15.sp)
                Text(row.whyNeeded, color = SadiaTextSecondary, fontSize = 12.sp)
            }
            if (!row.isGranted) {
                Button(
                    onClick = { onAllowTapped(row.permission) },
                    colors = ButtonDefaults.buttonColors(containerColor = SadiaPurple)
                ) {
                    Text("Allow", fontSize = 12.sp)
                }
            } else {
                Text("চালু ✓", color = Color(0xFF4CAF50), fontSize = 12.sp)
            }
        }
    }
}
