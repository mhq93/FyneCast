# Files under `app/src/main/java/com/mhq/fynecast/favorites`

## data\database\FavoriteDao.kt

```kotlin
package com.mhq.fynecast.favorites.data.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FavoriteDao {

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND locationKey = :key)")
    abstract fun observeIsFavorite(userId: String, key: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND locationKey = :key)")
    abstract suspend fun isFavorite(userId: String, key: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertFavorite(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND locationKey = :key")
    abstract suspend fun deleteFavorite(userId: String, key: String)

    @Query(
        """UPDATE favorites SET temperatureC = :tempC, temperatureF = :tempF, weatherIconUrl = :icon
              WHERE userId = :userId AND locationKey = :key"""
    )
    abstract suspend fun updateSnapshot(
        userId: String,
        key: String,
        tempC: Double,
        tempF: Double,
        icon: String
    )

    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY cityName ASC, countryName ASC")
    abstract fun getAllFavorites(userId: String): PagingSource<Int, FavoriteEntity>

    @Transaction
    open suspend fun toggle(entity: FavoriteEntity) {
        if (isFavorite(entity.userId, entity.locationKey))
            deleteFavorite(entity.userId, entity.locationKey)
        else
            insertFavorite(entity)
    }
}
```

## data\database\FavoriteEntity.kt

```kotlin
package com.mhq.fynecast.favorites.data.database

import androidx.room.Entity

@Entity(
    tableName = "favorites",
    primaryKeys = ["userId", "locationKey"])
data class FavoriteEntity(
    val userId: String,
    val locationKey: String,
    val cityName: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double,
    val temperatureC: Double,
    val temperatureF: Double,
    val weatherIconUrl: String
)
```

## data\database\FavoritesDatabase.kt

```kotlin
package com.mhq.fynecast.favorites.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FavoriteEntity::class], version = 1)
abstract class FavoritesDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
}
```

## data\mapper\FavoritesMapper.kt

```kotlin
package com.mhq.fynecast.favorites.data.mapper

import com.mhq.fynecast.favorites.data.database.FavoriteEntity
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel

fun FavoriteEntity.toDomain() = FavoriteCityDomainModel(
    cityName = cityName,
    countryName = countryName,
    latitude = latitude,
    longitude = longitude,
    temperatureC = temperatureC,
    temperatureF = temperatureF,
    weatherIconUrl = weatherIconUrl
)

fun FavoriteCityDomainModel.toEntity(userId: String) = FavoriteEntity(
    userId = userId,
    locationKey = id,
    cityName = cityName,
    countryName = countryName,
    latitude = latitude,
    longitude = longitude,
    temperatureC = temperatureC,
    temperatureF = temperatureF,
    weatherIconUrl = weatherIconUrl
)
```

## data\repoimpl\FavoritesRepoImpl.kt

```kotlin
package com.mhq.fynecast.favorites.data.repoimpl

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.mhq.fynecast.core.domain.infra.CurrentUserProvider
import com.mhq.fynecast.favorites.data.database.FavoriteDao
import com.mhq.fynecast.favorites.data.mapper.toDomain
import com.mhq.fynecast.favorites.data.mapper.toEntity
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesRepoImpl(
    private val favoriteDao: FavoriteDao,
    private val currentUserProvider: CurrentUserProvider
) : FavoritesRepository {

    override fun observeIsFavorite(id: String): Flow<Boolean> =
        currentUserProvider.observeUserId().flatMapLatest { userId ->
            if (userId == null)
                flowOf(false)
            else
                favoriteDao.observeIsFavorite(userId, id)
        }

    override suspend fun toggleFavorite(city: FavoriteCityDomainModel) {
        val userId = currentUserProvider.currentUserId() ?: return
        favoriteDao.toggle(city.toEntity(userId))
    }

    override suspend fun saveFavorite(city: FavoriteCityDomainModel) {
        val userId = currentUserProvider.currentUserId() ?: return
        favoriteDao.insertFavorite(city.toEntity(userId))
    }

    override suspend fun deleteFavorite(id: String) {
        val userId = currentUserProvider.currentUserId() ?: return
        favoriteDao.deleteFavorite(userId, id)
    }

    override suspend fun refreshSnapshot(city: FavoriteCityDomainModel) {
        val userId = currentUserProvider.currentUserId() ?: return
        favoriteDao.updateSnapshot(
            userId,
            city.id,
            city.temperatureC,
            city.temperatureF,
            city.weatherIconUrl
        )
    }

    override fun getAllFavorites(): Flow<PagingData<FavoriteCityDomainModel>> =
        currentUserProvider.observeUserId().flatMapLatest { userId ->
            if (userId == null) {
                flowOf(PagingData.empty())
            } else {
                Pager(
                    config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                    pagingSourceFactory = { favoriteDao.getAllFavorites(userId) }
                ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
            }
        }
}
```

## domain\mapper\FavoritesMapper.kt

```kotlin
package com.mhq.fynecast.favorites.domain.mapper

import com.mhq.fynecast.favorites.data.database.FavoriteEntity
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel

fun FavoriteCityDomainModel.toEntity(userId: String) = FavoriteEntity(
    userId = userId,
    locationKey = id,
    cityName = cityName,
    countryName = countryName,
    latitude = latitude,
    longitude = longitude,
    temperatureC = temperatureC,
    temperatureF = temperatureF,
    weatherIconUrl = weatherIconUrl
)
```

## domain\models\FavoriteCityDomainModel.kt

```kotlin
package com.mhq.fynecast.favorites.domain.models

import kotlin.math.roundToLong

data class FavoriteCityDomainModel(
    val cityName: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double,
    val temperatureC: Double,
    val temperatureF: Double,
    val weatherIconUrl: String
) {
    /** The only identity of a favorite:
     * its location, rounded to about 1 km. */
    val id: String get() = favoriteId(latitude, longitude)
}

fun favoriteId(latitude: Double, longitude: Double): String =
    "${(latitude * 100).roundToLong() / 100.0},${(longitude * 100).roundToLong() / 100.0}"
```

## domain\repository\FavoritesRepository.kt

```kotlin
package com.mhq.fynecast.favorites.domain.repository

import androidx.paging.PagingData
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun getAllFavorites(): Flow<PagingData<FavoriteCityDomainModel>>
    fun observeIsFavorite(id: String): Flow<Boolean>
    /** Adds the city if it isn't a favorite and removes it if it is, as one atomic step. */
    suspend fun toggleFavorite(city: FavoriteCityDomainModel)
    suspend fun saveFavorite(city: FavoriteCityDomainModel)            // used to undo a removal
    suspend fun deleteFavorite(id: String)
    suspend fun refreshSnapshot(city: FavoriteCityDomainModel)         // updates temperature and icon
}
```

## domain\usecases\DeleteFavoriteUseCase.kt

```kotlin
package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository

class DeleteFavoriteUseCase(private val favoritesRepository: FavoritesRepository) {
    suspend operator fun invoke(id: String) = favoritesRepository.deleteFavorite(id)
}
```

## domain\usecases\GetFavoritesUseCase.kt

```kotlin
package com.mhq.fynecast.favorites.domain.usecases

import androidx.paging.PagingData
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow

class GetFavoritesUseCase(private val favoritesRepository: FavoritesRepository) {
    operator fun invoke(): Flow<PagingData<FavoriteCityDomainModel>> {
        return favoritesRepository.getAllFavorites()
    }
}
```

## domain\usecases\RefreshFavoriteSnapshotUseCase.kt

```kotlin
package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository

class RefreshFavoriteSnapshotUseCase(private val favoritesRepository: FavoritesRepository) {
    suspend operator fun invoke(city: FavoriteCityDomainModel) =
        favoritesRepository.refreshSnapshot(city)
}
```

## domain\usecases\RestoreFavoriteUseCase.kt

```kotlin
package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository

class RestoreFavoriteUseCase(private val favoritesRepository: FavoritesRepository) {
    suspend operator fun invoke(city: FavoriteCityDomainModel) =
        favoritesRepository.saveFavorite(city)
}
```

## domain\usecases\ToggleFavoriteUseCase.kt

```kotlin
package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.repository.FavoritesRepository

class ToggleFavoriteUseCase(private val favoritesRepository: FavoritesRepository) {
    suspend operator fun invoke(city: FavoriteCityDomainModel) =
        favoritesRepository.toggleFavorite(city)
}
```

## ui\components\FavoriteCityItem.kt

```kotlin
package com.mhq.fynecast.favorites.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.GlassyCard
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel

@Composable
fun FavoriteCityItem(
    favoriteCityDomainModel: FavoriteCityDomainModel,
    isMetric: Boolean,
    modifier: Modifier = Modifier
) {
    GlassyCard(
        modifier = modifier.padding(horizontal = 8.dp),
        containerAlpha = 0.12f,
        borderAlpha = 0.3f
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left — City info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = favoriteCityDomainModel.cityName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Cursive,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = favoriteCityDomainModel.countryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right — Weather icon + temp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AsyncImage(
                    model = "https:${favoriteCityDomainModel.weatherIconUrl}",
                    placeholder = painterResource(R.drawable.ic_launcher_foreground),
                    error = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.weather_icon),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text =
                        if (isMetric)
                            "${favoriteCityDomainModel.temperatureC}°"
                        else
                            "${favoriteCityDomainModel.temperatureF}°",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFE0F7FA
)

@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)

@Preview
@Composable
private fun FavoriteCityItemPreview() {
    FyneCastTheme() {
        FavoriteCityItem(
            FavoriteCityDomainModel(
                cityName = "Alexandria",
                countryName = "Egypt",
                latitude = 100.0,
                longitude = 100.0,
                temperatureC = 100.0,
                temperatureF = 100.0,
                weatherIconUrl = "Url"
            ),
            isMetric = true
        )
    }
}
```

## ui\components\SwipeBackground.kt

```kotlin
package com.mhq.fynecast.favorites.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeBackground(dismissState: SwipeToDismissBoxState) {
    val color = when (dismissState.dismissDirection) {
        SwipeToDismissBoxValue.EndToStart -> Color.Red.copy(alpha = 0.8f)
        else -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = Color.White
            )
        }
    }
}
```

## ui\components\UndoDeleteSnackbar.kt

```kotlin
package com.mhq.fynecast.favorites.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.core.ui.theme.ElectricLimeGreen

@Composable
fun UndoDeleteSnackbar(
    snackbarData: SnackbarData?
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .padding(horizontal = 36.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            // Display message
            snackbarData?.visuals?.message?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }

            // Display "Undo" Action Button if present
            snackbarData?.visuals?.actionLabel?.let { label ->
                Text(
                    text = label.uppercase(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { snackbarData.performAction() }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UndoDeleteSnackbarPreview() {
    FyneCastTheme() {
        UndoDeleteSnackbar(snackbarData = null)
    }
}
```

## ui\FavoritesEvent.kt

```kotlin
package com.mhq.fynecast.favorites.ui

sealed interface FavoritesEvent {
    data class Removed(val cityName: String) : FavoritesEvent
    data class RemoveFailed(val cityName: String) : FavoritesEvent
}
```

## ui\FavoritesViewModel.kt

```kotlin
package com.mhq.fynecast.favorites.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.mhq.fynecast.core.di.FyneCastApplication
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.domain.usecases.DeleteFavoriteUseCase
import com.mhq.fynecast.favorites.domain.usecases.GetFavoritesUseCase
import com.mhq.fynecast.favorites.domain.usecases.RestoreFavoriteUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesViewModel(
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val deleteFavoriteUseCase: DeleteFavoriteUseCase,
    private val restoreFavoriteUseCase: RestoreFavoriteUseCase
) : ViewModel() {

    private val _uiEventChannel = Channel<String>(Channel.BUFFERED)
    val uiEvent = _uiEventChannel.receiveAsFlow()

    val favoritesPageFlow: Flow<PagingData<FavoriteCityDomainModel>> =
        getFavoritesUseCase().cachedIn(viewModelScope)

    private val _events = Channel<FavoritesEvent>(Channel.BUFFERED)
    val events: Flow<FavoritesEvent> = _events.receiveAsFlow()

    private var lastRemoved: FavoriteCityDomainModel? = null

    fun onDeleteFavorite(city: FavoriteCityDomainModel) {
        viewModelScope.launch {
            try {
                // One quick DB call; it must finish even if the screen is closed right after.
                withContext(NonCancellable) { deleteFavoriteUseCase(city.id) }
                lastRemoved = city
                _events.send(FavoritesEvent.Removed(city.cityName))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(FavoritesEvent.RemoveFailed(city.cityName))
            }
        }
    }

    fun onUndoRemove() {
        val city = lastRemoved ?: return
        lastRemoved = null
        viewModelScope.launch { withContext(NonCancellable) { restoreFavoriteUseCase(city) } }
    }

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FyneCastApplication)

                FavoritesViewModel(
                    getFavoritesUseCase = app.container.getFavoritesUseCase,
                    deleteFavoriteUseCase = app.container.deleteFavoriteUseCase,
                    restoreFavoriteUseCase = app.container.restoreFavoriteUseCase
                )
            }
        }
    }
}
```

## ui\screens\FavoritesBlank.kt

```kotlin
package com.mhq.fynecast.favorites.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.globalcomponents.BlankPagePlaceholder
import com.mhq.fynecast.core.ui.theme.FyneCastTheme

@Composable
fun FavoritesBlank(
    contentPadding: PaddingValues = PaddingValues(8.dp),
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(contentPadding)
            .fillMaxSize()
    ) {
        BlankPagePlaceholder(
            blankSectionIcon = Icons.Default.FavoriteBorder,
            blankSectionIconDescription = null,
            blankSectionTitle = stringResource(R.string.no_favorites_yet),
            blankSectionSubtitle = stringResource(R.string.cities_you_like_will_appear_here)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FavoritesBlankPreview() {
    FyneCastTheme() {
        FavoritesBlank()
    }
}
```

## ui\screens\FavoritesContainer.kt

```kotlin
package com.mhq.fynecast.favorites.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.mhq.fynecast.R
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.ui.FavoritesEvent
import com.mhq.fynecast.favorites.ui.FavoritesViewModel
import com.mhq.fynecast.favorites.ui.components.UndoDeleteSnackbar
import kotlinx.coroutines.launch

@Composable
fun FavoritesContainer(
    favoritesViewModel: FavoritesViewModel = viewModel(factory = FavoritesViewModel.factory),
    onCityRowSelected: (FavoriteCityDomainModel) -> Unit,
    isMetric: Boolean,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    val resources = LocalResources.current
    val undoLabel = stringResource(R.string.undo)
    val lazyFavoriteCities = favoritesViewModel.favoritesPageFlow.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val cityRemovedTemplate = stringResource(R.string.city_removed)

    LaunchedEffect(Unit) {
        favoritesViewModel.uiEvent.collect { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    LaunchedEffect(favoritesViewModel, resources) {
        favoritesViewModel.events.collect { event ->
            launch {   // each event gets its own coroutine so a new message replaces the old one
                snackbarHostState.currentSnackbarData?.dismiss()
                when (event) {
                    is FavoritesEvent.Removed -> {
                        val result = snackbarHostState.showSnackbar(
                            message = resources.getString(R.string.city_removed, event.cityName),
                            actionLabel = undoLabel,
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) favoritesViewModel.onUndoRemove()
                    }
                    is FavoritesEvent.RemoveFailed ->
                        snackbarHostState.showSnackbar(
                            message = resources.getString(R.string.city_remove_failed, event.cityName),
                            duration = SnackbarDuration.Short
                        )
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            lazyFavoriteCities.loadState.refresh is LoadState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            lazyFavoriteCities.itemCount == 0 -> {
                FavoritesBlank(
                    contentPadding = contentPadding
                )
            }

            else -> {
                FavoritesContent(
                    favoriteCities = lazyFavoriteCities,
                    onCityRowSelected = onCityRowSelected,
                    onDeleteFavorite = favoritesViewModel::onDeleteFavorite,
                    contentPadding = contentPadding,
                    isMetric = isMetric
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 160.dp)
        ) { data -> UndoDeleteSnackbar(snackbarData = data) }
    }
}
```

## ui\screens\FavoritesContent.kt

```kotlin
package com.mhq.fynecast.favorites.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.mhq.fynecast.core.ui.theme.FyneCastTheme
import com.mhq.fynecast.favorites.domain.models.FavoriteCityDomainModel
import com.mhq.fynecast.favorites.ui.components.FavoriteCityItem
import com.mhq.fynecast.favorites.ui.components.SwipeBackground
import kotlinx.coroutines.flow.flowOf

@Composable
fun FavoritesContent(
    favoriteCities: LazyPagingItems<FavoriteCityDomainModel>,
    onCityRowSelected: (FavoriteCityDomainModel) -> Unit,
    onDeleteFavorite: (FavoriteCityDomainModel) -> Unit,
    isMetric: Boolean,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(vertical = 48.dp, horizontal = 24.dp)
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                count = favoriteCities.itemCount,
                key = favoriteCities.itemKey { it.id }
            ) { index ->
                val favorite = favoriteCities[index]
                if (favorite != null) {
                    val currentFavorite by rememberUpdatedState(favorite)
                    val currentOnDelete by rememberUpdatedState(onDeleteFavorite)
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value -> value == SwipeToDismissBoxValue.EndToStart }
                    )

                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            currentOnDelete(currentFavorite)
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = { SwipeBackground(dismissState) },
                        enableDismissFromStartToEnd = false
                    ) {
                        FavoriteCityItem(
                            favoriteCityDomainModel = favorite,
                            isMetric = isMetric,
                            modifier = Modifier.clickable { onCityRowSelected(favorite) }
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun FavoritesContentPreview() {
    FyneCastTheme() {
        val mockFavoriteCities = flowOf(
            PagingData.empty<FavoriteCityDomainModel>()
        ).collectAsLazyPagingItems()

        FavoritesContent(
            favoriteCities = mockFavoriteCities,
            onCityRowSelected = {},
            onDeleteFavorite = {},
            isMetric = true
        )
    }
}
```

