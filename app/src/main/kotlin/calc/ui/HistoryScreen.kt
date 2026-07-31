package calc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import calc.model.HistoryEntry
import neoncore.theme.NeonAccent
import neoncore.theme.NeonDark
import neoncore.theme.NeonShape
import neoncore.theme.NeonSpacing
import neoncore.theme.NeonType

/**
 * The History Screen (CALC-UI-01 §8): header, then a scrollable list
 * of history cards, newest first.
 *
 * Built strictly to §8 as written — history cards only. Pins are
 * deliberately NOT surfaced here (see the "Open issue" notes now
 * inline in CALC-UI-01.md at §7/§8): §8 says nothing about how pinned
 * results should be displayed, and §7's long-press menu lists "Pin"
 * and "Save" as separate actions with no data-model distinction
 * between them (`PinnedRepository` has only one `insert()`).
 * `HistoryViewModel.pinned` exists and is deliberately unused by this
 * composable — dormant infrastructure until a future session resolves
 * that gap explicitly.
 *
 * Pinch-to-condense (§8's third gesture) is also not implemented here
 * — deferred to the later gesture-handling batch, per the priority
 * order set this session (a multi-touch interaction with no further
 * spec detail on what "condensed" means visually, unlike tap/swipe
 * which are simple and already fully specified).
 *
 * @param history Newest-first list, sourced from
 *   `HistoryViewModel.history` (already ordered by `HistoryDao.getAll()`
 *   — nothing here re-sorts it).
 * @param onLoadEntry Tap → loads a calculation. Callers wire this to
 *   `calcViewModel::loadFromHistory` — this composable holds no
 *   `CalcViewModel` reference directly (cross-ViewModel-boundary
 *   pattern already established in this project, e.g.
 *   `HistoryViewModel`'s own doc comment on the tap gesture).
 * @param onDeleteEntry Swipe → delete. Callers wire this to
 *   `historyViewModel::deleteEntry`. Swipe-to-delete is intrinsic to
 *   how a history card works (explicit person decision this session),
 *   not deferred to the later gesture-handling pass the way
 *   swipe-down-to-open/swipe-left-backspace are.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    history: List<HistoryEntry>,
    onLoadEntry: (HistoryEntry) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Header: "CALC" / "History" — same title/subtitle visual
        // relationship as StatusHeader's moduleName/subtitle, but this
        // screen doesn't use StatusHeader itself since HISTORY OPEN is
        // shown in the Home Screen's header (CalcScreen owns that, via
        // CalcScreenStatus) rather than duplicated here.
        Column(
            modifier = Modifier.padding(
                top = NeonSpacing.MarginTopSafeArea,
                start = NeonSpacing.MarginHorizontal,
                end = NeonSpacing.MarginHorizontal,
                bottom = NeonSpacing.Small
            )
        ) {
            Text(
                text = "CALC",
                style = NeonType.Title,
                color = NeonDark.TextPrimary
            )
            Text(
                text = "History",
                style = NeonType.Secondary,
                color = NeonDark.TextSecondary
            )
        }

        if (history.isEmpty()) {
            HistoryEmptyState(modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = NeonSpacing.MarginHorizontal,
                    end = NeonSpacing.MarginHorizontal,
                    bottom = NeonSpacing.Medium
                ),
                verticalArrangement = Arrangement.spacedBy(NeonSpacing.Tight)
            ) {
                items(history, key = { it.id }) { entry ->
                    HistoryCard(
                        entry = entry,
                        onTap = { onLoadEntry(entry) },
                        onDelete = { onDeleteEntry(entry.id) }
                    )
                }
            }
        }
    }
}

/**
 * A single history card with swipe-to-delete, per §8: 72dp height,
 * 20dp radius (`NeonShape.Card` — confirmed exact match against
 * `neon-core`'s `Shape.kt`, reused directly rather than redefined).
 * Expression (smaller/muted) above result (larger/primary) — same
 * visual relationship as the main Display area (CALC-UI-01 §5), just
 * smaller and inside a card.
 *
 * Uses Material3's `SwipeToDismissBox` + `rememberSwipeToDismissBoxState`
 * — the current, non-deprecated API (confirmed via live search before
 * writing this; the older `SwipeToDismiss` composable is deprecated).
 * This is the first use of `@OptIn(ExperimentalMaterial3Api::class)`
 * anywhere in this project (confirmed via grep before writing this —
 * no prior usage exists). No new Gradle dependency needed:
 * `androidx.compose.material3:material3` is already an
 * `app/build.gradle.kts` dependency.
 *
 * `SwipeToDismissBoxState` manages its own drag/settle animation
 * timing via `AnchoredDraggableState` internally — no custom
 * `NeonMotion` duration is applied here, since the component doesn't
 * expose a seam for one and its default feel is already appropriately
 * quick/mechanical for this UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryCard(
    entry: HistoryEntry,
    onTap: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    val cardInteractionSource = remember { MutableInteractionSource() }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(NeonShape.Card)
                    .background(NeonAccent.Red),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "Delete",
                    style = NeonType.Body,
                    color = NeonDark.TextPrimary,
                    modifier = Modifier.padding(horizontal = NeonSpacing.Small)
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(NeonShape.Card)
                .background(NeonDark.Card)
                .clickable(
                    interactionSource = cardInteractionSource,
                    indication = null,
                    onClickLabel = "Load calculation"
                ) { onTap() }
                .padding(horizontal = NeonSpacing.Small),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = entry.expression,
                    fontSize = 14.sp,
                    color = NeonDark.TextMuted,
                    textAlign = TextAlign.Start
                )
                Text(
                    text = entry.result,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonDark.TextPrimary,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

/**
 * Empty state for this screen. CALC-UI-01 §12 ("Empty State") only
 * describes the Home Screen's fresh-launch case (module header +
 * "Enter an expression" / "No fake history.") — it does not define a
 * dedicated History Screen empty case. Re-read in full per the
 * handoff's instruction before building this; confirmed there's
 * nothing more specific to reuse here beyond §12's underlying
 * principle ("No fake history" — never fabricate placeholder entries).
 * Wording below is this screen's own equivalent of that principle,
 * not a verbatim spec string, since none exists for this screen.
 */
@Composable
private fun HistoryEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No history yet",
                style = NeonType.Body,
                color = NeonDark.TextSecondary
            )
            Text(
                text = "Completed calculations will appear here",
                style = NeonType.Secondary,
                color = NeonDark.TextMuted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
