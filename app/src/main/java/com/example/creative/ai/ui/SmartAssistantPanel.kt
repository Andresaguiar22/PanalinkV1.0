package com.example.creative.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.creative.ai.animation.PostAnimationType
import com.example.creative.ai.animation.SmartAnimationEngine
import com.example.creative.ai.assistant.PanaCreativeBrain
import com.example.creative.core.CreativeLayer
import com.example.creative.post.PostPage
import com.example.creative.post.PostStudioProject
import com.example.creative.templates.CaptionDesignerEngine
import com.example.creative.templates.PostTemplate
import com.example.creative.templates.PostTemplateEngine
import com.example.creative.templates.TemplateVariable

/**
 * P6.6.5 - Smart Assistant Panel V2 ("✨ Asistente Pana AI Brain")
 * Multi-tab intelligent assistant for style recommendation, text generation, animation, and viral optimization score.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAssistantPanel(
    project: PostStudioProject,
    currentPage: PostPage,
    onApplyPageUpdate: (PostPage) -> Unit,
    onApplyTemplate: (PostTemplate, Map<TemplateVariable, String>) -> Unit,
    onUpdateCaption: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: IA Recomendado, 1: Plantillas, 2: Textos, 3: Animar, 4: Impacto Viral

    val brainResult = remember(project, currentPage) {
        PanaCreativeBrain.generateCreativeBrainSuggestions(project, currentPage)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with Viral Score badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Asistente Pana AI",
                        tint = Color(0xFFFF007A),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Asistente Pana AI Brain ✨",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Score Viral: ${brainResult.viralReport.totalScore}/100 • Calidad: ${brainResult.qualityReport.overallScorePercent}%",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFF38BDF8),
                edgePadding = 0.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("⚡ IA Recomendado", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🎨 Plantillas", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("✍ Captions & Textos", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("🎬 Animar", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("🚀 Score Viral", fontSize = 12.sp) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> { // IA Brain Summary & Best Match Recommendation
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Plantilla Recomendada",
                                        color = Color(0xFFFF007A),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = brainResult.recommendedTemplate.name,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = brainResult.recommendedTemplate.description,
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            onApplyTemplate(brainResult.recommendedTemplate, emptyMap())
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Aplicar Estilo Completo", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        item {
                            Text("Sugerencias de Composición:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        items(brainResult.layoutSuggestions) { suggestion ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = suggestion.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(text = suggestion.description, color = Color.Gray, fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = {
                                            val updated = suggestion.applyAction(currentPage)
                                            onApplyPageUpdate(updated)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Text("Optimizar", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> { // Templates Catalog
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PostTemplateEngine.availableTemplates) { template ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onApplyTemplate(template, emptyMap())
                                        onDismiss()
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = template.name,
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "${template.category.displayName} • ${template.description}",
                                        color = Color.LightGray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> { // Captions Generator & Typography Presets
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Text("Captions Sugeridos por IA:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        items(brainResult.suggestedCaptions) { genCaption ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = genCaption.tone.displayName,
                                            color = Color(0xFFFF007A),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Usar Caption",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable {
                                                onUpdateCaption(genCaption.text)
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = genCaption.text, color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Estilos de Tipografía Visual:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(CaptionDesignerEngine.captionPresets) { preset ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .width(130.dp)
                                            .clickable {
                                                val firstText = currentPage.layers.filterIsInstance<CreativeLayer.Text>().firstOrNull()
                                                if (firstText != null) {
                                                    val styledText = CaptionDesignerEngine.applyPresetToTextLayer(firstText, preset)
                                                    val updatedLayers = currentPage.layers.map { if (it.id == firstText.id) styledText else it }
                                                    onApplyPageUpdate(currentPage.copy(layers = updatedLayers))
                                                }
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(text = preset.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "TEXTO",
                                                color = Color(android.graphics.Color.parseColor(preset.colorHex)),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> { // Smart Animations
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PostAnimationType.entries) { animType ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val animatedPage = SmartAnimationEngine.applyPresetToPage(currentPage, animType)
                                        onApplyPageUpdate(animatedPage)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = animType.name.replace("_", " "), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(text = "Efecto de movimiento dinámico para esta página", color = Color.Gray, fontSize = 11.sp)
                                    }
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF38BDF8))
                                }
                            }
                        }
                    }
                }

                4 -> { // Viral Score & Quality
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Impacto Viral Estimado: ${brainResult.viralReport.totalScore} / 100",
                                        color = Color(0xFF10B981),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = "Enganche Título: ${brainResult.viralReport.hookScore}/100", color = Color.Gray, fontSize = 12.sp)
                                    Text(text = "Atractivo Visual: ${brainResult.viralReport.visualAppealScore}/100", color = Color.Gray, fontSize = 12.sp)
                                    Text(text = "Engagement Potencial: ${brainResult.viralReport.engagementScore}/100", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }

                        item {
                            Text("Recomendaciones para Viralizar:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        items(brainResult.viralReport.recommendations) { rec ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = rec, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
