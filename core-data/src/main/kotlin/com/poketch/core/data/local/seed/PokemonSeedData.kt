package com.poketch.core.data.local.seed

import com.poketch.core.data.local.entity.PokemonEntity

object PokemonSeedData {
    val ALL_151: List<PokemonEntity> by lazy {
        listOf(
            // 1-9 Starters
            e(1, "Bulbasaur", "Grass", "Poison", "STARTER", "ONBOARDING_STARTER", "Choose as starter during onboarding", 1),
            e(2, "Ivysaur", "Grass", "Poison", "STARTER", "STREAK_7_DAYS", "Maintain a 7-day logging streak", 7),
            e(3, "Venusaur", "Grass", "Poison", "STARTER", "STREAK_21_DAYS", "Maintain a 21-day logging streak", 21),
            e(4, "Charmander", "Fire", null, "STARTER", "ONBOARDING_STARTER", "Choose as starter during onboarding", 1),
            e(5, "Charmeleon", "Fire", null, "STARTER", "STREAK_7_DAYS", "Maintain a 7-day logging streak", 7),
            e(6, "Charizard", "Fire", "Flying", "STARTER", "STREAK_21_DAYS", "Maintain a 21-day logging streak", 21),
            e(7, "Squirtle", "Water", null, "STARTER", "ONBOARDING_STARTER", "Choose as starter during onboarding", 1),
            e(8, "Wartortle", "Water", null, "STARTER", "STREAK_7_DAYS", "Maintain a 7-day logging streak", 7),
            e(9, "Blastoise", "Water", null, "STARTER", "STREAK_21_DAYS", "Maintain a 21-day logging streak", 21),

            // 10-15 Bug line (Sleep & Recovery)
            e(10, "Caterpie", "Bug", null, "SLEEP_STREAK", "SLEEP_FIRST_NIGHT", "Log your first sleep session", 1),
            e(11, "Metapod", "Bug", null, "SLEEP_STREAK", "SLEEP_REST_DAY", "Log a scheduled active recovery day", 1),
            e(12, "Butterfree", "Bug", "Flying", "SLEEP_STREAK", "SLEEP_7_HOURS_3_DAYS", "Achieve 7+ hours of sleep for 3 days", 3),
            e(13, "Weedle", "Bug", "Poison", "SLEEP_STREAK", "SLEEP_SCHEDULE_CONSISTENT", "Wake up at your scheduled time 3 days in a row", 3),
            e(14, "Kakuna", "Bug", "Poison", "SLEEP_STREAK", "SLEEP_RECOVERY_HOURS", "Log 8+ hours of recovery sleep", 1),
            e(15, "Beedrill", "Bug", "Poison", "SLEEP_STREAK", "SLEEP_7_DAYS_STREAK", "Log 7 consecutive nights of quality sleep", 7),

            // 16-24 Birds, Rodents, Snakes (Run & Movement)
            e(16, "Pidgey", "Normal", "Flying", "RUN_STEPS", "STEPS_5K_ONCE", "Reach 5,000 steps in a single day", 5000),
            e(17, "Pidgeotto", "Normal", "Flying", "RUN_STEPS", "STEPS_10K_ONCE", "Reach 10,000 steps in a single day", 10000),
            e(18, "Pidgeot", "Normal", "Flying", "RUN_STEPS", "STEPS_15K_ONCE", "Reach 15,000 steps in a single day", 15000),
            e(19, "Rattata", "Normal", null, "RUN_STEPS", "STEPS_QUICK_WALK", "Log a 15-minute brisk walk", 1),
            e(20, "Raticate", "Normal", null, "RUN_STEPS", "STEPS_5K_3_DAYS", "Hit 5,000 steps 3 days in a row", 3),
            e(21, "Spearow", "Normal", "Flying", "RUN_STEPS", "RUN_EARLY_BIRD", "Log a morning walk or jog before 9 AM", 1),
            e(22, "Fearow", "Normal", "Flying", "RUN_STEPS", "RUN_PACE_IMPROVE", "Log a 1-mile run with improved pace", 1),
            e(23, "Ekans", "Poison", null, "SLEEP_STREAK", "SLEEP_EVENING_STRETCH", "Complete an evening wind-down stretch", 1),
            e(24, "Arbok", "Poison", null, "SLEEP_STREAK", "SLEEP_NIGHT_CHECKIN_5", "Check in before bed 5 nights in a row", 5),

            // 25-28 Pikachu, Raichu, Sandshrew (Movement & Calorie)
            e(25, "Pikachu", "Electric", null, "RUN_STEPS", "REMINDER_MORNING_5", "Respond to 5 morning movement reminders on time", 5),
            e(26, "Raichu", "Electric", null, "RUN_STEPS", "REMINDER_STREAK_14", "Respond to activity reminders 14 days in a row", 14),
            e(27, "Sandshrew", "Ground", null, "CALORIE_DEFICIT", "CALORIE_MEAL_PREP", "Log all 3 daily meals in advance", 1),
            e(28, "Sandslash", "Ground", null, "CALORIE_DEFICIT", "CALORIE_FIBER_FOCUS", "Stay in deficit while hitting whole food targets 3 days", 3),

            // 29-38 Nido families, Clefairy, Vulpix (Sleep & Calorie)
            e(29, "Nidoran♀", "Poison", null, "SLEEP_STREAK", "SLEEP_HYDRATION_BALANCE", "Drink glass of water upon waking 3 days", 3),
            e(30, "Nidorina", "Poison", null, "SLEEP_STREAK", "SLEEP_7_HOURS_5_DAYS", "Log 7+ hours of sleep for 5 days", 5),
            e(31, "Nidoqueen", "Poison", "Ground", "SLEEP_STREAK", "SLEEP_CONSISTENT_WEEK", "Maintain a consistent sleep window for 7 days", 7),
            e(32, "Nidoran♂", "Poison", null, "CALORIE_DEFICIT", "CALORIE_SNACK_CONTROL", "Avoid late-night snacking 3 days in a row", 3),
            e(33, "Nidorino", "Poison", null, "CALORIE_DEFICIT", "CALORIE_ON_TARGET_5_DAYS", "Stay within calorie target 5 days in a row", 5),
            e(34, "Nidoking", "Poison", "Ground", "CALORIE_DEFICIT", "CALORIE_ON_TARGET_10_DAYS", "Stay within calorie target 10 days in a row", 10),
            e(35, "Clefairy", "Fairy", null, "SLEEP_STREAK", "SLEEP_MOONLIGHT_REST", "Log sleep before midnight 5 nights in a row", 5),
            e(36, "Clefable", "Fairy", null, "SLEEP_STREAK", "SLEEP_RESTFUL_14_DAYS", "Log 14 restful nights with 7+ hours", 14),
            e(37, "Vulpix", "Fire", null, "CALORIE_DEFICIT", "CALORIE_WARM_MEAL", "Log home-cooked healthy meals for 2 days", 2),
            e(38, "Ninetales", "Fire", null, "CALORIE_DEFICIT", "CALORIE_ON_TARGET_14_DAYS", "Maintain a 14-day calorie deficit discipline", 14),

            // 39-40 Jigglypuff line (Sleep)
            e(39, "Jigglypuff", "Normal", "Fairy", "SLEEP_STREAK", "SLEEP_7_DAYS_GOAL", "Log 7 nights of 7+ hours sleep", 7),
            e(40, "Wigglytuff", "Normal", "Fairy", "SLEEP_STREAK", "SLEEP_21_DAYS_GOAL", "Log 21 nights of 7+ hours sleep", 21),

            // 41-45 Bats & Plants (Sleep & Calorie)
            e(41, "Zubat", "Poison", "Flying", "SLEEP_STREAK", "SLEEP_DARK_ROOM", "Log uninterrupted sleep 3 nights in a row", 3),
            e(42, "Golbat", "Poison", "Flying", "SLEEP_STREAK", "SLEEP_DEEP_REST_7", "Log 7 nights of uninterrupted sleep", 7),
            e(43, "Oddish", "Grass", "Poison", "CALORIE_DEFICIT", "CALORIE_GREENS_1", "Log a high-vegetable meal", 1),
            e(44, "Gloom", "Grass", "Poison", "CALORIE_DEFICIT", "CALORIE_GREENS_3", "Log vegetable-rich meals 3 days in a row", 3),
            e(45, "Vileplume", "Grass", "Poison", "CALORIE_DEFICIT", "CALORIE_PLANT_STREAK_7", "Maintain healthy nutrient-dense diet for 7 days", 7),

            // 46-53 Paras, Venonat, Diglett, Meowth (Movement, Calorie, Sleep)
            e(46, "Paras", "Bug", "Grass", "CALORIE_DEFICIT", "CALORIE_WHOLE_FOODS", "Log all natural whole food entries for 2 days", 2),
            e(47, "Parasect", "Bug", "Grass", "CALORIE_DEFICIT", "CALORIE_DISCIPLINE_WEEK", "Strictly stay within target ceiling for 7 days", 7),
            e(48, "Venonat", "Bug", "Poison", "SLEEP_STREAK", "SLEEP_NIGHT_ROUTINE", "Complete bedtime routine before 11 PM 3 days", 3),
            e(49, "Venomoth", "Bug", "Poison", "SLEEP_STREAK", "SLEEP_DEEP_CYCLE", "Complete full sleep cycles 7 nights", 7),
            e(50, "Diglett", "Ground", null, "RUN_STEPS", "STEPS_LUNCH_WALK", "Take a 1,500-step walk after lunch", 1500),
            e(51, "Dugtrio", "Ground", null, "RUN_STEPS", "STEPS_10K_3_DAYS", "Hit 10,000 steps 3 days in a row", 3),
            e(52, "Meowth", "Normal", null, "RUN_STEPS", "STEPS_DAILY_COIN", "Hit daily step goal for 5 consecutive days", 5),
            e(53, "Persian", "Normal", null, "RUN_STEPS", "STEPS_AGILE_10_DAYS", "Hit daily step goal for 10 consecutive days", 10),

            // 54-55 Psyduck, Golduck (Hydration)
            e(54, "Psyduck", "Water", null, "WATER_HYDRATION", "WATER_FIRST_GOAL", "Hit your 100% daily hydration goal", 1),
            e(55, "Golduck", "Water", null, "WATER_HYDRATION", "WATER_STREAK_5", "Hit 100% daily hydration goal 5 days in a row", 5),

            // 56-57 Mankey, Primeape (Running/Workout)
            e(56, "Mankey", "Fighting", null, "RUN_STEPS", "WORKOUT_INTENSE_1", "Log your first intense cardio session", 1),
            e(57, "Primeape", "Fighting", null, "RUN_STEPS", "WORKOUT_INTENSE_5", "Log 5 intense cardio workouts", 5),

            // 58-59 Growlithe, Arcanine (Movement & Running)
            e(58, "Growlithe", "Fire", null, "RUN_STEPS", "RUN_HALF_MILE", "Complete a 0.5-mile brisk run or jog", 1),
            e(59, "Arcanine", "Fire", null, "RUN_STEPS", "RUN_FAST_MILE", "Complete a 1-mile run under 10 minutes", 1),

            // 60-62 Poliwag line (Hydration)
            e(60, "Poliwag", "Water", null, "WATER_HYDRATION", "WATER_LOG_3_TIMES", "Log water 3 times in a single day", 3),
            e(61, "Poliwhirl", "Water", null, "WATER_HYDRATION", "WATER_STREAK_7", "Maintain a 7-day hydration streak", 7),
            e(62, "Poliwrath", "Water", "Fighting", "WATER_HYDRATION", "WATER_WORKOUT_HYDRATE", "Drink 500ml water before and after a run", 1),

            // 63-65 Abra, Kadabra, Alakazam (Calorie Deficit Discipline)
            e(63, "Abra", "Psychic", null, "CALORIE_DEFICIT", "CALORIE_ON_TARGET_7_DAYS", "Stay strictly within calorie target 7 days", 7),
            e(64, "Kadabra", "Psychic", null, "CALORIE_DEFICIT", "CALORIE_ON_TARGET_14_DAYS", "Stay strictly within calorie target 14 days", 14),
            e(65, "Alakazam", "Psychic", null, "CALORIE_DEFICIT", "CALORIE_ON_TARGET_30_DAYS", "Stay strictly within calorie target 30 days", 30),

            // 66-68 Machop line (Workouts & Steps)
            e(66, "Machop", "Fighting", null, "RUN_STEPS", "WORKOUT_FIRST", "Log your first strength or cardio workout", 1),
            e(67, "Machoke", "Fighting", null, "RUN_STEPS", "WORKOUT_5_SESSIONS", "Log 5 completed workouts", 5),
            e(68, "Machamp", "Fighting", null, "RUN_STEPS", "WORKOUT_15_SESSIONS", "Log 15 completed workouts", 15),

            // 69-71 Bellsprout line (Hydration & Calorie)
            e(69, "Bellsprout", "Grass", "Poison", "WATER_HYDRATION", "WATER_MORNING_GLASS", "Drink a 250ml glass of water before 9 AM", 1),
            e(70, "Weepinbell", "Grass", "Poison", "WATER_HYDRATION", "WATER_STREAK_10", "Maintain a 10-day hydration streak", 10),
            e(71, "Victreebel", "Grass", "Poison", "CALORIE_DEFICIT", "CALORIE_CLEAN_EAT_5", "Log 5 days with zero processed sugar entries", 5),

            // 72-73 Tentacool, Tentacruel (Hydration)
            e(72, "Tentacool", "Water", "Poison", "WATER_HYDRATION", "WATER_LOG_5_TIMES", "Log water 5 times in a single day", 5),
            e(73, "Tentacruel", "Water", "Poison", "WATER_HYDRATION", "WATER_LIFETIME_25L", "Log a cumulative 25 liters of water", 25000),

            // 74-76 Geodude, Graveler, Golem (Calorie Deficit Discipline)
            e(74, "Geodude", "Rock", "Ground", "CALORIE_DEFICIT", "CALORIE_RESIST_TEMPTATION", "Acknowledge and pass on a high-calorie snack", 1),
            e(75, "Graveler", "Rock", "Ground", "CALORIE_DEFICIT", "CALORIE_SOLID_DEFICIT_5", "Maintain solid deficit for 5 consecutive days", 5),
            e(76, "Golem", "Rock", "Ground", "CALORIE_DEFICIT", "CALORIE_SOLID_DEFICIT_14", "Maintain solid deficit for 14 consecutive days", 14),

            // 77-78 Ponyta, Rapidash (1-Mile Run Challenges)
            e(77, "Ponyta", "Fire", null, "RUN_STEPS", "RUN_1_MILE_ONCE", "Complete your first 1-mile run session", 1),
            e(78, "Rapidash", "Fire", null, "RUN_STEPS", "RUN_5_MILES_TOTAL", "Complete a cumulative 5 miles of tracked runs", 5),

            // 79-80 Slowpoke, Slowbro (Sleep & Recovery)
            e(79, "Slowpoke", "Water", "Psychic", "SLEEP_STREAK", "SLEEP_RELAX_EVENING", "Log an evening relaxation habit", 1),
            e(80, "Slowbro", "Water", "Psychic", "SLEEP_STREAK", "SLEEP_REST_WEEKEND", "Log 8+ hours sleep on both weekend nights", 2),

            // 81-82 Magnemite, Magneton (Movement / Reminders)
            e(81, "Magnemite", "Electric", "Steel", "RUN_STEPS", "REMINDER_AFTERNOON_3", "Respond to 3 afternoon movement reminders", 3),
            e(82, "Magneton", "Electric", "Steel", "RUN_STEPS", "REMINDER_ACTIVE_HOUR", "Move for at least 250 steps in 8 different hours", 8),

            // 83 Farfetch'd (Calorie)
            e(83, "Farfetch'd", "Normal", "Flying", "CALORIE_DEFICIT", "CALORIE_PORTION_CONTROL", "Stay within exact calorie target (+/- 50 kcal)", 1),

            // 84-85 Doduo, Dodrio (Running)
            e(84, "Doduo", "Normal", "Flying", "RUN_STEPS", "RUN_2_MILES_TOTAL", "Complete a cumulative 2 miles of runs", 2),
            e(85, "Dodrio", "Normal", "Flying", "RUN_STEPS", "RUN_10_MILES_TOTAL", "Complete a cumulative 10 miles of runs", 10),

            // 86-87 Seel, Dewgong (Hydration)
            e(86, "Seel", "Water", null, "WATER_HYDRATION", "WATER_ICE_COLD", "Log 2,000ml water in a single day", 2000),
            e(87, "Dewgong", "Water", "Ice", "WATER_HYDRATION", "WATER_STREAK_14", "Maintain a 14-day hydration streak", 14),

            // 88-89 Grimer, Muk (Calorie Detox)
            e(88, "Grimer", "Poison", null, "CALORIE_DEFICIT", "CALORIE_REDUCE_PROCESSED", "Cut ultra-processed food for 1 full day", 1),
            e(89, "Muk", "Poison", null, "CALORIE_DEFICIT", "CALORIE_CLEAN_STREAK_3", "Cut ultra-processed food 3 days in a row", 3),

            // 90-91 Shellder, Cloyster (Hydration)
            e(90, "Shellder", "Water", null, "WATER_HYDRATION", "WATER_AFTERNOON_REFILL", "Drink 500ml water between 2 PM and 5 PM", 1),
            e(91, "Cloyster", "Water", "Ice", "WATER_HYDRATION", "WATER_STREAK_21", "Maintain a 21-day hydration streak", 21),

            // 92-94 Gastly, Haunter, Gengar (Sleep & Night habits)
            e(92, "Gastly", "Ghost", "Poison", "SLEEP_STREAK", "SLEEP_NO_SCREENS", "Turn off screens 30m before bed 2 nights", 2),
            e(93, "Haunter", "Ghost", "Poison", "SLEEP_STREAK", "SLEEP_NIGHT_CONSISTENT_5", "Sleep before 11 PM 5 nights in a row", 5),
            e(94, "Gengar", "Ghost", "Poison", "SLEEP_STREAK", "SLEEP_MASTER_14", "Log 14 nights with optimal sleep schedule", 14),

            // 95 Onix (Workouts)
            e(95, "Onix", "Rock", "Ground", "RUN_STEPS", "STEPS_20K_DAY", "Achieve a massive 20,000-step day", 20000),

            // 96-97 Drowzee, Hypno (Sleep)
            e(96, "Drowzee", "Psychic", null, "SLEEP_STREAK", "SLEEP_NAP_OR_REST", "Log a 20-min power nap or deep rest", 1),
            e(97, "Hypno", "Psychic", null, "SLEEP_STREAK", "SLEEP_DREAM_STREAK_10", "Maintain healthy 8-hour sleep for 10 nights", 10),

            // 98-99 Krabby, Kingler (Hydration)
            e(98, "Krabby", "Water", null, "WATER_HYDRATION", "WATER_HYDRATE_LUNCH", "Drink 500ml with lunch", 1),
            e(99, "Kingler", "Water", null, "WATER_HYDRATION", "WATER_DAILY_3000ML", "Drink 3,000ml in a single active day", 3000),

            // 100-101 Voltorb, Electrode (Movement)
            e(100, "Voltorb", "Electric", null, "RUN_STEPS", "STEPS_SPRINT_INTERVAL", "Complete a high-cadence walking interval", 1),
            e(101, "Electrode", "Electric", null, "RUN_STEPS", "RUN_FAST_HALF_MILE", "Run half a mile at top pace", 1),

            // 102-103 Exeggcute, Exeggutor (Calorie & Nutrition)
            e(102, "Exeggcute", "Grass", "Psychic", "CALORIE_DEFICIT", "CALORIE_BALANCED_MACROS", "Log balanced protein, carbs, and fats for 2 meals", 2),
            e(103, "Exeggutor", "Grass", "Psychic", "CALORIE_DEFICIT", "CALORIE_BALANCED_WEEK", "Log balanced macros across 5 days", 5),

            // 104-105 Cubone, Marowak (Calorie Discipline)
            e(104, "Cubone", "Ground", null, "CALORIE_DEFICIT", "CALORIE_RECOVER_DAY", "Recover from an overage by meeting target next day", 1),
            e(105, "Marowak", "Ground", null, "CALORIE_DEFICIT", "CALORIE_RESILIENT_STREAK", "Maintain deficit for 7 days after an overage", 7),

            // 106-107 Hitmonlee, Hitmonchan (Workouts & Steps)
            e(106, "Hitmonlee", "Fighting", null, "RUN_STEPS", "RUN_10_WORKOUTS", "Complete 10 tracked cardio/running workouts", 10),
            e(107, "Hitmonchan", "Fighting", null, "RUN_STEPS", "WORKOUT_STRENGTH_10", "Complete 10 tracked strength sessions", 10),

            // 108 Lickitung (Calorie)
            e(108, "Lickitung", "Normal", null, "CALORIE_DEFICIT", "CALORIE_MINDFUL_EATING", "Log all snacks accurately for 3 days", 3),

            // 109-110 Koffing, Weezing (Sleep/Recovery)
            e(109, "Koffing", "Poison", null, "SLEEP_STREAK", "SLEEP_FRESH_AIR", "Take a 10-minute fresh air walk before bed", 1),
            e(110, "Weezing", "Poison", null, "SLEEP_STREAK", "SLEEP_DEEP_BREATHE", "Complete 3 days of evening relaxation breaths", 3),

            // 111-112 Rhyhorn, Rhydon (Workouts & Steps)
            e(111, "Rhyhorn", "Ground", "Rock", "RUN_STEPS", "STEPS_7K_5_DAYS", "Walk 7,000 steps 5 days in a row", 5),
            e(112, "Rhydon", "Ground", "Rock", "RUN_STEPS", "STEPS_12K_5_DAYS", "Walk 12,000 steps 5 days in a row", 5),

            // 113 Chansey (Calorie Discipline)
            e(113, "Chansey", "Normal", null, "CALORIE_DEFICIT", "LOG_3_MEALS_3_DAYS", "Log breakfast, lunch, and dinner for 3 consecutive days", 3),

            // 114 Tangela (Calorie)
            e(114, "Tangela", "Grass", null, "CALORIE_DEFICIT", "CALORIE_FIBER_GOAL", "Log high-fiber meals 3 days in a row", 3),

            // 115 Kangaskhan (Movement & Reminders)
            e(115, "Kangaskhan", "Normal", null, "RUN_STEPS", "RUN_WITH_BUDDY", "Log 3 runs with active watch buddy equipped", 3),

            // 116-121 Horsea, Seadra, Goldeen, Seaking, Staryu, Starmie (Hydration)
            e(116, "Horsea", "Water", null, "WATER_HYDRATION", "WATER_SIP_REGULAR", "Drink at least 250ml water every 2 hours", 4),
            e(117, "Seadra", "Water", null, "WATER_HYDRATION", "WATER_LIFETIME_50L", "Log a cumulative 50 liters of water", 50000),
            e(118, "Goldeen", "Water", null, "WATER_HYDRATION", "WATER_EASY_START", "Log your first glass of water in the app", 1),
            e(119, "Seaking", "Water", null, "WATER_HYDRATION", "WATER_STREAK_12", "Maintain a 12-day hydration streak", 12),
            e(120, "Staryu", "Water", null, "WATER_HYDRATION", "WATER_EVENING_HYDRATE", "Hit 100% water goal before 8 PM", 1),
            e(121, "Starmie", "Water", "Psychic", "WATER_HYDRATION", "WATER_PERFECT_WEEK", "Hit 100% water goal every day for a full week", 7),

            // 122 Mr. Mime (Calorie)
            e(122, "Mr. Mime", "Psychic", "Fairy", "CALORIE_DEFICIT", "CALORIE_NO_SURPLUS_5", "Zero over-target warnings for 5 days", 5),

            // 123 Scyther (Running)
            e(123, "Scyther", "Bug", "Flying", "RUN_STEPS", "RUN_INTERVAL_SPRINT", "Log a high-speed sprint interval workout", 1),

            // 124 Jynx (Sleep)
            e(124, "Jynx", "Ice", "Psychic", "SLEEP_STREAK", "SLEEP_DEEP_RECOVERY", "Log 8 hours of sleep after a strenuous workout", 1),

            // 125 Electabuzz (Running)
            e(125, "Electabuzz", "Electric", null, "RUN_STEPS", "RUN_3_MILES_ONCE", "Complete a 3-mile distance run in a single session", 3),

            // 126 Magmar (Workouts)
            e(126, "Magmar", "Fire", null, "RUN_STEPS", "WORKOUT_CALORIE_BURN", "Burn 400 active calories in a single workout", 400),

            // 127 Pinsir (Workouts)
            e(127, "Pinsir", "Bug", null, "RUN_STEPS", "WORKOUT_CONSISTENT_WEEK", "Log 4 workouts within a single week", 4),

            // 128 Tauros (Running)
            e(128, "Tauros", "Normal", null, "RUN_STEPS", "RUN_FAST_2_MILES", "Complete a continuous 2-mile run", 2),

            // 129-130 Magikarp, Gyarados (Hydration)
            e(129, "Magikarp", "Water", null, "WATER_HYDRATION", "WATER_STREAK_7", "Maintain a 7-day hydration streak", 7),
            e(130, "Gyarados", "Water", "Flying", "WATER_HYDRATION", "WATER_STREAK_30", "Maintain a 30-day hydration streak", 30),

            // 131 Lapras (Hydration)
            e(131, "Lapras", "Water", "Ice", "WATER_HYDRATION", "WATER_LIFETIME_100L", "Log a cumulative 100 liters of water", 100000),

            // 132 Ditto (Calorie Discipline)
            e(132, "Ditto", "Normal", null, "CALORIE_DEFICIT", "CALORIE_ADAPT_MEAL", "Successfully substitute a high-calorie item with a healthy alternative", 1),

            // 133-136 Eevee Evolutions (Starter / Hydration / Running)
            e(133, "Eevee", "Normal", null, "STARTER", "ONBOARDING_CHOICE", "Complete your fitness profile setup", 1),
            e(134, "Vaporeon", "Water", null, "WATER_HYDRATION", "WATER_STREAK_25", "Maintain a 25-day hydration streak", 25),
            e(135, "Jolteon", "Electric", null, "RUN_STEPS", "RUN_15_MILES_TOTAL", "Complete a cumulative 15 miles of runs", 15),
            e(136, "Flareon", "Fire", null, "RUN_STEPS", "WORKOUT_BURN_1000_TOTAL", "Burn 1,000 active calories across workouts", 1000),

            // 137 Porygon (Calorie Logging Tech)
            e(137, "Porygon", "Normal", null, "CALORIE_DEFICIT", "CALORIE_LOG_EVERY_MEAL_7", "Log every single meal and snack for 7 days straight", 7),

            // 138-142 Fossils & Aerodactyl (Fossils & Milestone Achievements)
            e(138, "Omanyte", "Rock", "Water", "LEGENDARY", "FOSSIL_DISCOVER_1", "Reach 50 cumulative fitness milestone points", 50),
            e(139, "Omastar", "Rock", "Water", "LEGENDARY", "FOSSIL_EVOLVE_1", "Reach 100 cumulative fitness milestone points", 100),
            e(140, "Kabuto", "Rock", "Water", "LEGENDARY", "FOSSIL_DISCOVER_2", "Hit both water and step goals on the same day 5 times", 5),
            e(141, "Kabutops", "Rock", "Water", "LEGENDARY", "FOSSIL_EVOLVE_2", "Hit both water and step goals on the same day 15 times", 15),
            e(142, "Aerodactyl", "Rock", "Flying", "LEGENDARY", "MILESTONE_SPEED_CHALLENGE", "Complete a 5K run under 30 minutes", 1),

            // 143 Snorlax (Calorie Deficit Discipline)
            e(143, "Snorlax", "Normal", null, "CALORIE_DEFICIT", "RECOVER_FROM_SURPLUS_3_DAYS", "Recover from an over-calorie day by staying on target the next 3 days", 3),

            // 144 Articuno (Legendary Bird - Hydration Master)
            e(144, "Articuno", "Ice", "Flying", "LEGENDARY", "LEGENDARY_HYDRATION_MASTER", "Reach a 30-day hydration streak with zero missed goals", 30),

            // 145 Zapdos (Legendary Bird - Stamina Master)
            e(145, "Zapdos", "Electric", "Flying", "LEGENDARY", "LEGENDARY_50_MILES_RUN", "Accumulate 50 cumulative miles of tracked runs", 50),

            // 146 Moltres (Legendary Bird - Deficit Master)
            e(146, "Moltres", "Fire", "Flying", "LEGENDARY", "LEGENDARY_30_DAYS_DEFICIT", "Maintain 30 consecutive days within your calorie ceiling", 30),

            // 147-149 Dratini line (High Milestone Stamina)
            e(147, "Dratini", "Dragon", null, "RUN_STEPS", "RUN_WEEKLY_10_MILES", "Run 10 miles within a single week", 10),
            e(148, "Dragonair", "Dragon", null, "RUN_STEPS", "RUN_WEEKLY_20_MILES", "Run 20 miles within a single week", 20),
            e(149, "Dragonite", "Dragon", "Flying", "LEGENDARY", "RUN_MARATHON_CUMULATIVE", "Accumulate 26.2 miles (Marathon distance) of runs", 26),

            // 150 Mewtwo (Grand Milestone: 149 Pokémon unlocked)
            e(150, "Mewtwo", "Psychic", null, "LEGENDARY", "UNLOCK_149_POKEMON", "Unlock 149 different Pokémon in the Pokédex", 149),

            // 151 Mew (Ultimate Grandmaster: Complete Kanto 1-150)
            e(151, "Mew", "Psychic", null, "LEGENDARY", "UNLOCK_ALL_150", "Complete Kanto Pokédex by unlocking all 150 Pokémon", 150)
        )
    }

    private fun e(
        id: Int,
        name: String,
        type1: String,
        type2: String?,
        category: String,
        ruleCode: String,
        description: String,
        threshold: Int
    ): PokemonEntity {
        val padded = id.toString().padStart(3, '0')
        return PokemonEntity(
            pokedexNumber = id,
            name = name,
            primaryType = type1,
            secondaryType = type2,
            categoryTag = category,
            unlockRuleCode = ruleCode,
            unlockDescription = description,
            requiredProgress = threshold,
            currentProgress = 0,
            isUnlocked = false,
            unlockedAtMillis = null,
            spriteAssetPath = "sprites/pokemon_$padded.png"
        )
    }
}
