package com.ezio.unishare

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.*
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
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.ezio.unishare.ui.theme.PeerRentTheme
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userEmail = intent.getStringExtra("USER_EMAIL") ?: "User"

        // Initialize Cloudinary with your new credentials
        try {
            MediaManager.get()
        } catch (e: Exception) {
            val config = mapOf(
                "cloud_name" to "diy92xdcf",
                "api_key" to "955723828439185",
                "api_secret" to "4CWdBK3BOZ54x_a4oohlbhF1BXg"
            )
            MediaManager.init(this, config)
        }

        setContent {
            PeerRentTheme { UniShareAppScreen(userEmail = userEmail) }
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
            if (currentRoute != "item_detail/{itemId}" && currentRoute != "add_product") {
                SmoothGooeyBottomNav(navController = navController)
            }
        },
        floatingActionButton = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            if (navBackStackEntry?.destination?.route == Screen.Home.route) {
                FloatingActionButton(
                    onClick = { navController.navigate("add_product") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) { Icon(Icons.Filled.Add, contentDescription = "Add") }
            }
        }
    ) { paddingValues ->
        AppNavHost(navController, Modifier.padding(paddingValues), userEmail = userEmail)
    }
}

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier, userEmail: String) {
    NavHost(navController = navController, startDestination = Screen.Home.route, modifier = modifier) {
        composable(Screen.Home.route) { HomeScreenContent(navController = navController) }
        composable(Screen.Rentals.route) {
            RentalScreen(userEmail = userEmail)
        }
        composable(Screen.Profile.route) { ProfileScreen(navController = navController, userEmail = userEmail) }
        composable(
            route = "item_detail/{itemId}",
            arguments = listOf(navArgument("itemId") { type = NavType.IntType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getInt("itemId") ?: 0
            ItemDetailScreen(navController, itemId, userEmail)
        }
        composable("add_product") { AddProductScreen(navController, userEmail) }
        composable("rental_requests") { RentalRequestsScreen(userEmail = userEmail) }
    }
}

@Composable
fun HomeScreenContent(navController: NavHostController) {
    val rentalItems = remember { mutableStateListOf<RentalItem>() }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current // NEEDED for Toasts

    LaunchedEffect(Unit) {
        RetrofitClient.instance.getAvailableItems().enqueue(object : Callback<List<RentalItem>> {
            override fun onResponse(call: Call<List<RentalItem>>, response: Response<List<RentalItem>>) {
                if (response.isSuccessful) {
                    rentalItems.clear()
                    response.body()?.let { items ->
                        rentalItems.addAll(items)
                        if (items.isEmpty()) {
                            Toast.makeText(context, "Database returned 0 items", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Success! Loaded ${items.size} items", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Server Error: ${response.code()}", Toast.LENGTH_LONG).show()
                }
                isLoading = false
            }
            override fun onFailure(call: Call<List<RentalItem>>, t: Throwable) {
                // CRUCIAL: This will tell us if there's a JSON crash or network failure
                Toast.makeText(context, "Fetch Failed: ${t.message}", Toast.LENGTH_LONG).show()
                isLoading = false
            }
        })
    }

    // ... rest of the UI (LazyColumn, etc.) stays exactly the same

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
        item { SearchBarSection() }
        item { BannerCarousel() }
        item { CategoryRow(navController = navController) }
        item {
            Text("Popular Rentals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
        }
        item {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                RentalItemGrid(items = rentalItems) { item ->
                    navController.navigate("item_detail/${item.item_id}")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(navController: NavHostController, userEmail: String) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var showSuccessScreen by remember { mutableStateOf(false) }

    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Electronics") }
    val categories = listOf("Books", "Electronics", "Furniture", "Sports", "Notes", "Other")

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { imageUri = it }
    val context = LocalContext.current

    if (showSuccessScreen) {
        SuccessScreen(productName = name) {
            showSuccessScreen = false
            navController.navigateUp()
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Add Product") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
                Card(modifier = Modifier.fillMaxWidth().height(200.dp).clickable { launcher.launch("image/*") }) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (imageUri == null) Icon(Icons.Filled.AddAPhoto, "Add Image", modifier = Modifier.size(48.dp))
                        else AsyncImage(model = imageUri, contentDescription = null, contentScale = ContentScale.Crop)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price/Day") }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
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

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

                Button(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    // Button is enabled if name is not blank and an image is picked
                    enabled = !isUploading && imageUri != null && name.isNotBlank(),
                    onClick = {
                        isUploading = true

                        // --- FRIEND'S CLOUDINARY FIX INJECTED HERE ---
                        MediaManager.get().upload(imageUri)
                            .callback(object : UploadCallback {
                                override fun onStart(requestId: String) {}
                                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                                    // Force it to string, and check both "secure_url" and standard "url"
                                    val secureUrl = resultData["secure_url"]?.toString() ?: resultData["url"]?.toString() ?: ""

                                    if (secureUrl.isEmpty()) {
                                        Toast.makeText(context, "Cloudinary upload failed to return a URL!", Toast.LENGTH_LONG).show()
                                    }

                                    val data = mapOf(
                                        "name" to name,
                                        "price" to price,
                                        "description" to desc,
                                        "category" to selectedCategory,
                                        "email" to userEmail,
                                        "image_url" to secureUrl // Sends the real URL to Flask
                                    )

                                    // ... RetrofitClient.instance.addItem(data)... stays exactly the same

                                    // Now send the data to Flask
                                    RetrofitClient.instance.addItem(data).enqueue(object : Callback<ApiResponse> {
                                        override fun onResponse(call: Call<ApiResponse>, r: Response<ApiResponse>) {
                                            if (r.isSuccessful) {
                                                showSuccessScreen = true
                                            } else {
                                                Toast.makeText(context, "Error: ${r.code()}", Toast.LENGTH_LONG).show()
                                            }
                                            isUploading = false
                                        }

                                        override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                                            Toast.makeText(context, "Failed: ${t.message}", Toast.LENGTH_LONG).show()
                                            isUploading = false
                                        }
                                    })
                                }

                                override fun onError(requestId: String?, error: ErrorInfo?) {
                                    isUploading = false
                                    Toast.makeText(context, "Image Upload Failed: ${error?.description}", Toast.LENGTH_LONG).show()
                                }

                                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                            }).dispatch()
                    }
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Place item for Rent")
                    }
                }
            }
        }
    }
}

// --- UI HELPERS ---

// Replace CustomTopBar in HomeActivity.kt with this

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopBar(
    scrollBehavior: TopAppBarScrollBehavior?,
    currentRoute: String?,
    navController: NavHostController,
    userEmail: String
) {
    var pendingCount by remember { mutableStateOf(0) }

    // Poll for pending requests every 30 seconds
    LaunchedEffect(userEmail) {
        while (true) {
            RetrofitClient.instance.getPendingCount(userEmail)
                .enqueue(object : retrofit2.Callback<ApiResponse> {
                    override fun onResponse(
                        call: retrofit2.Call<ApiResponse>,
                        response: retrofit2.Response<ApiResponse>
                    ) {
                        if (response.isSuccessful) {
                            pendingCount = response.body()?.count ?: 0
                        }
                    }
                    override fun onFailure(call: retrofit2.Call<ApiResponse>, t: Throwable) {}
                })
            kotlinx.coroutines.delay(30_000) // refresh every 30 seconds
        }
    }

    TopAppBar(
        title = {
            Column {
                Text(currentRoute ?: "Home")
                Text(userEmail, fontSize = 12.sp, color = Color.Gray)
            }
        },
        actions = {
            // Bell icon with badge
            Box {
                IconButton(onClick = { navController.navigate("rental_requests") }) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Requests")
                }
                if (pendingCount > 0) {
                    Badge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-6).dp, y = 6.dp),
                        containerColor = MaterialTheme.colorScheme.error
                    ) {
                        Text(
                            text = if (pendingCount > 9) "9+" else pendingCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Profile icon
            IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                Icon(
                    Icons.Filled.Person,
                    "Profile",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable fun SearchBarSection() { OutlinedTextField(value = "", onValueChange = {}, placeholder = { Text("Search...") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), leadingIcon = { Icon(Icons.Filled.Search, null) }) }
@Composable fun BannerCarousel() { Box(modifier = Modifier.height(120.dp).fillMaxWidth().background(Color.LightGray)) }
@Composable fun CategoryRow(navController: NavHostController) { Row(modifier = Modifier.horizontalScroll(rememberScrollState())) { listOf("Books", "Electronics", "Sports").forEach { AssistChip(onClick = {}, label = { Text(it) }, modifier = Modifier.padding(4.dp)) } } }

@Composable fun RentalItemGrid(items: List<RentalItem>, onClick: (RentalItem) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(500.dp)) {
        items(items) { item ->
            Card(modifier = Modifier.clickable { onClick(item) }.padding(4.dp)) {
                Column {
                    AsyncImage(model = item.imageUrl, contentDescription = null, modifier = Modifier.height(100.dp).fillMaxWidth(), contentScale = ContentScale.Crop)
                    Text(item.name, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp), maxLines = 1)
                    Text("₹${item.price}", modifier = Modifier.padding(start = 4.dp, bottom = 4.dp), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable fun SmoothGooeyBottomNav(navController: NavHostController) {
    NavigationBar {
        listOf(Screen.Home, Screen.Rentals, Screen.Profile).forEach { screen ->
            NavigationBarItem(
                selected = false,
                onClick = { navController.navigate(screen.route) },
                icon = { Icon(screen.icon, null) },
                label = { Text(screen.title) }
            )
        }
    }
}

@Composable fun SuccessScreen(productName: String, onComplete: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.CheckCircle, "Success", tint = Color.Green, modifier = Modifier.size(100.dp))
            Text("Listed $productName!", fontWeight = FontWeight.Bold)
            LaunchedEffect(Unit) { delay(2000); onComplete() }
        }
    }
}

// Replace the ItemDetailScreen function and related composables in HomeActivity.kt with this

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ItemDetailScreen(navController: NavHostController, itemId: Int, userEmail: String) {
    var item by remember { mutableStateOf<RentalItem?>(null) }
    var showRentalDialog by remember { mutableStateOf(false) }
    var showRequestSuccess by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }

    // NEW: State to track if the user has already requested this item
    var hasRequested by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(itemId) {
        // 1. Fetch Item Details
        RetrofitClient.instance.getAvailableItems().enqueue(object : Callback<List<RentalItem>> {
            override fun onResponse(call: Call<List<RentalItem>>, response: Response<List<RentalItem>>) {
                item = response.body()?.find { it.item_id == itemId }
                visible = true
            }
            override fun onFailure(call: Call<List<RentalItem>>, t: Throwable) {}
        })

        // 2. Fetch User's Rentals to check if already requested
        // 2. Fetch User's Rentals to check if already requested
        RetrofitClient.instance.getMyRentals(userEmail).enqueue(object : Callback<List<RentalRequest>> {
            override fun onResponse(call: Call<List<RentalRequest>>, response: Response<List<RentalRequest>>) {
                val rentals = response.body() ?: emptyList()
                // If the item exists in their rentals and is pending or accepted, lock the button
                val existingRequest = rentals.find {
                    it.item_id == itemId && (it.status == "pending" || it.status == "Accepted")
                }
                if (existingRequest != null) {
                    hasRequested = true
                }
            }

            override fun onFailure(call: Call<List<RentalRequest>>, t: Throwable) {}
        })
    }

    if (showRequestSuccess) {
        RentalRequestSuccessScreen(itemName = item?.name ?: "") {
            showRequestSuccess = false
            // Optional: Remove navigateUp() here if you want them to stay on the page and see the grey button
            // navController.navigateUp()
        }
    } else {
        if (showRentalDialog) {
            RentItemConfirmationDialog(item?.name ?: "", onDismiss = { showRentalDialog = false }) { days ->
                val data = mapOf("item_id" to itemId.toString(), "renter_email" to userEmail, "rental_days" to days)
                RetrofitClient.instance.requestItem(data).enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, r: Response<ApiResponse>) {
                        if (r.isSuccessful) {
                            showRentalDialog = false
                            showRequestSuccess = true
                            hasRequested = true // Instantly lock the button upon success
                        } else {
                            Toast.makeText(context, "Max 5 active rentals reached", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
                })
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Details") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    }
                )
            }
        ) { padding ->
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                modifier = Modifier.padding(padding)
            ) {
                item?.let {
                    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                        AsyncImage(
                            model = it.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.height(300.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(it.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("₹${it.price}/day", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        Text("Owner Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Email: ${it.owner_email}", color = Color.Gray)

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Description", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(it.description, modifier = Modifier.padding(top = 4.dp))

                        Spacer(modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.height(24.dp))

                        // --- BUTTON LOGIC ---
                        if (userEmail != it.owner_email) {
                            if (hasRequested) {
                                // Locked Grey Button
                                Button(
                                    onClick = { },
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        disabledContainerColor = Color.Gray,
                                        disabledContentColor = Color.White
                                    )
                                ) {
                                    Text("Request Sent")
                                }
                            } else {
                                // Active Blue Button
                                Button(
                                    onClick = { showRentalDialog = true },
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Text("Rent Now")
                                }
                            }
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            ) {
                                Text(
                                    "This is your listing",
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable fun RentItemConfirmationDialog(itemName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var days by remember { mutableStateOf("1") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rent $itemName") },
        text = {
            Column {
                Text("Days needed:")
                OutlinedTextField(value = days, onValueChange = { days = it }, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = { Button(onClick = { onConfirm(days) }) { Text("Request") } }
    )
}

@Composable fun RentalRequestSuccessScreen(itemName: String, onComplete: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Email, "Sent", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(100.dp))
            Text("Request sent for $itemName!")
            LaunchedEffect(Unit) { delay(2000); onComplete() }
        }
    }
}

