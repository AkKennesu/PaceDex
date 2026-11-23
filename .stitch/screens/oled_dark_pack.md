# Pokétch Mobile — Tactical Mint OLED Dark Mode (Export & Handoff Pack)

This document packages all 10 production-ready screens from the **Pokétch Mobile (`:app-mobile`)** OLED Dark Tactical Mint suite into an exportable HTML/CSS code bundle and architectural guide.

---

## 1. Design Tokens & Global CSS Setup

Include the following Google Font (`Plus Jakarta Sans`) and Tailwind CSS config / root CSS custom variables across your mobile views:

```html
<!-- Plus Jakarta Sans Font -->
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
<script src="https://cdn.tailwindcss.com"></script>

<style>
  :root {
    --bg-oled: #000000;
    --surface-base: #090F0D;
    --surface-card: #131E19;
    --surface-card-subtle: #0E1613;
    --border-hairline: rgba(52, 211, 153, 0.15);
    --border-subtle: rgba(255, 255, 255, 0.08);
    
    /* Tactical Mint System */
    --mint-primary: #34D399;
    --mint-deep: #059669;
    --mint-text-contrast: #022C22;
    --mint-glow: rgba(52, 211, 153, 0.25);
    
    /* Telemetry & Metric Accents */
    --accent-steps-amber: #FBBF24;
    --accent-hydration-cyan: #38BDF8;
    --accent-coral: #EF4444;
    
    /* Typography Neutrals */
    --text-primary: #F8FAFC;
    --text-secondary: #94A3B8;
    --text-muted: #64748B;
  }
  
  body {
    font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
    background-color: var(--bg-oled);
    color: var(--text-primary);
  }
</style>
```

---

## 2. Canvas Screens & Screen Inventory

The following 10 screens constitute the full Tactical Mint OLED Dark flow on the Canvas:

| # | Screen Name | Datastore ID | Key Interactive Elements / Content |
|---|---|---|---|
| 1 | **Today (Home)** | `SCREEN_18` | Calorie budget ring, active partner gauge (Bulbasaur), Bento habit cards, quick-log CTA |
| 2 | **Nutrition & TDEE Log** | `SCREEN_22` | Mifflin-St Jeor target banner, 3-macro progress rings, meal timeline with Phone/Watch badges |
| 3 | **Pokédex Collection** | `SCREEN_20` | Filter chips, 3-column collector grid (unlocked & locked silhouettes), Ponyta active run mission |
| 4 | **Watch & Sync Settings** | `SCREEN_16` | Wear OS pairing status, Data Layer sync CTA, toggleable smart reminders |
| 5 | **Sign In & Sign Up** | `SCREEN_14` | Auth mode tabs, dynamic strength meter, Google/Facebook/Email auth, forgot password route |
| 6 | **6-Digit OTP Verification** | `SCREEN_12` | 6 passkey input slots, Wear OS watch proximity detector card, countdown timer |
| 7 | **Reset Password** | `SCREEN_10` | 3-step recovery flow instructions, Trainer email input, dispatched preview toast |
| 8 | **Check Your Email** | `SCREEN_8` | Dispatch status pill, "Open Email App", OTP fallback, account safeguard Bento card |
| 9 | **Create New Password** | `SCREEN_6` | Password validation checklist, strength meter, device sign-out toggle |
| 10 | **Password Reset Success** | `SCREEN_4` | Shield-check confirmation aura, account security summary, return to login CTA |

---

## 3. Screen 1: Today (Home — OLED Dark) Code Skeleton

```html
<div class="min-h-screen bg-black text-slate-100 p-4 pb-28 max-w-md mx-auto relative font-['Plus_Jakarta_Sans']">
  <!-- Top App Bar -->
  <header class="flex items-center justify-between pt-2 pb-4">
    <div>
      <h1 class="text-2xl font-bold text-white tracking-tight">Today</h1>
      <p class="text-xs text-slate-400 font-medium">Mon, Nov 23</p>
    </div>
    <div class="flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#131E19] border border-emerald-500/20 text-xs text-emerald-400 font-semibold">
      <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
      Watch 92%
    </div>
  </header>

  <!-- Calorie Hero Bento Card -->
  <div class="rounded-3xl p-5 bg-[#131E19] border border-emerald-500/15 shadow-2xl mb-4">
    <div class="flex items-center justify-between mb-4">
      <span class="text-xs uppercase font-bold tracking-wider text-slate-400">Calorie Deficit</span>
      <span class="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-500/20 text-[#34D399]">On Track</span>
    </div>
    <div class="flex items-center gap-6">
      <!-- Progress Circle -->
      <div class="relative w-28 h-28 flex items-center justify-center">
        <svg class="w-full h-full -rotate-90" viewBox="0 0 100 100">
          <circle cx="50" cy="50" r="40" stroke="#1F2E26" stroke-width="8" fill="none" />
          <circle cx="50" cy="50" r="40" stroke="#34D399" stroke-width="8" stroke-dasharray="251.2" stroke-dashoffset="60" stroke-linecap="round" fill="none" />
        </svg>
        <div class="absolute text-center">
          <span class="block text-xl font-bold text-white tabular-nums">455</span>
          <span class="block text-[10px] text-slate-400">kcal left</span>
        </div>
      </div>
      <!-- Metrics Stack -->
      <div class="flex-1 space-y-2">
        <div class="flex justify-between text-xs">
          <span class="text-slate-400">Consumed</span>
          <span class="font-bold text-white tabular-nums">1,640 kcal</span>
        </div>
        <div class="flex justify-between text-xs">
          <span class="text-slate-400">Daily Goal</span>
          <span class="font-bold text-white tabular-nums">2,095 kcal</span>
        </div>
        <div class="flex justify-between text-xs border-t border-white/5 pt-1.5">
          <span class="text-slate-400">Target Deficit</span>
          <span class="font-bold text-[#34D399] tabular-nums">-500 kcal</span>
        </div>
      </div>
    </div>
  </div>

  <!-- Primary Action CTA -->
  <button class="w-full py-4 rounded-full bg-[#34D399] hover:bg-[#10B981] text-[#022C22] font-bold text-sm tracking-wide shadow-lg shadow-emerald-500/20 flex items-center justify-center gap-2 transition-all">
    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/></svg>
    Log Meal or Quick Calories
  </button>
</div>
```

---

## 4. Screen 2: Nutrition & TDEE Log (`SCREEN_22`) Key Snippet

```html
<!-- Macro Cards Row -->
<div class="grid grid-cols-3 gap-3 mb-5">
  <!-- Protein -->
  <div class="bg-[#131E19] border border-emerald-500/15 rounded-2xl p-3 text-center">
    <span class="text-xs text-slate-400 font-semibold block mb-2">Protein</span>
    <span class="text-lg font-bold text-[#34D399] tabular-nums">112g</span>
    <span class="text-[10px] text-slate-500 block">of 140g</span>
  </div>
  <!-- Carbs -->
  <div class="bg-[#131E19] border border-amber-500/20 rounded-2xl p-3 text-center">
    <span class="text-xs text-slate-400 font-semibold block mb-2">Carbs</span>
    <span class="text-lg font-bold text-[#FBBF24] tabular-nums">165g</span>
    <span class="text-[10px] text-slate-500 block">of 220g</span>
  </div>
  <!-- Fat -->
  <div class="bg-[#131E19] border border-sky-500/20 rounded-2xl p-3 text-center">
    <span class="text-xs text-slate-400 font-semibold block mb-2">Fat</span>
    <span class="text-lg font-bold text-[#38BDF8] tabular-nums">48g</span>
    <span class="text-[10px] text-slate-500 block">of 70g</span>
  </div>
</div>
```

---

## 5. Screen 3: Pokédex Collection (`SCREEN_20`) Key Snippet

```html
<!-- Collector Card Grid Item (Tactical Dark Mode) -->
<div class="bg-[#131E19] border border-emerald-500/20 rounded-2xl p-3 flex flex-col items-center text-center">
  <div class="w-full flex justify-between items-center text-[10px] font-bold text-slate-400 mb-2">
    <span>#001</span>
    <span class="text-[#34D399] flex items-center gap-1">● Partner</span>
  </div>
  <div class="w-16 h-16 rounded-xl bg-[#090F0D] border border-white/5 flex items-center justify-center mb-2">
    <!-- Companion Asset Image Placeholder -->
    <span class="text-xs text-emerald-400 font-semibold">Bulbasaur</span>
  </div>
  <span class="text-xs font-bold text-white">Bulbasaur</span>
  <span class="text-[10px] text-emerald-400 mt-0.5">Hydration Lv.4</span>
</div>
```

---

## 6. Floating 4-Tab Bottom Navigation Dock

```html
<nav class="fixed bottom-4 left-4 right-4 max-w-md mx-auto bg-[#090F0D]/90 backdrop-blur-md border border-emerald-500/20 rounded-full px-3 py-2 flex items-center justify-between shadow-2xl z-50">
  <button class="flex-1 py-2 px-3 rounded-full bg-[#34D399] text-[#022C22] font-bold text-xs flex items-center justify-center gap-1.5">
    <span>Home</span>
  </button>
  <button class="flex-1 py-2 px-3 text-slate-400 hover:text-white font-medium text-xs flex items-center justify-center">
    <span>Log</span>
  </button>
  <button class="flex-1 py-2 px-3 text-slate-400 hover:text-white font-medium text-xs flex items-center justify-center">
    <span>Pokédex</span>
  </button>
  <button class="flex-1 py-2 px-3 text-slate-400 hover:text-white font-medium text-xs flex items-center justify-center">
    <span>Sync</span>
  </button>
</nav>
```
