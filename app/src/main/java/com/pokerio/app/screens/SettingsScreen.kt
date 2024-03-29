package com.pokerio.app.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.pokerio.app.BuildConfig
import com.pokerio.app.R
import com.pokerio.app.utils.GameState
import com.pokerio.app.utils.IntUnitProvider
import com.pokerio.app.utils.Player
import com.pokerio.app.utils.PokerioLogger
import com.pokerio.app.utils.UnitUnitProvider
import java.lang.Float.max
import java.lang.Float.min

const val MAX_SMALL_BLIND_MODIFIER = 0.4f
const val DIFF_MAX = 1000
const val DIFF_MID = 100
const val DIFF_MIN = 10

const val APP_ICON_SCALE = 1.5F
val APP_ICON_SIZE = 100.dp

val CONTENT_PADDING = 10.dp
val SECTION_TITLE_FONT_SIZE = 24.sp
val SECTION_TITLE_FONT_WEIGHT = FontWeight.Bold
val SECTION_TITLE_MODIFIER = Modifier.padding(10.dp)
val SPACER_MODIFIER = Modifier.padding(10.dp)

@Preview
@Composable
fun SettingsScreen(
    @PreviewParameter(UnitUnitProvider::class) navigateBack: () -> Unit
) {
    val context = LocalContext.current

    var smallBlind by remember { mutableStateOf(getInitialSmallBlind(context)) }
    val onSmallBlindUpdate = { newValue: Int ->
        smallBlind = newValue
    }

    var startingFunds by remember { mutableStateOf(getInitialStartingFunds(context)) }
    val onStartingFundsUpdate = { newValue: Int ->
        startingFunds = newValue
        if (startingFunds * MAX_SMALL_BLIND_MODIFIER < smallBlind) {
            onSmallBlindUpdate((startingFunds * MAX_SMALL_BLIND_MODIFIER).toInt())
        }
    }

    val onNavigateBack = {
        // Unregister callback when we leave the view
        val onError = {
            ContextCompat.getMainExecutor(context).execute {
                Toast.makeText(
                    context,
                    context.getString(R.string.failed_update),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        val onSuccess = {
            PokerioLogger.debug("Successfully updated settings")
        }

        // Notify server about changes if we were in game
        if (GameState.isInGame()) {
            GameState.launchTask {
                GameState.modifyGameRequest(
                    smallBlind,
                    startingFunds,
                    onSuccess,
                    onError
                )
            }
        }

        navigateBack()
    }

    BackHandler {
        onNavigateBack()
    }

    Column {
        TopBar(onNavigateBack)
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(CONTENT_PADDING)
        ) {
            AppLogo()
            Spacer(modifier = SPACER_MODIFIER)
            NicknameEditor()
            Spacer(modifier = SPACER_MODIFIER)
            StartingFundsSelector(startingFunds, onStartingFundsUpdate)
            Spacer(modifier = SPACER_MODIFIER)
            SmallBlindSelector(smallBlind, startingFunds, onSmallBlindUpdate)
            Credits()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onNavigateBack: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = { Text(stringResource(id = R.string.settings)) },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        navigationIcon = {
            IconButton(
                onClick = { onNavigateBack() },
                modifier = Modifier.testTag("settings_back")
            ) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = stringResource(
                        id = R.string.contentDescription_navigate_back
                    )
                )
            }
        }
    )
}

@Composable
private fun AppLogo() {
    if (GameState.isInGame()) {
        return
    }

    Column(
        modifier = Modifier
            .padding(top = CONTENT_PADDING)
            .fillMaxWidth()
            .testTag("app_logo_section"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ElevatedCard(
            shape = CircleShape,
            colors = CardDefaults.elevatedCardColors(
                containerColor = Color.White
            ),
            modifier = Modifier.size(APP_ICON_SIZE)
        ) {
            Box(modifier = Modifier.scale(APP_ICON_SCALE)) {
                Image(
                    painterResource(R.drawable.ic_launcher_foreground),
                    stringResource(R.string.contentDescription_appLogo),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Text(
            text = stringResource(id = R.string.app_name),
            fontSize = SECTION_TITLE_FONT_SIZE,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = BuildConfig.VERSION_NAME,
            color = Color.Gray,
            fontWeight = FontWeight.Light,
            modifier = Modifier.testTag("app_version_name")
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NicknameEditor() {
    if (GameState.isInGame()) {
        return
    }

    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences(
        stringResource(id = R.string.shared_preferences_file),
        Context.MODE_PRIVATE
    )

    val nicknameSharedKey = stringResource(id = R.string.sharedPreferences_nickname)
    var nickname by remember { mutableStateOf(getInitialNickname(context)) }
    var nicknameCorrect by remember { mutableStateOf(true) }
    val onNicknameUpdate = { newValue: String ->
        nickname = newValue
        nicknameCorrect = Player.validateNickname(nickname)
    }

    DisposableEffect(LocalLifecycleOwner.current) {
        onDispose {
            nickname = Player.fixNickname(nickname)

            with(sharedPreferences.edit()) {
                if (nicknameCorrect) {
                    putString(nicknameSharedKey, nickname)
                }
                apply()
            }
        }
    }
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_nickname"),
        value = nickname,
        onValueChange = { onNicknameUpdate(it) },
        label = { Text(stringResource(id = R.string.nickname)) },
        isError = !nicknameCorrect,
        supportingText = {
            if (!nicknameCorrect) {
                Text(stringResource(id = R.string.nickname_error))
            }
        },
        singleLine = true
    )
}

@Composable
private fun StartingFundsSelector(
    startingFunds: Int,
    onStartingFundsUpdate: (Int) -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences(
        stringResource(id = R.string.shared_preferences_file),
        Context.MODE_PRIVATE
    )

    val startingFundsSharedKey = stringResource(id = R.string.sharedPreferences_starting_funds)

    DisposableEffect(LocalLifecycleOwner.current) {
        onDispose {
            with(sharedPreferences.edit()) {
                putInt(startingFundsSharedKey, startingFunds)
                apply()
            }
        }
    }
    Text(
        text = stringResource(id = R.string.starting_funds),
        fontSize = SECTION_TITLE_FONT_SIZE,
        fontWeight = SECTION_TITLE_FONT_WEIGHT,
        modifier = SECTION_TITLE_MODIFIER
    )
    Selector(
        onValueSelected = { onStartingFundsUpdate(it) },
        minValue = 100f,
        maxValue = 10000f,
        initialValue = startingFunds.toFloat()
    )
}

@Composable
private fun SmallBlindSelector(
    smallBlind: Int,
    startingFunds: Int,
    onSmallBlindUpdate: (Int) -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences(
        stringResource(id = R.string.shared_preferences_file),
        Context.MODE_PRIVATE
    )

    val smallBlindSharedKey = stringResource(id = R.string.sharedPreferences_small_blind)

    DisposableEffect(LocalLifecycleOwner.current) {
        onDispose {
            with(sharedPreferences.edit()) {
                putInt(smallBlindSharedKey, smallBlind)
                apply()
            }
        }
    }
    Text(
        text = stringResource(id = R.string.small_blind),
        fontSize = SECTION_TITLE_FONT_SIZE,
        fontWeight = SECTION_TITLE_FONT_WEIGHT,
        modifier = SECTION_TITLE_MODIFIER
    )
    Selector(
        onValueSelected = { onSmallBlindUpdate(it) },
        minValue = 10f,
        maxValue = startingFunds * MAX_SMALL_BLIND_MODIFIER,
        initialValue = smallBlind.toFloat()
    )
}

@Composable
private fun Credits() {
    if (GameState.isInGame()) {
        return
    }

    val uriHandler = LocalUriHandler.current
    val camaraLink = stringResource(R.string.camara_link)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.camara_credit),
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
            fontStyle = FontStyle.Italic,
            color = Color.Gray
        )
        ClickableText(
            text = buildLink(camaraLink, MaterialTheme.colorScheme.primary),
            onClick = {
                uriHandler.openUri(camaraLink)
            }
        )
    }
}

@Composable
fun Selector(
    minValue: Float = 0f,
    maxValue: Float = 100f,
    initialValue: Float = 50f,
    @PreviewParameter(IntUnitProvider::class) onValueSelected: (value: Int) -> Unit
) {
    var currentValue by remember { mutableStateOf(initialValue) }
    currentValue = minOf(currentValue, maxValue)

    fun updateValue(newValue: Float) {
        currentValue = min(max(newValue, minValue), maxValue)
        onValueSelected(currentValue.toInt())
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SliderButtonColumn(
                currentValue = currentValue,
                updateValue = { updateValue(it) },
                sign = '-',
                operand = Float::minus
            )
            Text(
                text = currentValue.toInt().toString(),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("slider_text")
            )
            SliderButtonColumn(
                currentValue = currentValue,
                updateValue = { updateValue(it) },
                sign = '+',
                operand = Float::plus
            )
        }
        Slider(
            value = (currentValue - minValue) / (maxValue - minValue),
            onValueChange = { updateValue(minValue + it * (maxValue - minValue)) },
            modifier = Modifier.testTag("selector_slider")
        )
    }
}

@Composable
private fun SliderButtonColumn(
    currentValue: Float,
    updateValue: (Float) -> Unit,
    sign: Char,
    operand: (Float, Int) -> Float
) {
    Column(modifier = Modifier.width(IntrinsicSize.Min)) {
        OutlinedButton(
            onClick = { updateValue(operand(currentValue, DIFF_MAX)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "$sign$DIFF_MAX")
        }
        OutlinedButton(
            onClick = { updateValue(operand(currentValue, DIFF_MID)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "$sign$DIFF_MID")
        }
        OutlinedButton(
            onClick = { updateValue(operand(currentValue, DIFF_MIN)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("slider${sign}10")
        ) {
            Text(text = "$sign$DIFF_MIN")
        }
    }
}

private fun buildLink(
    link: String,
    color: Color,
    sp: TextUnit = 10.sp
): AnnotatedString = buildAnnotatedString {
    pushStringAnnotation(tag = link, annotation = link)
    withStyle(
        style = SpanStyle(color = color, textDecoration = TextDecoration.Underline, fontSize = sp)
    ) {
        append(link)
    }
    pop()
}

private fun getInitialNickname(context: Context): String {
    val sharedPreferences = context.getSharedPreferences(
        context.getString(R.string.shared_preferences_file),
        Context.MODE_PRIVATE
    )

    // We can use a non-null assertion because we passed a non-null default value
    return sharedPreferences.getString(
        context.getString(R.string.sharedPreferences_nickname),
        "Player"
    )!!
}

private fun getInitialStartingFunds(context: Context): Int {
    val sharedPreferences = context.getSharedPreferences(
        context.getString(R.string.shared_preferences_file),
        Context.MODE_PRIVATE
    )

    return sharedPreferences.getInt(
        context.getString(R.string.sharedPreferences_starting_funds),
        GameState.STARTING_FUNDS_DEFAULT
    )
}

private fun getInitialSmallBlind(context: Context): Int {
    val sharedPreferences = context.getSharedPreferences(
        context.getString(R.string.shared_preferences_file),
        Context.MODE_PRIVATE
    )

    return sharedPreferences.getInt(
        context.getString(R.string.sharedPreferences_small_blind),
        GameState.SMALL_BLIND_DEFAULT
    )
}
