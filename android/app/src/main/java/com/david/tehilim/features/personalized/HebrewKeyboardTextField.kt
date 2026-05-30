package com.david.tehilim.features.personalized

import android.graphics.Color
import android.os.LocaleList
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.david.tehilim.core.service.HebrewLetterMapper
import java.util.Locale

/**
 * Action IME du clavier à afficher en bas-droite : flèche "Suivant" pour
 * enchaîner vers le champ suivant, ou ✓ "Terminé" pour fermer le clavier.
 */
enum class HebrewFieldImeAction { Next, Done }

/**
 * `TextField` qui force Android à proposer le clavier hébreu via
 * `imeHintLocales`. Équivalent du `HebrewKeyboardTextField` iOS V1.10.1.
 *
 * Pour ça on utilise un `AndroidView` qui wrap un `EditText` natif :
 * `imeHintLocales` n'est pas exposé par les Composables Compose en 1.7.x.
 *
 * Si l'utilisateur a un clavier hébreu installé (Gboard avec Hébreu activé
 * dans Réglages → Système → Langues), Android bascule automatiquement sur
 * la disposition hébraïque quand le champ devient focused.
 *
 * Sinon le clavier reste celui par défaut — le filtre live `filterHebrew`
 * rejette toute saisie non-hébraïque dans tous les cas.
 *
 * V1.4 — `imeAction` + callback `onImeAction` pour permettre au formulaire
 * de chaîner les champs (Next sur le 1er → focus le 2e ; Done sur le 2e →
 * ferme le clavier). Combiné à `focusRequester`, le formulaire pilote
 * complètement le parcours utilisateur sans clavier intrusif.
 */
@Composable
fun HebrewKeyboardTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    imeAction: HebrewFieldImeAction = HebrewFieldImeAction.Done,
    focusRequester: FocusRequester? = null,
    onImeAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.primary

    val ownFocusRequester = focusRequester ?: remember { FocusRequester() }

    androidx.compose.foundation.layout.Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Label aligné à DROITE — cohérent avec le sens d'écriture hébreu
        // du champ en-dessous (V1.4).
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp, bottom = 4.dp)
        )

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .focusRequester(ownFocusRequester)
                .border(1.dp, outline, RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            factory = { ctx ->
                EditText(ctx).apply {
                    // Apparence : aligné à droite (RTL), pas de background natif,
                    // taille raisonnable.
                    background = null
                    // V1.12 — `Gravity.RIGHT` ABSOLU (et non `Gravity.END`, qui
                    // se résout à GAUCHE quand la direction de layout résolue est
                    // RTL) : le champ ET le curseur restent ancrés à droite pour
                    // la saisie hébraïque, y compris champ vide.
                    gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    setTextColor(onSurface.toArgb())
                    setHintTextColor(onSurfaceVariant.toArgb())
                    hint = placeholder
                    isSingleLine = true

                    // Désactive corrections automatiques + suggestions (le filtre
                    // Hebrew agit déjà).
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

                    // Action IME : Next pour enchaîner, Done pour valider/fermer.
                    imeOptions = when (imeAction) {
                        HebrewFieldImeAction.Next -> EditorInfo.IME_ACTION_NEXT
                        HebrewFieldImeAction.Done -> EditorInfo.IME_ACTION_DONE
                    }
                    setOnEditorActionListener { _, actionId, _ ->
                        when (actionId) {
                            EditorInfo.IME_ACTION_NEXT,
                            EditorInfo.IME_ACTION_DONE -> {
                                onImeAction()
                                true
                            }
                            else -> false
                        }
                    }

                    // Hint locales = clavier hébreu si dispo.
                    // Équivalent direct du textInputMode iOS V1.10.1.
                    imeHintLocales = LocaleList(Locale.forLanguageTag("he"))

                    // Direction RTL native de l'EditText.
                    textDirection = View.TEXT_DIRECTION_RTL

                    // Initial value
                    setText(value)
                    setSelection(value.length)

                    // Listener : filtre live + propagation Compose
                    addTextChangedListener(object : TextWatcher {
                        var isUpdating = false
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                        override fun afterTextChanged(s: Editable?) {
                            if (isUpdating) return
                            val current = s?.toString().orEmpty()
                            val filtered = HebrewLetterMapper.filterHebrew(current)
                            if (filtered != current) {
                                isUpdating = true
                                setText(filtered)
                                setSelection(filtered.length)
                                isUpdating = false
                            }
                            onValueChange(filtered)
                        }
                    })
                }
            },
            update = { editText ->
                // Sync depuis Compose seulement si différent (évite les boucles)
                if (editText.text.toString() != value) {
                    editText.setText(value)
                    editText.setSelection(value.length)
                }
            }
        )
    }
}

/** Helper : `Color` Compose → Int ARGB. */
private fun androidx.compose.ui.graphics.Color.toArgb(): Int = Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt()
)
