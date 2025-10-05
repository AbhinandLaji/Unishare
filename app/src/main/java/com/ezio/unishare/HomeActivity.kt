package com.ezio.unishare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import com.ezio.unishare.ui.theme.PeerRentTheme
import kotlin.math.*

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the email passed from MainActivity
        val userEmail = intent.getStringExtra("USER_EMAIL") ?: "Welcome!"

        setContent {
            PeerRentTheme {
                UniShareAppScreen(userEmail = userEmail)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniShareAppScreen(userEmail: String) {
    val navController = rememberNavController()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute == Screen.Home.route || currentRoute == Screen.Rentals.route) {
                CustomTopBar(scrollBehavior, currentRoute, navController, userEmail = userEmail)
            }
        },
        bottomBar = { SmoothGooeyBottomNav(navController = navController) }
    ) { paddingValues ->
        AppNavHost(navController, Modifier.padding(paddingValues), userEmail = userEmail)
    }
}

// In HomeActivity.kt

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier, userEmail: String) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) { HomeScreenContent() }
        composable(Screen.Rentals.route) { RentalScreen() }
        
        // THE FIX: Pass both navController and userEmail to the ProfileScreen
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController, userEmail = userEmail)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopBar(scrollBehavior: TopAppBarScrollBehavior?, currentRoute: String?, navController: NavHostController, userEmail: String) {
    val title = when (currentRoute) {
        Screen.Home.route -> "Home"
        Screen.Rentals.route -> "Rentals"
        else -> ""
    }
    val subTitle = if (currentRoute == Screen.Home.route) userEmail else null
    val context = LocalContext.current

    TopAppBar(
        title = {
            Column {
                Text(title, fontSize = 20.sp, style = MaterialTheme.typography.titleLarge)
                subTitle?.let { Text(it, fontSize = 12.sp, color = Color.Gray, style = MaterialTheme.typography.bodySmall) }
            }
        },
        actions = {
            if (currentRoute == Screen.Home.route) {
                IconButton(onClick = {
                    val intent = Intent(context, ChatActivity::class.java)
                    intent.putExtra("CURRENT_USER_EMAIL", userEmail)
                    context.startActivity(intent)
                }) { Icon(Icons.Filled.Chat, contentDescription = "Messages") }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Profile",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun HomeScreenContent() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item { SearchBarSection() }
        item { BannerCarousel() }
        item { CategoryRow() }
        item { Text("Popular Rentals", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
        item { RentalItemGrid() }
        items(5) { index -> Text("More item $index", modifier = Modifier.padding(16.dp)) }
    }
}

@Composable
fun RentalScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("My Rentals", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Text("Currently Rented Items", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(3) { index ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text("Rented Item ${index + 1}", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Recommended for You", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(5) { index ->
                Card(modifier = Modifier.size(120.dp).padding(4.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("Item $index")
                    }
                }
            }
        }
    }
}

// The old ProfileScreen composable that was here is now DELETED.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarSection() {
    OutlinedTextField(
        value = "",
        onValueChange = { },
        placeholder = { Text("Search for rentals...") },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
        trailingIcon = { Icon(Icons.Filled.Mic, contentDescription = "Mic") }
    )
}

@Composable
fun BannerCarousel() {
    val banners = listOf("https://picsum.photos/600/200?1","https://picsum.photos/600/200?2","https://picsum.photos/600/200?3")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
        items(banners) { bannerUrl ->
            Card(modifier = Modifier.width(300.dp).height(150.dp), colors = CardDefaults.cardColors(containerColor = Color.LightGray)) {
                AsyncImage(model = bannerUrl, contentDescription = "Banner Image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
    }
}

@Composable
fun CategoryRow() {
    val categories = listOf("Books", "Electronics", "Furniture", "Sports", "Notes", "Other")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
        items(categories) { category -> AssistChip(onClick = { }, label = { Text(category) }) }
    }
}

@Composable
fun RentalItemGrid() {
    val itemsData = listOf("Camera" to "₹200/day", "Laptop" to "₹500/day","Bike" to "₹100/day", "Projector" to "₹300/day")
    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(280.dp).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(itemsData) { (name, price) ->
            Card {
                Column(modifier = Modifier.padding(8.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(model = "https://picsum.photos/200?random=$name", contentDescription = name,
                        modifier = Modifier.height(100.dp).fillMaxWidth(), contentScale = ContentScale.Crop)
                    Text(name, style = MaterialTheme.typography.titleSmall)
                    Text(price, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// Navigation Item and Gooey Bottom Nav code remain the same
data class NavigationItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun SmoothParticleEffect(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    repeat(6) { i ->
        val angle = (360f / 6f) * i
        val delay = i * 50
        val distance by infiniteTransition.animateFloat(initialValue = 0f, targetValue = if (isActive) 30f else 0f,
            animationSpec = infiniteRepeatable(animation = tween(1500, delayMillis = delay, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "distance$i")
        val alpha by infiniteTransition.animateFloat(initialValue = if (isActive) 0.8f else 0f, targetValue = if (isActive) 0f else 0f,
            animationSpec = infiniteRepeatable(animation = tween(1500, delayMillis = delay, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "alpha$i")
        if (isActive) {
            Box(modifier = Modifier.offset(x = (cos(angle * PI / 180f) * distance).dp, y = (sin(angle * PI / 180f) * distance).dp).size(6.dp).background(Color(0xFF0000FF).copy(alpha = alpha), CircleShape))
        }
    }
}

@Composable
fun SmoothGooeyBottomNav(navController: NavHostController) {
    val haptic = LocalHapticFeedback.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var selectedIndex by remember { mutableIntStateOf(0) }
    val animatedOffset by animateFloatAsState(targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow), label = "gooeyOffset")
    val navigationItems = listOf(
        NavigationItem(Screen.Home.route, Screen.Home.title, Screen.Home.icon),
        NavigationItem(Screen.Rentals.route, Screen.Rentals.title, Screen.Rentals.icon),
        NavigationItem(Screen.Profile.route, Screen.Profile.title, Screen.Profile.icon)
    )
    LaunchedEffect(currentRoute) {
        val newIndex = navigationItems.indexOfFirst { it.route == currentRoute }
        if (newIndex != -1) selectedIndex = newIndex
    }
    Box(modifier = Modifier.fillMaxWidth().height(85.dp).background(Brush.verticalGradient(colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val itemWidth = size.width / navigationItems.size
            val blobCenterX = itemWidth * (animatedOffset + 0.5f)
            val blobCenterY = size.height / 2f
            drawPillShape(Offset(blobCenterX, blobCenterY - 8.dp.toPx()), 80.dp.toPx(), 50.dp.toPx(), Color(0xFF1E90FF))
        }
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            navigationItems.forEachIndexed { index, navItem ->
                val isSelected = selectedIndex == index
                val scale by animateFloatAsState(targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "iconScale$index")
                val iconColor by animateColorAsState(targetValue = if (isSelected) Color.White else Color.Gray, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "iconColor$index")
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f).clickable {
                        if (selectedIndex != index) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            navController.navigate(navItem.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }.scale(scale).padding(vertical = 8.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
                        Icon(navItem.icon, contentDescription = navItem.label, tint = if (isSelected) Color(0xFF0000FF) else Color.Gray, modifier = Modifier.size(if (isSelected) 26.dp else 22.dp))
                        SmoothParticleEffect(isSelected)
                    }
                    AnimatedVisibility(visible = isSelected, enter = fadeIn(tween(300)) + slideInVertically(tween(300), initialOffsetY = { it / 2 }),
                        exit = fadeOut(tween(200)) + slideOutVertically(tween(200), targetOffsetY = { it / 2 })) {
                        Text(text = navItem.label, fontSize = 11.sp, color = if (isSelected) Color(0xFF0000FF) else Color.Gray, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}

fun DrawScope.drawPillShape(center: Offset, width: Float, height: Float, color: Color) {
    drawRoundRect(brush = Brush.radialGradient(colors = listOf(color.copy(alpha = 0.9f), color.copy(alpha = 0.7f)), center = center, radius = width / 2f),
        topLeft = Offset(center.x - width / 2f, center.y - height / 2f), size = Size(width, height), cornerRadius = CornerRadius(height / 2f, height / 2f))
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    PeerRentTheme { UniShareAppScreen("user@example.com") }
}