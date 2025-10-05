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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.ezio.unishare.ui.theme.PeerRentTheme
import kotlin.math.*
import androidx.compose.animation.AnimatedContentTransitionScope

// Data class for rental items
data class RentalItem(
    val id: String,
    val name: String,
    val price: String,
    val description: String,
    val category: String,
    val ownerName: String,
    val ownerEmail: String,
    val imageUrl: String,
    val rating: Float = 4.5f,
    val available: Boolean = true
)

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            // Hide bottom nav on detail page
            if (currentRoute != "item_detail/{itemId}" && currentRoute != "add_product") {
                SmoothGooeyBottomNav(navController = navController)
            }
        },
        floatingActionButton = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            // Show FAB only on Home screen
            if (currentRoute == Screen.Home.route) {
                FloatingActionButton(
                    onClick = { navController.navigate("add_product") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Product")
                }
            }
        }
    ) { paddingValues ->
        AppNavHost(navController, Modifier.padding(paddingValues), userEmail = userEmail)
    }
}

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier, userEmail: String) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreenContent(navController = navController)
        }
        composable(Screen.Rentals.route) { RentalScreen() }
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController, userEmail = userEmail)
        }
        composable(
            route = "item_detail/{itemId}",
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            ItemDetailScreen(navController = navController, itemId = itemId)
        }
        composable(
            route = "category/{categoryName}",
            arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
            CategoryListScreen(navController = navController, categoryName = categoryName)
        }
        composable("add_product") {
            AddProductScreen(navController = navController, userEmail = userEmail)
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

// Sample data - Replace with Firebase data later
fun getSampleRentalItems(): List<RentalItem> {
    return listOf(
        RentalItem("1", "Camera", "₹200/day", "Professional DSLR camera perfect for photography enthusiasts. Includes lens kit and tripod.", "Electronics", "John Doe", "john@university.edu", "https://picsum.photos/seed/camera/400"),
        RentalItem("2", "Laptop", "₹500/day", "High-performance laptop with 16GB RAM, ideal for coding and design work.", "Electronics", "Jane Smith", "jane@university.edu", "https://picsum.photos/seed/laptop/400"),
        RentalItem("3", "Bike", "₹100/day", "Mountain bike in excellent condition. Perfect for campus commute.", "Sports", "Mike Johnson", "mike@university.edu", "https://picsum.photos/seed/bike/400"),
        RentalItem("4", "Projector", "₹300/day", "HD projector suitable for presentations and movie nights.", "Electronics", "Sarah Wilson", "sarah@university.edu", "https://picsum.photos/seed/projector/400"),
        RentalItem("5", "Guitar", "₹150/day", "Acoustic guitar for beginners and intermediate players.", "Music", "Tom Brown", "tom@university.edu", "https://picsum.photos/seed/guitar/400"),
        RentalItem("6", "Textbook: Data Structures", "₹50/day", "CS textbook for semester courses. Like new condition.", "Books", "Emily Davis", "emily@university.edu", "https://picsum.photos/seed/book/400"),
    )
}

@Composable
fun HomeScreenContent(navController: NavHostController) {
    val rentalItems = remember { getSampleRentalItems() }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item { SearchBarSection() }
        item { BannerCarousel() }
        item { CategoryRow(navController = navController) }
        item {
            Text(
                "Popular Rentals",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        item {
            RentalItemGrid(
                items = rentalItems,
                onItemClick = { item ->
                    navController.navigate("item_detail/${item.id}")
                }
            )
        }
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
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        "Rented Item ${index + 1}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
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
    val banners = listOf(R.drawable.banner_1, R.drawable.banner_2, R.drawable.banner_3)
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(banners) { bannerUrl ->
            Card(
                modifier = Modifier.width(300.dp).height(150.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                AsyncImage(
                    model = bannerUrl,
                    contentDescription = "Banner Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun CategoryRow(navController: NavHostController) {
    val categories = listOf("Books", "Electronics", "Furniture", "Sports", "Notes", "Other")
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(categories) { category ->
            AssistChip(
                onClick = {
                    navController.navigate("category/$category")
                },
                label = { Text(category) }
            )
        }
    }
}

@Composable
fun RentalItemGrid(items: List<RentalItem>, onItemClick: (RentalItem) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.height((items.size / 2 * 160 + 40).dp).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            var isPressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.95f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "cardScale"
            )

            Card(
                modifier = Modifier
                    .scale(scale)
                    .clickable {
                        isPressed = true
                        onItemClick(item)
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        modifier = Modifier.height(100.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1
                    )
                    Text(
                        item.price,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(navController: NavHostController, itemId: String) {
    val item = remember { getSampleRentalItems().find { it.id == itemId } }

    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Item not found")
        }
        return
    }

    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                // Product Image with scale animation
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(400)) +
                            scaleIn(initialScale = 0.8f, animationSpec = tween(400, easing = FastOutSlowInEasing))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Item Name and Price with slide animation
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(400, delayMillis = 100)) +
                            slideInVertically(
                                initialOffsetY = { it / 2 },
                                animationSpec = tween(400, delayMillis = 100, easing = FastOutSlowInEasing)
                            )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.price,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Rating and Category with slide animation
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(400, delayMillis = 200)) +
                            slideInVertically(
                                initialOffsetY = { it / 2 },
                                animationSpec = tween(400, delayMillis = 200, easing = FastOutSlowInEasing)
                            )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = " ${item.rating}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        AssistChip(
                            onClick = { },
                            label = { Text(item.category) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description Card with slide animation
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(400, delayMillis = 300)) +
                            slideInVertically(
                                initialOffsetY = { it / 2 },
                                animationSpec = tween(400, delayMillis = 300, easing = FastOutSlowInEasing)
                            )
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Description",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Owner Info Card with slide animation
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(400, delayMillis = 400)) +
                            slideInVertically(
                                initialOffsetY = { it / 2 },
                                animationSpec = tween(400, delayMillis = 400, easing = FastOutSlowInEasing)
                            )
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Owner Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(item.ownerName)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Email, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(item.ownerEmail, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons with slide animation
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(400, delayMillis = 500)) +
                            slideInVertically(
                                initialOffsetY = { it / 2 },
                                animationSpec = tween(400, delayMillis = 500, easing = FastOutSlowInEasing)
                            )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { /* Handle rent action */ },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Rent Now")
                        }

                        OutlinedButton(
                            onClick = { /* Handle contact owner */ },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Contact")
                        }
                    }
                }
            }
        }
    }
}

data class NavigationItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun SmoothParticleEffect(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    repeat(6) { i ->
        val angle = (360f / 6f) * i
        val delay = i * 50
        val distance by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = if (isActive) 30f else 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, delayMillis = delay, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "distance$i"
        )
        val alpha by infiniteTransition.animateFloat(
            initialValue = if (isActive) 0.8f else 0f,
            targetValue = if (isActive) 0f else 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, delayMillis = delay, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha$i"
        )
        if (isActive) {
            Box(
                modifier = Modifier
                    .offset(
                        x = (cos(angle * PI / 180f) * distance).dp,
                        y = (sin(angle * PI / 180f) * distance).dp
                    )
                    .size(6.dp)
                    .background(Color(0xFF0000FF).copy(alpha = alpha), CircleShape)
            )
        }
    }
}

@Composable
fun SmoothGooeyBottomNav(navController: NavHostController) {
    val haptic = LocalHapticFeedback.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var selectedIndex by remember { mutableIntStateOf(0) }
    val animatedOffset by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "gooeyOffset"
    )

    val navigationItems = listOf(
        NavigationItem(Screen.Home.route, Screen.Home.title, Screen.Home.icon),
        NavigationItem(Screen.Rentals.route, Screen.Rentals.title, Screen.Rentals.icon),
        NavigationItem(Screen.Profile.route, Screen.Profile.title, Screen.Profile.icon)
    )

    LaunchedEffect(currentRoute) {
        val newIndex = navigationItems.indexOfFirst { it.route == currentRoute }
        if (newIndex != -1) selectedIndex = newIndex
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(85.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val itemWidth = size.width / navigationItems.size
            val blobCenterX = itemWidth * (animatedOffset + 0.5f)
            val blobCenterY = size.height / 2f
            drawPillShape(
                Offset(blobCenterX, blobCenterY - 8.dp.toPx()),
                80.dp.toPx(),
                50.dp.toPx(),
                Color(0xFF1E90FF)
            )
        }
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navigationItems.forEachIndexed { index, navItem ->
                val isSelected = selectedIndex == index
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "iconScale$index"
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (selectedIndex != index) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                navController.navigate(navItem.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                        .scale(scale)
                        .padding(vertical = 8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            navItem.icon,
                            contentDescription = navItem.label,
                            tint = if (isSelected) Color(0xFF0000FF) else Color.Gray,
                            modifier = Modifier.size(if (isSelected) 26.dp else 22.dp)
                        )
                        SmoothParticleEffect(isSelected)
                    }
                    AnimatedVisibility(
                        visible = isSelected,
                        enter = fadeIn(tween(300)) + slideInVertically(tween(300), initialOffsetY = { it / 2 }),
                        exit = fadeOut(tween(200)) + slideOutVertically(tween(200), targetOffsetY = { it / 2 })
                    ) {
                        Text(
                            text = navItem.label,
                            fontSize = 11.sp,
                            color = if (isSelected) Color(0xFF0000FF) else Color.Gray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

fun DrawScope.drawPillShape(center: Offset, width: Float, height: Float, color: Color) {
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.9f), color.copy(alpha = 0.7f)),
            center = center,
            radius = width / 2f
        ),
        topLeft = Offset(center.x - width / 2f, center.y - height / 2f),
        size = Size(width, height),
        cornerRadius = CornerRadius(height / 2f, height / 2f)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(navController: NavHostController, userEmail: String) {
    var productName by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Electronics") }
    var expanded by remember { mutableStateOf(false) }
    val categories = listOf("Books", "Electronics", "Furniture", "Sports", "Notes", "Other")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Product for Rent") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "List Your Item",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Fill in the details to rent out your product",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            item {
                // Product Image Upload (Placeholder)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable { /* Handle image upload */ },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Add Image",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tap to add product image")
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Product Name") },
                    placeholder = { Text("e.g., Canon DSLR Camera") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) }
                )
            }

            item {
                OutlinedTextField(
                    value = productPrice,
                    onValueChange = { productPrice = it },
                    label = { Text("Rental Price per Day") },
                    placeholder = { Text("e.g., 200") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("₹ ") },
                    suffix = { Text("/day") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                )
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = {
                            Icon(
                                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Dropdown"
                            )
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = productDescription,
                    onValueChange = { productDescription = it },
                    label = { Text("Description") },
                    placeholder = { Text("Describe your product, its condition, and any additional details...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    maxLines = 6,
                    leadingIcon = {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null
                        )
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        // TODO: Save product to Firebase
                        // For now, just navigate back
                        navController.navigateUp()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = productName.isNotBlank() && productPrice.isNotBlank() && productDescription.isNotBlank()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("List Product", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", fontSize = 16.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(navController: NavHostController, categoryName: String) {
    val allItems = remember { getSampleRentalItems() }
    val filteredItems = remember(categoryName) {
        allItems.filter { it.category == categoryName }
    }

    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(categoryName) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No items in $categoryName",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Check back later for new listings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(300)) +
                                slideInVertically(
                                    initialOffsetY = { -it / 4 },
                                    animationSpec = tween(300)
                                )
                    ) {
                        Text(
                            "${filteredItems.size} items available",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray
                        )
                    }
                }

                itemsIndexed(filteredItems) { index, item ->
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(300, delayMillis = index * 50)) +
                                slideInVertically(
                                    initialOffsetY = { it / 2 },
                                    animationSpec = tween(300, delayMillis = index * 50)
                                )
                    ) {
                        CategoryItemCard(
                            item = item,
                            onClick = { navController.navigate("item_detail/${item.id}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryItemCard(item: RentalItem, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable {
                isPressed = true
                onClick()
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Product Image
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            // Product Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = " ${item.rating}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.price,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    if (item.available) {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    "Available",
                                    fontSize = 12.sp
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f),
                                labelColor = Color(0xFF4CAF50)
                            )
                        )
                    }
                }
            }
        }
    }
}