# Google Glass Pickleball Scorekeeper

## Product Specification --- v0.1

### Goal

A glanceable, touchpad-only pickleball scoreboard for Google Glass
Explorer Edition. The player should be able to operate it during a match
without menus or looking away from the court for more than a moment.

The application should be fully usable offline and optimized for the
limited Google Glass display and touchpad.

------------------------------------------------------------------------

## 1. Primary Interaction Model

  Gesture          Action
  ---------------- -----------------------------------------------------
  Swipe forward    Serving side won rally → add 1 point
  Swipe backward   Serving side lost rally → advance or transfer serve
  Double tap       Undo previous scoring action
  Long press       Reset scoreboard, with confirmation

### Design rationale

A backward swipe represents a lost rally rather than an undo operation.
The scoring engine therefore advances the serve counter or transfers
possession without awarding a point.

Double tap is reserved for Undo because accidental score input is likely
during active play and undoing a single action is substantially more
useful than having two gestures mapped to reset.

Long press is used for reset because it is less likely to occur
accidentally.

------------------------------------------------------------------------

## 2. Core Display Model

The serving team is **always displayed on the left**.

Example:

``` text
     7       5
    YOU     THEM

        S2
```

If the opposing team gains serve:

``` text
     5       7
    THEM     YOU

        S1
```

This makes spatial position communicate service possession:

-   Left = serving team
-   Right = receiving team
-   S1 / S2 = current server number

A compact visual implementation could resemble:

``` text
┌──────────────────────────────┐
│                              │
│       5        7             │
│      THEM     YOU            │
│                              │
│          ①                   │
└──────────────────────────────┘
```

The server indicator may use `①` / `②`, `S1` / `S2`, or another highly
legible minimal representation.

A subtle underline or service marker beneath the left score may
reinforce that the left side is serving without requiring a dedicated
possession label.

------------------------------------------------------------------------

## 3. Match State

Presentation order should not determine internal team identity. The
application should maintain canonical teams and derive display ordering
from service possession.

Conceptual state:

``` text
MatchState
├── teamA.score
├── teamB.score
├── servingTeam       A | B
├── serverNumber      1 | 2
├── startingTeam      A | B
├── openingServe      bool
├── gameStarted       bool
├── gameOver          bool
└── history[]         MatchState snapshots
```

Display ordering is derived as:

``` text
displayLeft  = servingTeam
displayRight = receivingTeam
```

This prevents display-order changes from contaminating scoring logic.

------------------------------------------------------------------------

## 4. Rally State Machine

### 4.1 Forward Swipe --- Serving Side Wins Rally

Behavior:

``` text
SWIPE_FORWARD
      │
      ▼
servingTeam.score++
      │
      ▼
same serving team
same server
      │
      ▼
check game completion
      │
      ▼
persist state
      │
      ▼
refresh display
```

Example:

``` text
YOU 4 - 3 THEM
S2
```

Forward swipe:

``` text
YOU 5 - 3 THEM
S2
```

The serving team retains service and the current server remains
unchanged.

------------------------------------------------------------------------

### 4.2 Backward Swipe --- Serving Side Loses Rally

Normal doubles behavior:

``` text
SWIPE_BACK
      │
      ▼
Is this Server 1?
   /          \
 YES          NO
  │            │
server = 2     transfer serve
  │            │
  │          server = 1
  │            │
  └─────┬──────┘
        ▼
 persist state
        │
        ▼
 refresh display
```

#### First server loses rally

Before:

``` text
YOU 5 - 3 THEM
S1
```

After:

``` text
YOU 5 - 3 THEM
S2
```

No point is awarded.

#### Second server loses rally

Before:

``` text
YOU 5 - 3 THEM
S2
```

After:

``` text
THEM 3 - 5 YOU
S1
```

The team order swaps because the serving team is always displayed first.

------------------------------------------------------------------------

## 5. Initial Serve Exception

Standard doubles pickleball gives the opening team only one server
before the first side-out.

This complexity should be handled internally rather than exposed through
additional UI.

Initial state:

``` text
openingServe = true
serverNumber = 2
```

Display:

``` text
YOU 0 - 0 THEM
S2
```

If the opening serving team loses the rally:

``` text
THEM 0 - 0 YOU
S1
```

After this first side-out, normal `S1 → S2 → side-out` behavior applies.

------------------------------------------------------------------------

## 6. Match Setup

Setup should require as few interactions as possible.

Recommended initial screen:

``` text
0     0

WHO SERVES?

← THEM   YOU →
```

A single directional gesture selects the initial serving team and begins
the match.

Alternative flow:

``` text
PICKLEBALL
    ↓
DOUBLES
    ↓
YOU SERVE FIRST?

← NO      YES →
```

The one-screen service selection is preferred for the MVP.

------------------------------------------------------------------------

## 7. Undo

Double tap restores the immediately preceding `MatchState`.

Conceptually:

``` text
DOUBLE_TAP
    │
    ▼
history.pop()
    │
    ▼
restore previous state
    │
    ▼
persist
    │
    ▼
refresh display
```

Undo should restore all relevant state, including:

-   Scores
-   Serving team
-   Server number
-   Opening-serve state
-   Game-over state

Multiple consecutive undo operations may be supported by retaining a
small history stack.

The storage requirement is negligible.

------------------------------------------------------------------------

## 8. Reset

Long press opens a confirmation state:

``` text
RESET GAME?

← CANCEL
→ RESET
```

Reset should return the application to pre-match setup rather than
silently beginning another game with the previous serving configuration.

Accidental long presses must not immediately destroy match state.

------------------------------------------------------------------------

## 9. Interaction Feedback

Every accepted gesture should produce immediate, subtle visual feedback.

### Point awarded

Example:

``` text
5 → 6
```

The changed score can briefly enlarge, brighten, or otherwise emphasize
the new value.

### Server transition

Example:

``` text
① → ②
```

The server indicator should briefly animate or highlight.

### Side-out

The left/right score swap itself provides strong visual feedback:

``` text
YOU 6  | THEM 4

       ↓

THEM 4 | YOU 6
```

Normal scoring should avoid dialogs, sounds, or persistent
notifications.

------------------------------------------------------------------------

## 10. Game Completion

Default game rules:

-   First to 11
-   Must win by 2

Game completion condition:

``` text
score >= 11
AND
abs(teamA.score - teamB.score) >= 2
```

Completion screen:

``` text
YOU WIN

11 - 7

Double tap: New Game
Hold: Exit
```

Exact post-game gestures can be refined during implementation.

### Future scoring modes

The scoring engine should be structured so alternative targets can
eventually be supported:

-   11, win by 2
-   15, win by 2
-   21, win by 2

These belong in configuration rather than the active-match UI.

------------------------------------------------------------------------

## 11. Singles Support

The scoring engine should accommodate singles even if the first UI
release focuses on doubles.

Singles eliminates the server-number state.

### Forward swipe

``` text
servingTeam.score++
```

### Backward swipe

``` text
servingTeam = receivingTeam
```

No `S1` / `S2` transition is necessary.

Separating scoring rules from presentation makes this inexpensive to
support later.

------------------------------------------------------------------------

## 12. Persistence

The complete `MatchState` should be persisted locally after every
accepted scoring gesture.

This protects against:

-   Activity termination
-   Process termination
-   Accidental application closure
-   Glass reboot
-   Battery loss

On startup, if an unfinished match exists:

``` text
RESUME GAME?

5 - 7
```

The user should be able to resume or discard the previous match.

No network connection should be required.

------------------------------------------------------------------------

## 13. Architecture

The MVP should be fully standalone on Google Glass.

No phone companion, account, network service, Meridian integration, or
external server is required.

``` text
┌─────────────────────┐
│ Glass Touchpad      │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Gesture Controller  │
│                     │
│ forward             │
│ backward            │
│ double tap          │
│ long press          │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Match Engine        │
│                     │
│ score               │
│ serving team        │
│ server #            │
│ opening serve       │
│ win detection       │
│ history / undo      │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Persistence Layer   │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Scoreboard View     │
│                     │
│ SERVING | RECEIVING │
│    5         7      │
│         ②           │
└─────────────────────┘
```

### Component responsibilities

#### Gesture Controller

Translates Glass touchpad events into semantic application actions.

It should not contain scoring logic.

#### Match Engine

Pure match-state transition logic.

Given:

``` text
current state + action
```

it should produce:

``` text
new state
```

Keeping this layer independent of Android UI APIs makes the scoring
rules straightforward to unit test.

#### Persistence Layer

Serializes the latest match state and history locally.

#### Scoreboard View

Renders state but does not determine scoring behavior.

------------------------------------------------------------------------

## 14. Recommended Action Model

Rather than allowing the gesture layer to directly manipulate state,
define semantic actions:

``` text
RALLY_WON
RALLY_LOST
UNDO
RESET
START_MATCH
```

The Glass-specific gesture mappings then become:

``` text
SWIPE_FORWARD  → RALLY_WON
SWIPE_BACKWARD → RALLY_LOST
DOUBLE_TAP     → UNDO
LONG_PRESS     → RESET
```

This separation makes it possible to change gestures later without
modifying the scoring engine.

------------------------------------------------------------------------

## 15. MVP Acceptance Criteria

The MVP is complete when a player can run an entire regulation doubles
game without removing Google Glass.

1.  User chooses which team serves first.
2.  Score begins at `0–0`.
3.  Forward swipe awards exactly one point to the serving team.
4.  Winning a point does not change the current server.
5.  Backward swipe from Server 1 changes to Server 2 without changing
    score.
6.  Backward swipe from Server 2 transfers service and resets the server
    to Server 1.
7.  The opening team's first lost rally immediately transfers service.
8.  The serving team is always rendered on the left.
9.  Double tap restores the immediately preceding match state.
10. Long press exposes reset confirmation.
11. Reset cannot occur accidentally from a single long press without
    confirmation.
12. The game detects 11 points with a two-point winning margin.
13. Match state is persisted after every accepted scoring action.
14. An interrupted match can be resumed after application/process
    restart.
15. The complete match can be operated without a network connection.
16. Scoring behavior can be unit tested independently of the
    Android/Glass UI.

------------------------------------------------------------------------

## 16. Design Principles

### Glanceability

The user should be able to determine the current score, serving team,
and server number with a very short glance.

### Spatial possession

The left side always represents the serving team. Service possession
therefore does not require a large dedicated indicator.

### Minimal interaction vocabulary

Normal play requires only two gestures:

-   Forward = serving side won rally
-   Backward = serving side lost rally

Undo and reset are secondary interactions.

### State-machine correctness

Pickleball scoring rules should be encoded explicitly rather than
inferred from UI state.

### Offline-first

Core match tracking has no dependency on connectivity or external
infrastructure.

### Recoverability

Mistakes and interruptions should be inexpensive:

-   Double tap → undo mistake
-   Persist after every action → recover interrupted game

------------------------------------------------------------------------

## 17. Deferred / Future Features

The following are intentionally outside the MVP but should remain
architecturally possible:

-   Singles mode
-   Games to 15 or 21
-   Best-of-three match tracking
-   Match history
-   Game duration
-   Rally count
-   Serve statistics
-   Side-out statistics
-   Team/player naming
-   Voice feedback
-   Phone companion
-   Export
-   Meridian integration
-   Automatic synchronization
-   Tournament scoring presets
-   Optional audio/haptic feedback where supported

These features should not complicate the active-match interaction model.
