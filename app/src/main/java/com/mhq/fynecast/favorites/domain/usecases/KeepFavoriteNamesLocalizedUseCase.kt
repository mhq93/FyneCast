package com.mhq.fynecast.favorites.domain.usecases

import com.mhq.fynecast.core.domain.infra.AppLanguageProvider
import com.mhq.fynecast.core.domain.infra.CurrentUserProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

/**
 * Keeps the saved favorite names in the app language without any screen being involved.
 *
 * It rewrites them whenever the app language changes while someone is signed in,
 * and when a user signs in (their favorites may have been saved in another language).
 * Nothing runs at app start: the names are already in the language they were saved in.
 */
class KeepFavoriteNamesLocalizedUseCase(
    private val appLanguageProvider: AppLanguageProvider,
    private val currentUserProvider: CurrentUserProvider,
    private val relocalizeFavoriteNames: RelocalizeFavoriteNamesUseCase
) {
    /** Never returns on its own: it runs for as long as the scope that calls it lives. */
    suspend operator fun invoke() {
        combine(
            appLanguageProvider.observeLanguageCode(),
            currentUserProvider.observeUserId()
        ) { lang, userId -> lang to userId }
            .distinctUntilChanged()
            .drop(1) // the state the app started in
            .collectLatest { (lang, userId) ->
                if (userId == null) return@collectLatest
                try {
                    relocalizeFavoriteNames(lang)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // The old names stay; the next language change or sign-in tries again.
                }
            }
    }
}
