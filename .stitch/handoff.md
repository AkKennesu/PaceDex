# Pokétch Mobile — Google Stitch Handoff Pack (`DESIGN.md` + Screen Prompts)

Use this pack to transfer your **Pokétch Mobile (`:app-mobile`)** UI/UX design directly into [Google Stitch](https://stitch.google.com/).

---

## Step-by-Step Transfer Workflow
1. Open [Google Stitch](https://stitch.google.com/) and start a new **App** project.
2. **Attach Reference Image**: Click the attachment icon and upload **`poketch_mobile_modern_v2_showcase.png`** from our chat so Stitch matches your exact visual direction.
3. **Generate Screen 1 First**: Paste **Prompt 1 (Home — Today)** below to establish the visual foundation, then lock your colors and typography in Stitch's **Design System** panel (or import the **`DESIGN.md`** section below).
4. **Generate Remaining Screens**: Paste Prompts **2 through 5** one at a time on the same canvas so Stitch keeps every screen cohesive.
5. **Test & Export**: Click **Play** in Stitch to preview the clickable prototype, refine any component with Direct Edit, and export to **Figma**, **HTML/CSS**, or your coding agent via **Stitch MCP**.

---

## 1. Stitch Design System Rules (`DESIGN.md`)

Paste or import this into Google Stitch's Design System / `DESIGN.md` configuration to lock visual consistency across all screens:

```markdown
# Pokétch Mobile — DESIGN.md

## Visual Direction
- **Vibe**: Modern 2026 consumer fitness & habit companion (clean, airy, approachable, high-contrast).
- **No Retro UI Clutter**: Zero pixel fonts, zero monospace stat badges, zero heavy black borders. Keep all typography modern geometric sans-serif; let the companion Pokémon illustrations be the sole playful gamified accent.

## Color Palette
- **Canvas Background**: `#F4F7F5` (Airy botanical off-white with a subtle emerald top ambient wash)
- **Primary Card Surface**: `#FFFFFF` (Pure white Bento cards)
- **Card Border**: `1px solid #E4EBE7` with soft diffused drop shadow (`0 6px 20px rgba(15, 23, 42, 0.06)`)
- **Dark Accent Surface**: `#0F172A` (Deep Slate-Navy for TDEE hero banner, active navigation pill, and dark buttons)
- **Primary Action / On-Track**: `#16A34A` (Emerald Green) with `#DCFCE7` soft tint
- **Hydration Accent**: `#0EA5E9` (Sky Cyan) with `#E0F2FE` soft tint
- **Steps / Mission Accent**: `#F59E0B` (Warm Amber) with `#FEF3C7` soft tint
- **Over-Target Alert / Destructive**: `#EF4444` (Coral Red)
- **Text Hierarchy**: Primary `#0F172A`, Secondary `#475569`, Muted `#94A3B8`

## Typography
- **Font Family**: `Plus Jakarta Sans` (or `Inter`) across all headings, body copy, buttons, and numbers (`tabular-nums` for calorie/step counters).
- **Scale**: Page Title `24px Bold`, Section Heading `16px SemiBold`, Hero Metric `28–34px Bold`, Body `13–14px Regular`, Pill Labels `11px SemiBold`.

## Layout & Components
- **Corner Radii**: `24px–26px` on hero Bento cards, `18px–20px` on list/grid cards, `999px` full pill on primary CTA buttons and filter chips.
- **Bottom Navigation**: Floating white capsule dock (`margin: 16px`, `border-radius: 30px`) with 4 equal destinations (`Home`, `Log`, `Pokédex`, `Sync`) and a solid `#0F172A` active pill indicator.
```

---

## 2. Screen-by-Screen Google Stitch Prompts

### Prompt 1 — Home (`Today` Command Center) *(Upload `poketch_mobile_modern_v2_showcase.png` with this prompt)*
```text
Design the "Today" home screen for a modern Android fitness and habit-tracking companion app called Pokétch that pairs with a Wear OS smartwatch and unlocks collectible companion creatures through healthy habits.

Theme: Clean, airy 2026 consumer health-app aesthetic (similar to modern Material 3 Expressive and minimalist Bento fitness apps). Airy botanical off-white background (#F4F7F5) with a soft emerald ambient glow at the top, pure white rounded Bento cards (#FFFFFF, 24px radius, subtle 1px #E4EBE7 border, soft shadow), modern Plus Jakarta Sans typography (no pixel fonts), emerald green (#16A34A) primary accent, and deep slate-navy (#0F172A) secondary accent.

Content from top to bottom:
1. Header with "Today" title and "Mon, Nov 23" subtitle on the left, and a compact white status pill "● Watch 92%" on the right.
2. Hero "Calorie Budget" white Bento card with a soft green "On Track" badge, a large smooth circular emerald progress ring on the left showing "455 kcal left" in the center, and three stacked metrics on the right: Consumed (1,640 kcal), Daily Goal (2,095 kcal), and Deficit (-500 kcal).
3. "Active Partner" soft mint-tinted card (#EEF9F1) featuring "Bulbasaur", "7-day streak • Next: Ponyta", a slim amber unlock progress bar, and a cute creature companion illustration framed in a soft circular glow on the right.
4. Two-column Bento habit cards side by side:
   - Left card ("Water" sky-blue pill): "1.5 L" of 2.5 L goal, slim blue progress bar, and a sky-blue "+ 250 ml" pill button.
   - Right card ("Steps" warm-amber pill): "6,840" of 10,000 steps, slim amber progress bar, and a deep slate-navy "Start 1-Mi Run" pill button.
5. Full-width emerald green pill CTA button: "+ Log Meal or Quick Calories".
6. Floating white capsule bottom navigation bar with 4 tabs: Home (active inside a dark slate #0F172A pill), Log, Pokédex, and Sync.
```

---

### Prompt 2 — Nutrition & TDEE Log (`Log` Tab)
```text
Create the "Nutrition" screen for the Pokétch mobile app matching the exact design system and floating 4-tab capsule bottom navigation bar (with "Log" active).

Content from top to bottom:
1. Header with "Nutrition" on the left and a white pill button "Edit TDEE" on the right.
2. Dark slate-navy (#0F172A) rounded hero card titled "Daily Deficit Target (Mifflin-St Jeor)" showing bold white "2,095 kcal", a soft emerald "-500 / day" pill badge, and a clean footer row: "BMR 1,674 • TDEE 2,595 • Floor 1,500".
3. Three equal-width white Bento macro cards side by side, each with a smooth circular progress ring and percentage inside: Protein (80%, 112g of 140g, emerald), Carbs (75%, 165g of 220g, amber), and Fat (68%, 48g of 70g, sky blue).
4. "Today's Log" section header with "1,640 / 2,095" on the right, followed by clean white rounded meal cards:
   - Breakfast (soft green "Phone" chip) — Oatmeal, Eggs & Coffee — 420 kcal
   - Lunch (soft green "Phone" chip) — Grilled Chicken Bowl — 680 kcal
   - Snack (soft sky-blue "Watch" chip) — Watch Quick-Add — 199 kcal
   - Dinner (soft green "Phone" chip) — Salmon & Greens — 341 kcal
```

---

### Prompt 3 — 151 Kanto Collection (`Pokédex` Tab)
```text
Create the "Pokédex" collection screen for the Pokétch mobile app matching the exact design system and floating 4-tab capsule bottom navigation bar (with "Pokédex" active).

Content from top to bottom:
1. Header with "Pokédex" on the left and a dark slate-navy pill "18 / 151" on the right.
2. Horizontal row of rounded filter chips: "All" (active dark slate), "Water", "Steps", "Deficit".
3. Clean 3-column grid of rounded white collector cards (20px radius):
   - Top row (unlocked): #001 Bulbasaur (green dot + "Partner"), #025 Pikachu ("Unlocked"), #002 Ivysaur ("Unlocked").
   - Second row (locked): #077, #129, #143 showing soft grey silhouettes, "Locked" label, and a slim amber progress bar at the bottom of each card.
4. Elevated white "Active Mission" bottom detail card showing a soft amber "Active Mission" badge, title "Unlock #077 Ponyta", description "Run 1.0 mile in a single session on your Wear OS watch to capture Ponyta", an amber progress bar at 64% ("0.64 / 1.00 miles completed"), and a full-width emerald green pill CTA button "Send Run Prompt to Watch".
```

---

### Prompt 4 — Watch Sync & Settings (`Sync` Tab)
```text
Create the "Watch & Sync" settings screen for the Pokétch mobile app matching the exact design system and floating 4-tab capsule bottom navigation bar (with "Sync" active).

Content from top to bottom:
1. Header titled "Watch & Sync".
2. Elevated white "Wear OS Watch" status card featuring a circular red-and-white Poké Ball app icon inside a soft grey badge on the left, "Connected • 92%" green pill badge, subtitle "Last synced just now via Data Layer", and a full-width dark slate-navy "Sync Now" pill button.
3. "Smart Reminders" section with three clean white rounded toggle cards using emerald green switches:
   - Hydration Nudges — Every 90 min • 08:00–21:00 (ON)
   - 1-Mile Run Prompt — Daily at 17:00 if under 50% steps (ON)
   - Meal Check-Ins — Breakfast, Lunch & Dinner (ON)
4. White "Appearance & Account" card containing a segmented Light / OLED Dark pill switcher, divider line, "trainer@poketch.app" ("Google Account Linked"), and a coral-red "Sign Out" text action on the right.
```

---

### Prompt 5 — Firebase Auth & 6-Digit Email OTP Screen (`Login` Flow)
```text
Create a modern two-state authentication flow for the Pokétch mobile app matching the exact airy botanical off-white (#F4F7F5) and white Bento card design system:
- State 1 (Sign In): Centered circular red-and-white Poké Ball app icon, headline "Welcome to Pokétch", subtitle "Sync your Wear OS workouts, calorie deficit, and 151 collection", a clean white card with a rounded email input field, a full-width emerald green pill button "Send 6-Digit Email OTP", a subtle "or" divider, and a clean "Continue with Google" button.
- State 2 (6-Digit OTP Verification): Shows "Enter the 6-digit code sent to trainer@poketch.app", six clean rounded numeric OTP input boxes (active box highlighted with emerald border), a full-width emerald button "Verify & Continue", and bottom links for "Change email" and "Resend code (00:45)".
```
