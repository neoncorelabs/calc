package calc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import calc.ui.CalcScreen
import calc.viewmodel.AppContainer
import neoncore.theme.NeonCoreTheme

/**
 * App entry point. Nothing existed here before this batch — the
 * manifest had no launcher activity at all, meaning CI's
 * assembleDebug has been compiling a technically-valid but
 * unlaunchable app this whole time (nothing about the engine/data/
 * viewmodel layers required an Activity to compile). Adding this is
 * part of Batch H, not a separate concern, since it's the first batch
 * that actually needs something on screen.
 *
 * AppContainer is constructed once here, at the top of the Activity,
 * and handed down to CalcScreen — per AppContainer's own doc comment,
 * it must be built from an application Context, not an Activity
 * Context, to avoid leaking the Activity if AppContainer's lazy
 * properties outlive a configuration change. `applicationContext`
 * (not `this`) is passed for exactly that reason.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = AppContainer(applicationContext)

        setContent {
            NeonCoreTheme(darkTheme = true) {
                CalcScreen(container = container)
            }
        }
    }
}
