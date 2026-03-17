package com.frontend.ui.screen.dog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.data.local.TokenDataStore
import com.frontend.data.repository.DogRepository
import com.frontend.domain.model.UpdateDogRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DogEditState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val dogId: Long = 0L,
    val name: String = "",
    val breed: String = "",
    val birthYear: String = "",
    val birthMonth: String = "",
    val birthDay: String = "",
    val gender: String = "MALE",
    val weight: String = "",
    val selectedTraits: List<String> = emptyList()
)

@HiltViewModel
class DogEditViewModel @Inject constructor(
    private val dogRepository: DogRepository,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(DogEditState())
    val state: StateFlow<DogEditState> = _state.asStateFlow()

    init {
        loadDogProfile()
    }

    private fun loadDogProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val dogId = tokenDataStore.getDogId().first()
                    ?: throw Exception("반려견 정보가 없습니다. 다시 로그인해주세요.")
                val profile = dogRepository.getDogProfile(dogId)
                val parts = profile.birthDate.split("-")
                _state.update {
                    it.copy(
                        isLoading = false,
                        dogId = profile.dogId,
                        name = profile.name,
                        breed = profile.breed,
                        birthYear = parts.getOrElse(0) { "" },
                        birthMonth = parts.getOrElse(1) { "" },
                        birthDay = parts.getOrElse(2) { "" },
                        gender = profile.gender,
                        weight = if (profile.weight == profile.weight.toLong().toDouble())
                            profile.weight.toInt().toString()
                        else profile.weight.toString(),
                        selectedTraits = profile.traits.orEmpty()
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value) }
    fun onBreedChange(value: String) = _state.update { it.copy(breed = value) }
    fun onBirthYearChange(value: String) = _state.update { it.copy(birthYear = value) }
    fun onBirthMonthChange(value: String) = _state.update { it.copy(birthMonth = value) }
    fun onBirthDayChange(value: String) = _state.update { it.copy(birthDay = value) }
    fun onGenderChange(value: String) = _state.update { it.copy(gender = value) }
    fun onWeightChange(value: String) = _state.update { it.copy(weight = value) }
    fun onTraitToggle(trait: String) {
        _state.update { s ->
            val current = s.selectedTraits.toMutableList()
            if (current.contains(trait)) current.remove(trait) else current.add(trait)
            s.copy(selectedTraits = current)
        }
    }

    fun save() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            try {
                val birthDate = "${s.birthYear}-${s.birthMonth.padStart(2, '0')}-${s.birthDay.padStart(2, '0')}"
                dogRepository.updateDogProfile(
                    s.dogId,
                    UpdateDogRequest(
                        name = s.name,
                        breed = s.breed,
                        birthDate = birthDate,
                        gender = s.gender
                    )
                )
                val weightDouble = s.weight.toDoubleOrNull()
                if (weightDouble != null) {
                    dogRepository.updateWeight(s.dogId, weightDouble)
                }
                dogRepository.updateTraits(s.dogId, s.selectedTraits)
                _state.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.message ?: "저장에 실패했습니다") }
            }
        }
    }
}
