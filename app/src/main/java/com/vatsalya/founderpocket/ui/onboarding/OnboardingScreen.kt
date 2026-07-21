package com.vatsalya.founderpocket.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

private sealed class PageArt {
    data class VectorIcon(val icon: ImageVector) : PageArt()
    object LogoSplash : PageArt()
}

private data class OnboardingPage(
    val art: PageArt,
    val title: String,
    val body: String
)

private val PAGES = listOf(
    OnboardingPage(
        art   = PageArt.LogoSplash,
        title = "Founder Pocket",
        body  = "Your offline-first notebook. Every note, meeting, and document stays encrypted on your device — nothing ever leaves."
    ),
    OnboardingPage(
        art   = PageArt.VectorIcon(Icons.Default.GridView),
        title = "12 capture types",
        body  = "Notes, voice, meetings, ideas, tasks, follow-ups, contacts, expenses, wins, links, docs, and more — under 60 seconds each."
    ),
    OnboardingPage(
        art   = PageArt.VectorIcon(Icons.Default.Psychology),
        title = "Semantic recall + AI",
        body  = "Find any capture by meaning, not just keywords. On-device AI surfaces what needs your attention today."
    )
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pagerState = rememberPagerState { PAGES.size }
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == PAGES.lastIndex

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { index ->
                OnboardingPageContent(PAGES[index])
            }

            // Dot indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                repeat(PAGES.size) { index ->
                    val selected = index == pagerState.currentPage
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(width = if (selected) 24.dp else 8.dp, height = 8.dp)
                    ) {}
                }
            }

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onComplete) { Text("Skip") }

                Button(
                    onClick = {
                        if (isLast) onComplete()
                        else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                ) {
                    Text(if (isLast) "Get started" else "Next")
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (page.art) {
            is PageArt.LogoSplash -> {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .background(Color.Black, MaterialTheme.shapes.extraLarge),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = "file:///android_asset/logo.png",
                        contentDescription = "Founder Pocket logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            is PageArt.VectorIcon -> {
                Icon(
                    imageVector = page.art.icon,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
