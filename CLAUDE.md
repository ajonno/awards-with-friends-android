# Awards With Friends - Android App

Android version of Awards With Friends, matching iOS app functionality exactly.

## Project Info

- **Package**: `com.aamsco.awardswithfriends`
- **Min SDK**: 30 (Android 11+)
- **Target SDK**: 36
- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3)

## Architecture

**MVVM with Repository Pattern**

```
UI Layer (Compose)
├── Screens (Composables)
└── ViewModels (StateFlow)
        │
Data Layer
├── Repositories (interfaces + impl)
└── Data Sources (Firebase, Billing)
```

## Firebase Configuration

- **Project**: Same as iOS (awards-with-friends)
- **Region**: asia-south1 (Mumbai)
- **Config file**: `app/google-services.json`

### Cloud Functions Called
- `createCompetition` - Creates competition with invite code
- `joinCompetition` - Validates code, adds participant
- `leaveCompetition` - Removes participant
- `deleteCompetition` - Owner only
- `castVote` - Records/updates vote
- `updateFcmToken` - Saves FCM token
- `deleteAccount` - Cascading delete

## Authentication

**Supported Methods** (Android):
1. Google Sign-In (primary)
2. Email/Password (sign up + sign in)

Note: Sign in with Apple is iOS-only.

## Data Models

All models match Firestore schema exactly. See iOS CLAUDE.md for full field definitions.

### User
```kotlin
data class User(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoURL: String? = null,
    val fcmToken: String? = null,
    val createdAt: Timestamp,
    val updatedAt: Timestamp
)
```

### Competition
```kotlin
data class Competition(
    val id: String,
    val name: String,
    val ceremonyId: String,
    val inviteCode: String,  // 6-char alphanumeric
    val createdBy: String,
    val createdAt: Timestamp,
    val status: CompetitionStatus,
    val participantCount: Int
)

enum class CompetitionStatus { OPEN, LOCKED, COMPLETED, INACTIVE }
```

### Ceremony
```kotlin
data class Ceremony(
    val id: String,
    val name: String,
    val eventType: EventType,
    val date: Timestamp,
    val status: CeremonyStatus,
    val categoryCount: Int
)

enum class EventType { OSCARS, EMMYS, GOLDENGLOBES, GRAMMYS, TONYS, SAGAWARDS, BAFTAS, OTHER }
```

### Category
```kotlin
data class Category(
    val id: String,
    val ceremonyId: String,
    val name: String,
    val displayOrder: Int,
    val isLocked: Boolean,
    val winnerId: String? = null,
    val nomineeCount: Int
)
```

### Nominee
```kotlin
data class Nominee(
    val id: String,
    val categoryId: String,
    val name: String,
    val secondaryName: String? = null,
    val imageURL: String? = null
)
```

### Vote
```kotlin
data class Vote(
    val id: String,
    val competitionId: String,
    val userId: String,
    val categoryId: String,
    val nomineeId: String,
    val createdAt: Timestamp,
    val updatedAt: Timestamp
)
```

### Participant
```kotlin
data class Participant(
    val id: String,
    val competitionId: String,
    val userId: String,
    val displayName: String,
    val photoURL: String? = null,
    val score: Int,
    val totalVotes: Int,
    val joinedAt: Timestamp
)
```

## Screens

### 1. LoginScreen
- App logo/icon
- "Awards With Friends" title
- Google Sign-In button (full-width)
- "Sign in with Email" text button
- Loading overlay during auth
- Error snackbar

### 2. EmailSignInScreen
- Back button
- Title: "Sign In" or "Create Account"
- Email TextField
- Password TextField (PasswordVisualTransformation)
- Confirm password (sign up only)
- Primary button
- Toggle text link
- Validation: email format, password 6+ chars

### 3. HomeScreen
- TopAppBar: "Competitions" + plus menu
- TabRow filter: All | Mine | Joined
- LazyColumn of CompetitionCards
- Empty state with Create/Join buttons
- FAB or menu for Create/Join
- Paywall check before Create/Join

### 4. CompetitionCard
- Competition name (bold)
- Event name
- Status chip (Open/Locked/Completed/Inactive)
- Participant count
- Score display
- Leaderboard icon button
- Share icon (owner only)

### 5. CreateCompetitionScreen
- TopAppBar: Cancel + Create
- Name TextField (50 char max)
- Ceremony picker (ExposedDropdownMenuBox)
- Loading indicator
- On success → dismiss + show InviteBottomSheet

### 6. JoinCompetitionScreen
- TopAppBar: Cancel + Join
- Invite code TextField (6 chars, uppercase)
- Instructions text
- Loading indicator
- Error handling

### 7. CompetitionDetailScreen
- TopAppBar: back + competition name + overflow menu
- Progress summary
- LazyColumn of categories grouped by status:
  - "Make Your Picks" (unvoted)
  - "Your Picks" (voted)
  - "Winners Announced" (completed)
- Category row shows: name, your pick, result icon

### 8. CategoryDetailScreen
- TopAppBar: category name
- "Locked" banner if voting closed
- LazyVerticalGrid of NomineeCards
- Winner highlighted with crown

### 9. NomineeCard
- AsyncImage (Coil) for poster/photo
- Name text
- Secondary name (acting categories)
- Selection border/checkmark
- Winner crown icon

### 10. LeaderboardScreen
- TopAppBar: "Leaderboard"
- Competition name subtitle
- LazyColumn of participant rows
- Current user highlighted
- Top 3 with medal icons

### 11. ProfileScreen
- Profile header (photo, name, email)
- Edit name inline
- Photo picker
- Settings section:
  - Push notifications toggle
  - App version
- Actions:
  - Restore Purchases
  - Sign Out
  - Delete Account (destructive)

### 12. PaywallScreen
- Close button (X)
- Trophy icon
- "Unlock Competitions" title
- Feature list with checkmarks
- Price ($2.99)
- Purchase button
- Restore Purchases link

### 13. InviteBottomSheet
- ModalBottomSheet
- "Invite Friends" title
- Large invite code (monospace)
- Copy button
- Share button (ShareSheet)

## In-App Purchase

- **Product ID**: `com.awardswithfriends.competitions`
- **Type**: Non-consumable (one-time $2.99)
- **Library**: Google Play Billing 6.x

### Feature Flag
Check Firestore `/config/features`:
```kotlin
val requiresPaymentForCompetitions: Boolean
```

If false → allow all users
If true → check BillingRepository.hasCompetitionsAccess

## Push Notifications

- **Service**: FirebaseMessagingService
- **Channel**: "competition_updates"
- **Permission**: Required on Android 13+

### Notification Types
1. Winner Announced
2. Voting Locked
3. Competition Update

### Deep Linking
Handle notification data payload to navigate to specific competition/category.

## Key Patterns

### Firestore Real-time → Flow
```kotlin
fun competitionsFlow(): Flow<List<Competition>> = callbackFlow {
    val listener = firestore.collection("competitions")
        .whereArrayContains("participantIds", userId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) close(error)
            else trySend(snapshot.toObjects())
        }
    awaitClose { listener.remove() }
}
```

### ViewModel State
```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val competitionRepo: CompetitionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            competitionRepo.competitionsFlow().collect { competitions ->
                _uiState.update { it.copy(competitions = competitions, isLoading = false) }
            }
        }
    }
}
```

### Optimistic UI for Voting
```kotlin
fun castVote(nomineeId: String) {
    // 1. Update UI immediately
    _uiState.update { it.copy(selectedNomineeId = nomineeId) }

    viewModelScope.launch {
        try {
            // 2. Call cloud function
            voteRepo.castVote(competitionId, categoryId, nomineeId)
        } catch (e: Exception) {
            // 3. Revert on error
            _uiState.update { it.copy(selectedNomineeId = previousId, error = e.message) }
        }
    }
}
```

## Dependencies

See `gradle/libs.versions.toml` for versions.

### Core
- Kotlin Coroutines
- Hilt (DI)
- Navigation Compose
- Coil (images)

### Firebase
- firebase-auth-ktx
- firebase-firestore-ktx
- firebase-functions-ktx
- firebase-messaging-ktx

### Billing
- billing-ktx (Google Play Billing)

### Auth
- play-services-auth (Google Sign-In)

## Project Structure

```
app/src/main/java/com/aamsco/awardswithfriends/
├── AwardsWithFriendsApp.kt
├── MainActivity.kt
├── di/
│   ├── AppModule.kt
│   └── FirebaseModule.kt
├── data/
│   ├── model/
│   ├── repository/
│   └── source/
├── ui/
│   ├── navigation/
│   ├── theme/
│   ├── components/
│   ├── auth/
│   ├── home/
│   ├── competition/
│   ├── leaderboard/
│   └── profile/
└── util/
```

## iOS Reference

For detailed UI specifications, state management, and feature behavior, see:
`/Users/angusjohnston/src-ios-native/awards-with-friends/CLAUDE.md`

The Android app must match iOS functionality exactly (except Sign in with Apple).
