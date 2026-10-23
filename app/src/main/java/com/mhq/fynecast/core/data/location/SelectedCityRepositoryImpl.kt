package com.mhq.fynecast.core.data.location

import com.mhq.fynecast.core.domain.location.SelectedCity
import com.mhq.fynecast.core.domain.location.SelectedCityRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class SelectedCityRepositoryImpl : SelectedCityRepository {
    private val channel = Channel<SelectedCity>(Channel.BUFFERED)
    override val selections: Flow<SelectedCity> = channel.receiveAsFlow()
    override suspend fun select(city: SelectedCity) = channel.send(city)
}