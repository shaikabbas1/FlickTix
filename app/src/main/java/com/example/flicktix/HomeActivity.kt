package com.example.flicktix

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

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlickTixTheme {
                HomeScreen()
            }
        }
    }
}

// ------------ DATA MODEL ------------

data class Movie(
    val id: Int,
    val title: String,
    val genre: String,
    val durationMinutes: Int,
    val rating: String,        // e.g. "PG-13"
    val language: String,      // e.g. "English"
    val cinema: String,        // e.g. "Screen 2 · City Mall"
    val showTimes: List<String>,
    val isTrending: Boolean,
    val isComingSoon: Boolean
)

// ------------ HOME SCREEN ------------

@Composable
fun HomeScreen() {
    val allMovies = remember { sampleMovies() }

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
        topBar = { HomeTopBar() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
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
                                // TODO: Navigate to booking / seat selection
                            },
                            onCardClick = {
                                // TODO: Navigate to movie details
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
                                // TODO: Maybe show "Notify me" or pre-book option
                            },
                            onCardClick = {
                                // TODO: Navigate to movie details
                            }
                        )
                    }
                }

                if (filteredNowShowing.isEmpty() && filteredComingSoon.isEmpty()) {
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
}

// ------------ TOP BAR ------------

@Composable
fun HomeTopBar() {
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
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
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

// ------------ SAMPLE DATA ------------

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
        HomeScreen()
    }
}
