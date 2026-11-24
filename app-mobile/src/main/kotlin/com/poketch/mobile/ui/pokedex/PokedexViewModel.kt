package com.poketch.mobile.ui.pokedex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poketch.core.domain.model.Pokemon
import com.poketch.core.domain.model.PokemonCategory
import com.poketch.core.domain.repository.PokemonRepository
import com.poketch.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PokedexFilter(val label: String) {
    ALL("All (151)"),
    UNLOCKED("Unlocked"),
    LOCKED("Locked"),
    STARTERS("Starters"),
    HYDRATION("Hydration"),
    RUNNING_STEPS("Running & Steps"),
    CALORIE_DEFICIT("Calorie Deficit"),
    SLEEP("Sleep"),
    LEGENDARIES("Legendaries"),
}

data class PokedexUiState(
    val allPokemon: List<Pokemon> = emptyList(),
    val filteredPokemon: ImmutableList<Pokemon> = persistentListOf(),
    val unlockedCount: Int = 0,
    val totalCount: Int = 151,
    val activeBuddyId: Int = 1,
    val selectedFilter: PokedexFilter = PokedexFilter.ALL,
    val selectedPokemon: Pokemon? = null,
    val isLoading: Boolean = false,
)

@HiltViewModel
class PokedexViewModel @Inject constructor(
    pokemonRepository: PokemonRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(PokedexFilter.ALL)
    private val _selectedPokemon = MutableStateFlow<Pokemon?>(null)

    private val allPokemonFlow = pokemonRepository.observeAllPokemon()
    private val userProfileFlow = userRepository.observeUserProfile()

    val uiState: StateFlow<PokedexUiState> = combine(
        allPokemonFlow,
        userProfileFlow,
        _selectedFilter,
        _selectedPokemon,
    ) { catalog, userProfile, filter, selectedPokemon ->
        val sortedCatalog = catalog.sortedBy { it.pokedexNumber }
        val unlockedCount = sortedCatalog.count { it.isUnlocked }
        val activeBuddyId = userProfile?.activeBuddyPokemonId ?: 1

        val filtered = when (filter) {
            PokedexFilter.ALL -> sortedCatalog
            PokedexFilter.UNLOCKED -> sortedCatalog.filter { it.isUnlocked }
            PokedexFilter.LOCKED -> sortedCatalog.filter { !it.isUnlocked }
            PokedexFilter.STARTERS -> sortedCatalog.filter { it.unlockRule.category == PokemonCategory.STARTER }
            PokedexFilter.HYDRATION -> sortedCatalog.filter { it.unlockRule.category == PokemonCategory.WATER_HYDRATION }
            PokedexFilter.RUNNING_STEPS -> sortedCatalog.filter { it.unlockRule.category == PokemonCategory.RUN_STEPS }
            PokedexFilter.CALORIE_DEFICIT -> sortedCatalog.filter { it.unlockRule.category == PokemonCategory.CALORIE_DEFICIT }
            PokedexFilter.SLEEP -> sortedCatalog.filter { it.unlockRule.category == PokemonCategory.SLEEP_STREAK }
            PokedexFilter.LEGENDARIES -> sortedCatalog.filter { it.unlockRule.category == PokemonCategory.LEGENDARY }
        }

        // If the selected pokemon is currently open in the bottom sheet, refresh its state from catalog
        val updatedSelected = selectedPokemon?.let { sel ->
            sortedCatalog.find { it.pokedexNumber == sel.pokedexNumber } ?: sel
        }

        PokedexUiState(
            allPokemon = sortedCatalog,
            filteredPokemon = filtered.toImmutableList(),
            unlockedCount = unlockedCount,
            totalCount = 151,
            activeBuddyId = activeBuddyId,
            selectedFilter = filter,
            selectedPokemon = updatedSelected,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PokedexUiState(isLoading = true),
    )

    fun onFilterSelected(filter: PokedexFilter) {
        _selectedFilter.value = filter
    }

    fun onPokemonClicked(pokemon: Pokemon) {
        _selectedPokemon.value = pokemon
    }

    fun onDismissBottomSheet() {
        _selectedPokemon.value = null
    }

    fun setActiveBuddy(pokemonId: Int) {
        viewModelScope.launch {
            userRepository.updateActiveBuddy(pokemonId)
        }
    }
}
