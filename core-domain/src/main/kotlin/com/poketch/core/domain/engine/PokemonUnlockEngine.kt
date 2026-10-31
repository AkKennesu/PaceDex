package com.poketch.core.domain.engine

import com.poketch.core.domain.model.Pokemon

object PokemonUnlockEngine {

    fun evaluate(
        catalog: List<Pokemon>,
        context: UnlockTriggerContext,
        nowMillis: Long
    ): UnlockEvaluationResult {
        val workingMap = catalog.associateBy { it.pokedexNumber }.toMutableMap()
        val newlyUnlockedList = mutableListOf<UnlockEvent.NewPokemonUnlocked>()
        val progressList = mutableListOf<UnlockEvent.ProgressAdvanced>()

        fun updateProgressAndUnlock(pokedexNumber: Int, calculatedProgress: Int, reason: String) {
            val current = workingMap[pokedexNumber] ?: return
            val targetThreshold = current.unlockRule.targetThreshold
            val newProgress = maxOf(current.currentProgress, calculatedProgress)

            if (newProgress > current.currentProgress && !current.isUnlocked) {
                progressList.add(
                    UnlockEvent.ProgressAdvanced(
                        pokedexNumber = pokedexNumber,
                        previousProgress = current.currentProgress,
                        newProgress = newProgress,
                        targetThreshold = targetThreshold
                    )
                )
            }

            val shouldUnlock = !current.isUnlocked && newProgress >= targetThreshold
            if (shouldUnlock) {
                val unlocked = current.copy(
                    currentProgress = newProgress,
                    isUnlocked = true,
                    unlockedAtMillis = nowMillis
                )
                workingMap[pokedexNumber] = unlocked
                newlyUnlockedList.add(
                    UnlockEvent.NewPokemonUnlocked(
                        pokemon = unlocked,
                        triggerReason = reason,
                        timestampMillis = nowMillis
                    )
                )
            } else if (newProgress > current.currentProgress) {
                workingMap[pokedexNumber] = current.copy(currentProgress = newProgress)
            }
        }

        // 1. Starters (#001 - #009 and #133 Eevee)
        context.starterChoiceId?.let { starterId ->
            if (starterId in listOf(1, 4, 7)) {
                updateProgressAndUnlock(starterId, 1, "Starter chosen during onboarding")
            }
        }
        // Stage 2 & 3 evolutions of starters
        val chosenStarter = context.starterChoiceId
        if (chosenStarter != null) {
            val stage2Id = chosenStarter + 1
            val stage3Id = chosenStarter + 2

            if (context.currentLoggingStreakDays >= 7) {
                updateProgressAndUnlock(stage2Id, context.currentLoggingStreakDays, "7-day logging streak reached")
            } else {
                updateProgressAndUnlock(stage2Id, context.currentLoggingStreakDays, "Streak progression")
            }

            if (context.currentLoggingStreakDays >= 21) {
                updateProgressAndUnlock(stage3Id, context.currentLoggingStreakDays, "21-day logging streak reached")
            } else {
                updateProgressAndUnlock(stage3Id, context.currentLoggingStreakDays, "Streak progression")
            }
        }

        // Unlocking other starters at 30 & 60 days
        if (context.currentLoggingStreakDays >= 30) {
            updateProgressAndUnlock(1, 1, "30-day logging streak bonus starter")
            updateProgressAndUnlock(4, 1, "30-day logging streak bonus starter")
            updateProgressAndUnlock(7, 1, "30-day logging streak bonus starter")
        }
        if (context.currentLoggingStreakDays >= 60) {
            listOf(2, 5, 8).forEach { updateProgressAndUnlock(it, 7, "60-day milestone starter evolution") }
            listOf(3, 6, 9).forEach { updateProgressAndUnlock(it, 21, "60-day milestone starter evolution") }
        }

        // #133 Eevee (Profile complete / Onboarding)
        updateProgressAndUnlock(133, 1, "Completed user fitness profile")

        // 2. Hydration Rules
        if (context.todayWaterMl >= context.dailyWaterGoalMl && context.dailyWaterGoalMl > 0) {
            updateProgressAndUnlock(54, 1, "Reached 100% daily hydration goal") // #054 Psyduck
        }
        if (context.todayWaterMl >= 2000) {
            updateProgressAndUnlock(86, context.todayWaterMl, "Logged 2,000ml water in a day") // #086 Seel
        }
        if (context.todayWaterMl >= 3000) {
            updateProgressAndUnlock(99, context.todayWaterMl, "Logged 3,000ml water in an active day") // #099 Kingler
        }
        if (context.todayWaterMl > 0) {
            updateProgressAndUnlock(118, 1, "Logged first glass of water") // #118 Goldeen
            updateProgressAndUnlock(60, minOf(4, context.todayWaterMl / 250), "Logged water progress") // #060 Poliwag
        }

        // Hydration streaks
        if (context.waterGoalStreakDays > 0) {
            updateProgressAndUnlock(55, context.waterGoalStreakDays, "5-day hydration streak") // #055 Golduck
            updateProgressAndUnlock(61, context.waterGoalStreakDays, "7-day hydration streak") // #061 Poliwhirl
            updateProgressAndUnlock(121, context.waterGoalStreakDays, "7-day perfect hydration week") // #121 Starmie
            updateProgressAndUnlock(129, context.waterGoalStreakDays, "7-day hydration streak") // #129 Magikarp
            updateProgressAndUnlock(70, context.waterGoalStreakDays, "10-day hydration streak") // #070 Weepinbell
            updateProgressAndUnlock(119, context.waterGoalStreakDays, "12-day hydration streak") // #119 Seaking
            updateProgressAndUnlock(87, context.waterGoalStreakDays, "14-day hydration streak") // #087 Dewgong
            updateProgressAndUnlock(91, context.waterGoalStreakDays, "21-day hydration streak") // #091 Cloyster
            updateProgressAndUnlock(134, context.waterGoalStreakDays, "25-day hydration streak") // #134 Vaporeon
            updateProgressAndUnlock(130, context.waterGoalStreakDays, "30-day hydration streak") // #130 Gyarados
            updateProgressAndUnlock(144, context.waterGoalStreakDays, "30-day legendary hydration master") // #144 Articuno
        }

        // Lifetime water (in ml)
        val lifetimeMl = (context.lifetimeWaterLiters * 1000.0).toInt()
        if (lifetimeMl > 0) {
            updateProgressAndUnlock(73, lifetimeMl, "Logged 25 cumulative liters") // #073 Tentacruel
            updateProgressAndUnlock(117, lifetimeMl, "Logged 50 cumulative liters") // #117 Seadra
            updateProgressAndUnlock(131, lifetimeMl, "Logged 100 cumulative liters") // #131 Lapras
        }

        // 3. Movement, Steps & Running
        if (context.todaySteps > 0) {
            updateProgressAndUnlock(16, context.todaySteps, "Hit 5,000 steps today") // #016 Pidgey
            updateProgressAndUnlock(17, context.todaySteps, "Hit 10,000 steps today") // #017 Pidgeotto
            updateProgressAndUnlock(18, context.todaySteps, "Hit 15,000 steps today") // #018 Pidgeot
            updateProgressAndUnlock(95, context.todaySteps, "Hit 20,000 steps today") // #095 Onix
        }

        // 1-Mile & Distance Runs
        if (context.latestRunDistanceMiles >= 1.0) {
            updateProgressAndUnlock(77, 1, "Completed first 1-mile run") // #077 Ponyta
        }
        if (context.latestRunDistanceMiles >= 0.5) {
            updateProgressAndUnlock(58, 1, "Completed 0.5-mile run") // #058 Growlithe
        }
        if (context.latestRunDistanceMiles >= 2.0) {
            updateProgressAndUnlock(128, 2, "Completed continuous 2-mile run") // #128 Tauros
        }
        if (context.latestRunDistanceMiles >= 3.0) {
            updateProgressAndUnlock(125, 3, "Completed 3-mile distance run") // #125 Electabuzz
        }

        // Cumulative runs
        val cumulativeMilesInt = context.cumulativeMilesRun.toInt()
        if (cumulativeMilesInt > 0) {
            updateProgressAndUnlock(84, cumulativeMilesInt, "Completed 2 cumulative miles") // #084 Doduo
            updateProgressAndUnlock(78, cumulativeMilesInt, "Completed 5 cumulative miles") // #078 Rapidash
            updateProgressAndUnlock(85, cumulativeMilesInt, "Completed 10 cumulative miles") // #085 Dodrio
            updateProgressAndUnlock(135, cumulativeMilesInt, "Completed 15 cumulative miles") // #135 Jolteon
            updateProgressAndUnlock(149, cumulativeMilesInt, "Completed Marathon distance (26.2 mi)") // #149 Dragonite
            updateProgressAndUnlock(145, cumulativeMilesInt, "50 cumulative miles legendary stamina") // #145 Zapdos
        }

        // Workouts
        if (context.totalCompletedWorkouts > 0) {
            updateProgressAndUnlock(66, context.totalCompletedWorkouts, "Logged first workout") // #066 Machop
            updateProgressAndUnlock(67, context.totalCompletedWorkouts, "Logged 5 workouts") // #067 Machoke
            updateProgressAndUnlock(106, context.totalCompletedWorkouts, "Completed 10 cardio workouts") // #106 Hitmonlee
            updateProgressAndUnlock(68, context.totalCompletedWorkouts, "Completed 15 workouts") // #068 Machamp
        }

        // 4. Calorie Deficit & Discipline
        if (context.todayMealsLoggedCount >= 3) {
            updateProgressAndUnlock(113, minOf(3, context.todayMealsLoggedCount), "Logged all 3 meals today") // #113 Chansey
            updateProgressAndUnlock(27, 1, "Logged all meals in advance") // #027 Sandshrew
        }

        if (context.isWithinCalorieTargetToday && context.calorieDeficitStreakDays > 0) {
            updateProgressAndUnlock(33, context.calorieDeficitStreakDays, "5 days within calorie target") // #033 Nidorino
            updateProgressAndUnlock(63, context.calorieDeficitStreakDays, "7 days within calorie target") // #063 Abra
            updateProgressAndUnlock(47, context.calorieDeficitStreakDays, "7 days within calorie target") // #047 Parasect
            updateProgressAndUnlock(34, context.calorieDeficitStreakDays, "10 days within calorie target") // #034 Nidoking
            updateProgressAndUnlock(38, context.calorieDeficitStreakDays, "14 days within calorie target") // #038 Ninetales
            updateProgressAndUnlock(64, context.calorieDeficitStreakDays, "14 days within calorie target") // #064 Kadabra
            updateProgressAndUnlock(76, context.calorieDeficitStreakDays, "14 days within calorie target") // #076 Golem
            updateProgressAndUnlock(65, context.calorieDeficitStreakDays, "30 days within calorie target") // #065 Alakazam
            updateProgressAndUnlock(146, context.calorieDeficitStreakDays, "30 days legendary calorie deficit master") // #146 Moltres
        }

        // Recovery after surplus
        if (context.recoveryDaysAfterSurplus >= 1) updateProgressAndUnlock(104, 1, "Recovered from calorie surplus") // #104 Cubone
        if (context.recoveryDaysAfterSurplus >= 3) updateProgressAndUnlock(143, 3, "Recovered 3 consecutive days after surplus") // #143 Snorlax

        // 5. Sleep & Recovery
        if (context.sleepStreakDays > 0) {
            updateProgressAndUnlock(10, context.sleepStreakDays, "Logged first night of sleep") // #010 Caterpie
            updateProgressAndUnlock(12, context.sleepStreakDays, "3 days of quality sleep") // #012 Butterfree
            updateProgressAndUnlock(15, context.sleepStreakDays, "7 consecutive nights of quality sleep") // #015 Beedrill
            updateProgressAndUnlock(39, context.sleepStreakDays, "7 nights of 7+ hours sleep") // #039 Jigglypuff
            updateProgressAndUnlock(40, context.sleepStreakDays, "21 nights of 7+ hours sleep") // #040 Wigglytuff
        }

        // 6. Mythicals & Legendaries (#150 Mewtwo and #151 Mew)
        // Mewtwo: unlocked when 149 Pokémon are unlocked
        val currentUnlockedCount = workingMap.values.count { it.isUnlocked }
        updateProgressAndUnlock(150, currentUnlockedCount, "Unlocked 149 Pokémon in Pokédex")

        // Mew: unlocked when all #001 to #150 are unlocked
        val first150UnlockedCount = workingMap.values.filter { it.pokedexNumber in 1..150 }.count { it.isUnlocked }
        updateProgressAndUnlock(151, first150UnlockedCount, "Completed Kanto Pokédex #001 to #150")

        val sortedUpdatedCatalog = workingMap.values.sortedBy { it.pokedexNumber }
        return UnlockEvaluationResult(
            updatedCatalog = sortedUpdatedCatalog,
            newlyUnlockedEvents = newlyUnlockedList,
            progressEvents = progressList
        )
    }
}
