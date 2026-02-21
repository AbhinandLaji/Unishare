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
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.ezio.unishare.ui.theme.PeerRentTheme
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.*

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userEmail = intent.getStringExtra("USER_EMAIL") ?: "User"

        // Initialize Cloudinary
        try {
            val config = mapOf(
                "cloud_name" to "dsbv82xeg", // Replace with your credentials
                "api_key" to "969922813769336",
                "api_secret" to "CNZ9RMnaabxWNq-nl3TvwWwMbjI"
            )
            MediaManager.init(this, config)
        } catch (e: Exception) {}

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
            // Matches routes defined in Screen.kt
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
        composable(Screen.Rentals.route) { RentalScreen() }
        composable(Screen.Profile.route) { ProfileScreen(navController = navController, userEmail = userEmail) }
        composable(
            route = "item_detail/{itemId}",
            arguments = listOf(navArgument("itemId") { type = NavType.IntType }) // Updated to Int to match DB
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getInt("itemId") ?: 0
            ItemDetailScreen(navController, itemId, userEmail)
        }
        composable("add_product") { AddProductScreen(navController, userEmail) }
    }
}

@Composable
fun HomeScreenContent(navController: NavHostController) {
    val rentalItems = remember { mutableStateListOf<RentalItem>() }
    var isLoading by remember { mutableStateOf(true) }

    // Real-time fetching from your Fedora backend
    LaunchedEffect(Unit) {
        RetrofitClient.instance.getAvailableItems().enqueue(object : Callback<List<RentalItem>> {
            override fun onResponse(call: Call<List<RentalItem>>, response: Response<List<RentalItem>>) {
                if (response.isSuccessful) {
                    rentalItems.clear()
                    response.body()?.let { rentalItems.addAll(it) }
                }
                isLoading = false
            }
            override fun onFailure(call: Call<List<RentalItem>>, t: Throwable) { isLoading = false }
        })
    }

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
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { imageUri = it }

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
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

                Button(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    enabled = !isUploading && imageUri != null && name.isNotBlank(),
                    onClick = {
                        isUploading = true
                        // Upload image to Cloudinary first
                        MediaManager.get().upload(imageUri).unsigned("your_preset").callback(object : UploadCallback {
                            override fun onSuccess(requestId: String?, resultData: Map<*, *>) {
                                val cloudUrl = resultData["secure_url"].toString()
                                val data = mapOf(
                                    "name" to name, "price" to price, "description" to desc,
                                    "category" to "Electronics", "email" to userEmail, "image_url" to cloudUrl
                                )
                                // Send link and details to Fedora backend
                                RetrofitClient.instance.addItem(data).enqueue(object : Callback<ApiResponse> {
                                    override fun onResponse(call: Call<ApiResponse>, r: Response<ApiResponse>) {
                                        if (r.isSuccessful) showSuccessScreen = true
                                        isUploading = false
                                    }
                                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) { isUploading = false }
                                })
                            }
                            override fun onError(id: String?, err: ErrorInfo?) { isUploading = false }
                            override fun onStart(id: String?) {}
                            override fun onProgress(id: String?, b: Long, t: Long) {}
                            override fun onReschedule(id: String?, err: ErrorInfo?) {}
                        }).dispatch()
                    }
                ) { if (isUploading) CircularProgressIndicator(color = Color.White) else Text("Place item for Rent") }
            }
        }
    }
}

// --- UI HELPERS WITH FIXED PARAMETER NAMES ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CustomTopBar(
    scrollBehavior: TopAppBarScrollBehavior?,
    currentRoute: String?,
    navController: NavHostController,
    userEmail: String
) {
    TopAppBar(
        title = { Column { Text(currentRoute ?: "Home"); Text(userEmail, fontSize = 12.sp, color = Color.Gray) } },
        actions = {
            IconButton(onClick = { /* Handle chat */ }) { Icon(Icons.Filled.Chat, "Messages") }
            IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                Icon(Icons.Filled.Person, "Profile", modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ItemDetailScreen(navController: NavHostController, itemId: Int, userEmail: String) {
    var item by remember { mutableStateOf<RentalItem?>(null) }
    var showRentalDialog by remember { mutableStateOf(false) }
    var showRequestSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        RetrofitClient.instance.getAvailableItems().enqueue(object : Callback<List<RentalItem>> {
            override fun onResponse(call: Call<List<RentalItem>>, response: Response<List<RentalItem>>) {
                item = response.body()?.find { it.item_id == itemId }
            }
            override fun onFailure(call: Call<List<RentalItem>>, t: Throwable) {}
        })
    }

    if (showRequestSuccess) {
        RentalRequestSuccessScreen(itemName = item?.name ?: "") { showRequestSuccess = false; navController.navigateUp() }
    } else {
        if (showRentalDialog) {
            RentItemConfirmationDialog(item?.name ?: "", onDismiss = { showRentalDialog = false }) { days ->
                val data = mapOf("item_id" to itemId.toString(), "renter_email" to userEmail, "rental_days" to days)
                RetrofitClient.instance.requestItem(data).enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, r: Response<ApiResponse>) {
                        if (r.isSuccessful) { showRentalDialog = false; showRequestSuccess = true }
                        else Toast.makeText(navController.context, "Max 5 active rentals reached", Toast.LENGTH_SHORT).show()
                    }
                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
                })
            }
        }

        Scaffold(topBar = { TopAppBar(title = { Text("Details") }, navigationIcon = { IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { padding ->
            item?.let {
                Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                    AsyncImage(model = it.imageUrl, contentDescription = null, modifier = Modifier.height(300.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                    Text(it.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("₹${it.price}/day", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
                    Text(it.description, modifier = Modifier.padding(vertical = 16.dp))
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { showRentalDialog = true }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Rent Now") }
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

@Composable fun ProfileScreen(navController: NavHostController, userEmail: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Person, null, modifier = Modifier.size(80.dp))
            Text("Profile for $userEmail", style = MaterialTheme.typography.headlineSmall)
        }
    }
}