package com.ezio.unishare

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.ByteArrayOutputStream
import kotlin.math.*
import androidx.compose.animation.AnimatedContentTransitionScope

// Data class for rental items
data class RentalItem(
    val id: String = "",
    val name: String = "",
    val price: String = "",
    val description: String = "",
    val category: String = "",
    val ownerName: String = "",
    val ownerEmail: String = "",
    val imageUrl: String = "",
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

@Composable
fun Base64Image(
    base64String: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val imageBitmap = remember(base64String) {
        try {
            val pureBase64 = base64String.substringAfter(',')
            val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        // Placeholder in case of decoding error
        Box(
            modifier = modifier.background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = "Image load failed",
                tint = Color.White
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniShareAppScreen(userEmail: String) {
    val navController = rememberNavController()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val rentalItems = remember { mutableStateListOf<RentalItem>() }
    var currentUserName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    // Fetch all rental items from Firebase
    LaunchedEffect(Unit) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("rentals")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<RentalItem>()
                for (childSnapshot in snapshot.children) {
                    val item = childSnapshot.getValue(RentalItem::class.java)
                    item?.let { items.add(it) }
                }
                rentalItems.clear()
                rentalItems.addAll(items.reversed()) // Show newest first
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        }
        databaseRef.addValueEventListener(listener)
    }

    // Fetch user's name from Firebase
    LaunchedEffect(userEmail) {
        if (userEmail.isNotBlank()) {
            val userKey = userEmail.replace(".", "_")
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(userKey)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val firstName = snapshot.child("firstName").getValue(String::class.java)
                    if (firstName != null) {
                        currentUserName = firstName
                    } else {
                        currentUserName = userEmail.substringBefore('@')
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    currentUserName = userEmail.substringBefore('@')
                }
            })
        }
    }

    val filteredItems = remember(searchQuery, rentalItems) {
        if (searchQuery.isBlank()) {
            rentalItems
        } else {
            rentalItems.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

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
            if (currentRoute != "item_detail/{itemId}" && currentRoute != "add_product") {
                SmoothGooeyBottomNav(navController = navController)
            }
        },
        floatingActionButton = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
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
        AppNavHost(
            navController,
            Modifier.padding(paddingValues),
            userEmail = userEmail,
            userName = currentUserName,
            rentalItems = filteredItems,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it }
        )
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    userEmail: String,
    userName: String,
    rentalItems: List<RentalItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
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
            HomeScreenContent(
                navController = navController,
                rentalItems = rentalItems,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange
            )
        }
        composable(Screen.Rentals.route) { 
            RentalScreen(rentalItems = rentalItems, userEmail = userEmail) 
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController, userEmail = userEmail)
        }
        composable(
            route = "item_detail/{itemId}",
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            ItemDetailScreen(navController = navController, itemId = itemId, rentalItems = rentalItems, currentUserEmail = userEmail)
        }
        composable(
            route = "category/{categoryName}",
            arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
            CategoryListScreen(navController = navController, categoryName = categoryName, rentalItems = rentalItems)
        }
        composable("add_product") {
            AddProductScreen(
                navController = navController,
                userEmail = userEmail,
                userName = userName
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopBar(scrollBehavior: TopAppBarScrollBehavior?, currentRoute: String?, navController: NavHostController, userEmail: String) {
    val title = when (currentRoute) {
        Screen.Home.route -> "Home"
        Screen.Rentals.route -> "My Rentals"
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
                }) { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Messages") }

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
fun HomeScreenContent(
    navController: NavHostController, 
    rentalItems: List<RentalItem>,
    searchQuery: String, 
    onSearchQueryChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SearchBarSection(searchQuery, onSearchQueryChange)

        if (searchQuery.isBlank()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
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
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(rentalItems) { item ->
                    SearchResultItem(item = item) {
                        navController.navigate("item_detail/${item.id}")
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(item: RentalItem, onItemClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onItemClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Base64Image(
                base64String = item.imageUrl,
                contentDescription = item.name,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(item.price, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun RentalScreen(rentalItems: List<RentalItem>, userEmail: String) {
    val myItems = remember(rentalItems, userEmail) {
        rentalItems.filter { it.ownerEmail == userEmail }
    }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("My Listed Items", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        if (myItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("You haven't listed any items for rent yet.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(myItems) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Base64Image(
                                base64String = item.imageUrl,
                                contentDescription = item.name,
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                Text(item.price, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = {
                                val databaseRef = FirebaseDatabase.getInstance().getReference("rentals")
                                databaseRef.child(item.id).removeValue()
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show()
                                    }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Item",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarSection(searchQuery: String, onSearchQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Search for rentals...") },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                }
            } else {
                Icon(Icons.Filled.Mic, contentDescription = "Mic")
            }
        }
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
    val gridHeight = if (items.isEmpty()) 0.dp else (((items.size + 1) / 2) * 180).dp
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.height(gridHeight).padding(horizontal = 12.dp),
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
                    Base64Image(
                        base64String = item.imageUrl,
                        contentDescription = item.name,
                        modifier = Modifier.height(100.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp))
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
fun ItemDetailScreen(navController: NavHostController, itemId: String, rentalItems: List<RentalItem>, currentUserEmail: String) {
    val item = remember(itemId, rentalItems) { rentalItems.find { it.id == itemId } }
    val context = LocalContext.current

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
                        Base64Image(
                            base64String = item.imageUrl,
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize()
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
                            onClick = {
                                if (currentUserEmail != item.ownerEmail) {
                                    val intent = Intent(context, ConversationActivity::class.java).apply {
                                        putExtra("CURRENT_USER_EMAIL", currentUserEmail)
                                        putExtra("OTHER_USER_EMAIL", item.ownerEmail)
                                    }
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, "You cannot chat with yourself.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = currentUserEmail != item.ownerEmail
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
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
fun AddProductScreen(
    navController: NavHostController,
    userEmail: String,
    userName: String
) {
    var productName by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Electronics") }
    var expanded by remember { mutableStateOf(false) }
    val categories = listOf("Books", "Electronics", "Furniture", "Sports", "Notes", "Other")
    val context = LocalContext.current

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }


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
        Box(modifier = Modifier.fillMaxSize()) {

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
                    // Product Image Upload
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUri != null) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Selected Product Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
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
                            if (imageUri == null) {
                                Toast.makeText(context, "Please select an image.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // Convert image Uri to Base64
                            val inputStream = context.contentResolver.openInputStream(imageUri!!)
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            val byteArrayOutputStream = ByteArrayOutputStream()
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream) // Compress to reduce size
                            val byteArray = byteArrayOutputStream.toByteArray()
                            val encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)
                            val imageAsString = "data:image/jpeg;base64,$encodedImage"

                            val newItemId = System.currentTimeMillis().toString()
                            val newItem = RentalItem(
                                id = newItemId,
                                name = productName,
                                price = "₹$productPrice",
                                description = productDescription,
                                category = selectedCategory,
                                ownerName = userName,
                                ownerEmail = userEmail,
                                imageUrl = imageAsString
                            )
                            
                            val database = FirebaseDatabase.getInstance()
                            val rentalsRef = database.getReference("rentals")

                            rentalsRef.child(newItemId).setValue(newItem)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Product listed successfully!", Toast.LENGTH_SHORT).show()
                                    navController.navigateUp()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Failed to list product. Please try again.", Toast.LENGTH_SHORT).show()
                                }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    navController: NavHostController,
    categoryName: String,
    rentalItems: List<RentalItem>
) {
    val filteredItems = remember(categoryName, rentalItems) {
        rentalItems.filter { it.category == categoryName }
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
            Base64Image(
                base64String = item.imageUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp))
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
