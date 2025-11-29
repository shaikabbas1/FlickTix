package com.example.flicktix

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flicktix.ui.theme.FlickTixTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlickTixTheme {
                HomeScreen(
                    onLogoutClick = {
                        // Sign out the user
                        FirebaseAuth.getInstance().signOut()
                        // Go back to LoginActivity
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

// ------------ DATA MODEL ------------

data class Movie(
    val id: Int = 0,
    val title: String = "",
    val genre: String = "",
    val durationMinutes: Int = 0,
    val rating: String = "",        // e.g. "PG-13"
    val language: String = "",      // e.g. "English"
    val cinema: String = "",        // e.g. "Screen 2 · City Mall"
    val showTimes: List<String> = emptyList(),
    val isTrending: Boolean = false,
    val isComingSoon: Boolean = false
)

data class Booking(
    val id: String = "",
    val movieTitle: String = "",
    val cinema: String = "",
    val tickets: Int = 0,
    val totalPrice: Int = 0,
    val paymentMethod: String = "",
    val bookingTime: Long = 0L
)

// ------------ HOME TABS ------------

enum class HomeTab {
    BOOKING,
    PAYMENT,
    HISTORY,
    PROFILE
}

// ------------ HOME SCREEN ------------

@Composable
fun HomeScreen(
    onLogoutClick: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }

    val firestoreMovies = remember { mutableStateListOf<Movie>() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Selected bottom tab
    var selectedTab by remember { mutableStateOf(HomeTab.BOOKING) }

    // Movie selected to pay for
    var selectedMovieForPayment by remember { mutableStateOf<Movie?>(null) }

    // Load movies from Firestore (collection: "movies")
    LaunchedEffect(Unit) {
        db.collection("movies")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    errorMessage = "Failed to load movies."
                    isLoading = false
                    return@addSnapshotListener
                }

                val docs = snapshot?.documents ?: emptyList()
                firestoreMovies.clear()

                for (doc in docs) {
                    val id = (doc.getLong("id") ?: 0L).toInt()
                    val title = doc.getString("title") ?: ""
                    val genre = doc.getString("genre") ?: ""
                    val durationMinutes = (doc.getLong("durationMinutes") ?: 0L).toInt()
                    val rating = doc.getString("rating") ?: ""
                    val language = doc.getString("language") ?: ""
                    val cinema = doc.getString("cinema") ?: ""
                    val showTimes = (doc.get("showTimes") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    val isTrending = doc.getBoolean("isTrending") ?: false
                    val isComingSoon = doc.getBoolean("isComingSoon") ?: false

                    firestoreMovies.add(
                        Movie(
                            id = id,
                            title = title,
                            genre = genre,
                            durationMinutes = durationMinutes,
                            rating = rating,
                            language = language,
                            cinema = cinema,
                            showTimes = showTimes,
                            isTrending = isTrending,
                            isComingSoon = isComingSoon
                        )
                    )
                }

                errorMessage = null
                isLoading = false
            }
    }

    // If Firestore has data, use it. Otherwise fall back to local sample data
    val allMovies: List<Movie> =
        if (firestoreMovies.isNotEmpty()) firestoreMovies else sampleMovies()

    // Booking tab filters
    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("All") }

    val genres = listOf("All", "Action", "Comedy", "Drama", "Sci-Fi")

    val filteredNowShowing = allMovies.filter { movie ->
        !movie.isComingSoon &&
                (searchQuery.isBlank() ||
                        movie.title.contains(searchQuery, ignoreCase = true) ||
                        movie.genre.contains(searchQuery, ignoreCase = true)) &&
                (selectedGenre == "All" || movie.genre.equals(selectedGenre, ignoreCase = true))
    }

    val filteredComingSoon = allMovies.filter { movie ->
        movie.isComingSoon &&
                (searchQuery.isBlank() ||
                        movie.title.contains(searchQuery, ignoreCase = true) ||
                        movie.genre.contains(searchQuery, ignoreCase = true)) &&
                (selectedGenre == "All" || movie.genre.equals(selectedGenre, ignoreCase = true))
    }

    Scaffold(
        topBar = { HomeTopBar(onLogoutClick = onLogoutClick) },
        bottomBar = {
            HomeBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            when (selectedTab) {
                HomeTab.BOOKING -> {
                    // --- BOOKING TAB CONTENT (home screen) ---
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isLoading) {
                            Text(
                                text = "Loading movies...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        errorMessage?.let { msg ->
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Search
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search movies") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Genre filters
                        Text(
                            text = "Browse by genre",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            genres.forEach { genre ->
                                GenreChip(
                                    text = genre,
                                    selected = selectedGenre == genre,
                                    onClick = { selectedGenre = genre }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (filteredNowShowing.isNotEmpty()) {
                                item {
                                    SectionHeader(title = "Now Showing")
                                }
                                items(filteredNowShowing, key = { it.id }) { movie ->
                                    MovieCard(
                                        movie = movie,
                                        onBookClick = {
                                            // Book button: select movie + go to Payment tab
                                            selectedMovieForPayment = movie
                                            selectedTab = HomeTab.PAYMENT
                                        },
                                        onCardClick = {
                                            // TODO: Navigate to movie details (optional)
                                        }
                                    )
                                }
                            }

                            if (filteredComingSoon.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SectionHeader(title = "Coming Soon")
                                }
                                items(filteredComingSoon, key = { it.id }) { movie ->
                                    MovieCard(
                                        movie = movie,
                                        onBookClick = {
                                            selectedMovieForPayment = movie
                                            selectedTab = HomeTab.PAYMENT
                                        },
                                        onCardClick = {
                                            // TODO: Navigate to movie details (optional)
                                        }
                                    )
                                }
                            }

                            if (filteredNowShowing.isEmpty() && filteredComingSoon.isEmpty() && !isLoading) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No movies match your search.",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HomeTab.PAYMENT -> {
                    // --- PAYMENT TAB CONTENT ---
                    PaymentScreen(selectedMovie = selectedMovieForPayment)
                }

                HomeTab.HISTORY -> {
                    // --- HISTORY TAB CONTENT ---
                    HistoryScreen()
                }

                HomeTab.PROFILE -> {
                    // --- PROFILE TAB CONTENT ---
                    ProfileScreen(onLogoutClick = onLogoutClick)
                }
            }
        }
    }
}

// ------------ TOP BAR ------------

@Composable
fun HomeTopBar(
    onLogoutClick: () -> Unit
) {
    val gradientColors = listOf(
        Color(0xFF0D47A1),
        Color(0xFF1976D2)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(gradientColors))
            .padding(
                top = WindowInsets.statusBars
                    .asPaddingValues()
                    .calculateTopPadding()
            )
            .height(72.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "FlickTix",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Book your next movie night",
                    color = Color(0xFFBBDEFB),
                    fontSize = 13.sp
                )
            }

            IconButton(onClick = onLogoutClick) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Logout",
                    tint = Color.White
                )
            }
        }
    }
}

// ------------ BOTTOM NAV BAR ------------

@Composable
fun HomeBottomBar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == HomeTab.BOOKING,
            onClick = { onTabSelected(HomeTab.BOOKING) },
            icon = { Text("🎬") },
            label = { Text("Booking") }
        )
        NavigationBarItem(
            selected = selectedTab == HomeTab.PAYMENT,
            onClick = { onTabSelected(HomeTab.PAYMENT) },
            icon = { Text("💳") },
            label = { Text("Payment") }
        )
        NavigationBarItem(
            selected = selectedTab == HomeTab.HISTORY,
            onClick = { onTabSelected(HomeTab.HISTORY) },
            icon = { Text("📜") },
            label = { Text("History") }
        )
        NavigationBarItem(
            selected = selectedTab == HomeTab.PROFILE,
            onClick = { onTabSelected(HomeTab.PROFILE) },
            icon = { Text("👤") },
            label = { Text("Profile") }
        )
    }
}

// ------------ PAYMENT SCREEN ------------

@Composable
fun PaymentScreen(selectedMovie: Movie?) {
    if (selectedMovie == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No movie selected.",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Go to the Booking tab and choose a movie.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    var tickets by remember { mutableStateOf("1") }

    // Each ticket is £8
    val pricePerTicket = 8
    val ticketCount = tickets.toIntOrNull() ?: 0
    val totalPrice = ticketCount * pricePerTicket

    // Payment method: Cash or Card
    var selectedPaymentMethod by remember { mutableStateOf<String?>(null) }

    // For showing success popup
    var showSuccessDialog by remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Payment",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Selected movie info
        Text(
            text = selectedMovie.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "${selectedMovie.genre} • ${selectedMovie.durationMinutes} min • ${selectedMovie.language}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = selectedMovie.cinema,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tickets input
        OutlinedTextField(
            value = tickets,
            onValueChange = { tickets = it.filter { ch -> ch.isDigit() } },
            label = { Text("Number of tickets") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Price breakdown like a real app
        Text(
            text = "Price per ticket: £$pricePerTicket",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Tickets: $ticketCount x £$pricePerTicket",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Total: £$totalPrice",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Payment method selection
        Text(
            text = "Payment method",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { selectedPaymentMethod = "Cash" }
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = selectedPaymentMethod == "Cash",
                    onClick = { selectedPaymentMethod = "Cash" }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Cash")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { selectedPaymentMethod = "Card" }
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = selectedPaymentMethod == "Card",
                    onClick = { selectedPaymentMethod = "Card" }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Card")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val user = auth.currentUser
                val method = selectedPaymentMethod ?: "Unknown"

                val bookingData = hashMapOf(
                    "userId" to (user?.uid ?: "guest"),
                    "movieTitle" to selectedMovie.title,
                    "cinema" to selectedMovie.cinema,
                    "tickets" to ticketCount,
                    "totalPrice" to totalPrice,
                    "paymentMethod" to method,
                    "bookingTime" to System.currentTimeMillis()
                )

                // Save booking in Firestore
                db.collection("bookings")
                    .add(bookingData)
                    .addOnSuccessListener {
                        showSuccessDialog = true
                    }
                    .addOnFailureListener {
                        // Even if save fails, you could show an error or still show dialog
                        showSuccessDialog = true
                    }
            },
            enabled = ticketCount > 0 && selectedPaymentMethod != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Pay Now")
        }
    }

    // Success popup
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = {
                Text(text = "Payment Successful")
            },
            text = {
                Text(
                    text = "Payment successful! Your ticket will be sent to the registered email."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showSuccessDialog = false }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

// ------------ HISTORY SCREEN ------------

@Composable
fun HistoryScreen() {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    val bookings = remember { mutableStateListOf<Booking>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(user?.uid) {
        if (user == null) {
            isLoading = false
            return@LaunchedEffect
        }

        db.collection("bookings")
            .whereEqualTo("userId", user.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                val docs = snapshot?.documents ?: emptyList()
                bookings.clear()

                for (doc in docs) {
                    val id = doc.id
                    val movieTitle = doc.getString("movieTitle") ?: ""
                    val cinema = doc.getString("cinema") ?: ""
                    val tickets = (doc.getLong("tickets") ?: 0L).toInt()
                    val totalPrice = (doc.getLong("totalPrice") ?: 0L).toInt()
                    val paymentMethod = doc.getString("paymentMethod") ?: ""
                    val bookingTime = doc.getLong("bookingTime") ?: 0L

                    bookings.add(
                        Booking(
                            id = id,
                            movieTitle = movieTitle,
                            cinema = cinema,
                            tickets = tickets,
                            totalPrice = totalPrice,
                            paymentMethod = paymentMethod,
                            bookingTime = bookingTime
                        )
                    )
                }

                // Sort latest first
                bookings.sortByDescending { it.bookingTime }
                isLoading = false
            }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Booking History",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (user == null) {
            Text(
                text = "Please log in to view your booking history.",
                style = MaterialTheme.typography.bodyMedium
            )
            return
        }

        if (isLoading) {
            Text(
                text = "Loading bookings...",
                style = MaterialTheme.typography.bodyMedium
            )
            return
        }

        if (bookings.isEmpty()) {
            Text(
                text = "No bookings yet.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(bookings, key = { it.id }) { booking ->
                    BookingCard(booking)
                }
            }
        }
    }
}

@Composable
fun BookingCard(booking: Booking) {
    val dateFormat = remember {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    }
    val dateText = dateFormat.format(Date(booking.bookingTime))

    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = booking.movieTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = booking.cinema,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tickets: ${booking.tickets}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Total: £${booking.totalPrice}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Payment: ${booking.paymentMethod}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Date: $dateText",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ------------ PROFILE SCREEN ------------

@Composable
fun ProfileScreen(
    onLogoutClick: () -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    val email = user?.email ?: "Guest User"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Email: $email",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onLogoutClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Logout")
        }
    }
}

// ------------ COMPONENTS ------------

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun GenreChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .clickable { onClick() }
    ) {
        Text(
            text = text,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 13.sp,
            color = if (selected) Color.White
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MovieCard(
    movie: Movie,
    onBookClick: () -> Unit,
    onCardClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            // Title + Rating pill
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = movie.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                RatingPill(text = movie.rating)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${movie.genre} • ${movie.durationMinutes} min • ${movie.language}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = movie.cinema,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Showtimes
            if (movie.showTimes.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    movie.showTimes.take(4).forEach { time ->
                        ShowTimeChip(time)
                    }
                    if (movie.showTimes.size > 4) {
                        Text(
                            text = "+${movie.showTimes.size - 4} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (movie.isTrending && !movie.isComingSoon) {
                    TrendingTag()
                } else if (movie.isComingSoon) {
                    ComingSoonTag()
                } else {
                    Spacer(modifier = Modifier.width(0.dp))
                }

                Button(
                    onClick = onBookClick,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .height(40.dp)
                ) {
                    Text(text = if (movie.isComingSoon) "Notify Me" else "Book")
                }
            }
        }
    }
}

@Composable
fun RatingPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun ShowTimeChip(time: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = time,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 12.sp
        )
    }
}

@Composable
fun TrendingTag() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFA000))
        )
        Text(
            text = "Trending",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFFFA000)
        )
    }
}

@Composable
fun ComingSoonTag() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFF8E24AA))
        )
        Text(
            text = "Coming Soon",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF8E24AA)
        )
    }
}

// ------------ SAMPLE DATA (fallback if Firestore is empty) ------------

fun sampleMovies(): List<Movie> = listOf(
    Movie(
        id = 1,
        title = "Galactic Odyssey",
        genre = "Sci-Fi",
        durationMinutes = 132,
        rating = "PG-13",
        language = "English",
        cinema = "Screen 1 · City Mall",
        showTimes = listOf("11:30", "14:15", "18:00", "21:30"),
        isTrending = true,
        isComingSoon = false
    ),
    Movie(
        id = 2,
        title = "Laugh Out Loud",
        genre = "Comedy",
        durationMinutes = 105,
        rating = "12A",
        language = "English",
        cinema = "Screen 3 · Grand Plaza",
        showTimes = listOf("10:00", "13:00", "16:00", "19:15"),
        isTrending = false,
        isComingSoon = false
    ),
    Movie(
        id = 3,
        title = "Midnight Chase",
        genre = "Action",
        durationMinutes = 118,
        rating = "15",
        language = "English",
        cinema = "Screen 2 · City Mall",
        showTimes = listOf("12:45", "17:10", "20:45"),
        isTrending = true,
        isComingSoon = false
    ),
    Movie(
        id = 4,
        title = "Autumn Letters",
        genre = "Drama",
        durationMinutes = 124,
        rating = "PG",
        language = "English",
        cinema = "Screen 5 · Central Cinemas",
        showTimes = listOf("09:30", "13:15", "18:20"),
        isTrending = false,
        isComingSoon = false
    ),
    Movie(
        id = 5,
        title = "Skyline Dreams",
        genre = "Drama",
        durationMinutes = 110,
        rating = "PG",
        language = "English",
        cinema = "Screen 4 · Grand Plaza",
        showTimes = emptyList(),
        isTrending = false,
        isComingSoon = true
    ),
    Movie(
        id = 6,
        title = "Quantum Heist",
        genre = "Sci-Fi",
        durationMinutes = 140,
        rating = "15",
        language = "English",
        cinema = "Screen 6 · IMAX",
        showTimes = emptyList(),
        isTrending = true,
        isComingSoon = true
    )
)

// ------------ PREVIEW ------------

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    FlickTixTheme {
        HomeScreen(
            onLogoutClick = {}
        )
    }
}
