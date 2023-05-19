package com.pokerio.app.utils

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.pokerio.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import java.io.IOException
import java.net.URL
import java.security.MessageDigest
import kotlin.jvm.Throws

// This is an object - a static object if you will that will exist in the global context. There
// will always be one and only one instance of this object
object GameState {
    // Class fields
    var gameID = ""
    val players = mutableListOf<Player>()
    var startingFunds: Int = -1
    var smallBlind: Int = -1
    var isPlayerAdmin: Boolean = false
    var gameCard1: GameCard? = null
    var gameCard2: GameCard? = null

    // Callbacks
    var onGameReset = {}
    var onGameStart = {}
    private val playerJoinedCallbacks = HashMap<Int, (Player) -> Unit>()
    private val playerRemovedCallbacks = HashMap<Int, (Player) -> Unit>()
    private var settingsChangedCallback = HashMap<Int, () -> Unit>()
    private var nextId = 0

    // Constants
    private const val BASE_URL = "http://192.168.86.30:42069"
    const val STARTING_FUNDS_DEFAULT = 1000
    const val SMALL_BLIND_DEFAULT = 100
    const val MAX_PLAYERS = 8

    // Methods
    fun launchTask(task: suspend () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            task()
        }
    }

    suspend fun createGameRequest(
        context: Context,
        onSuccess: () -> Unit,
        onError: () -> Unit,
        baseUrl: String = BASE_URL,
        firebaseId: String? = null
    ) {
        // Get all values
        val sharedPreferences = context.getSharedPreferences(
            context.getString(R.string.shared_preferences_file),
            Context.MODE_PRIVATE
        )

        val nickname = sharedPreferences.getString(
            context.getString(R.string.sharedPreferences_nickname),
            "Player"
        ) ?: "Player"

        val preferredSmallBlind = sharedPreferences.getInt(
            context.getString(R.string.sharedPreferences_small_blind),
            SMALL_BLIND_DEFAULT
        )

        val preferredStartingFunds = sharedPreferences.getInt(
            context.getString(R.string.sharedPreferences_starting_funds),
            STARTING_FUNDS_DEFAULT
        )

        // Make request
        try {
            val creatorID = firebaseId ?: FirebaseMessaging.getInstance().token.await()
            // Prepare url
            val urlString =
                "/createGame?creatorToken=$creatorID&nickname=$nickname" +
                    "&smallBlind=$preferredSmallBlind&startingFunds=$preferredStartingFunds"
            val url = URL(baseUrl + urlString)

            val responseJson = url.readText()
            val responseObject =
                Json.decodeFromString(CreateGameResponseSerializer, responseJson)

            gameID = responseObject.gameId.toString()
            startingFunds = responseObject.startingFunds
            smallBlind = responseObject.smallBlind

            // We have to add the creator to the list of players
            players.clear()
            addPlayer(Player(nickname, creatorID, true))

            isPlayerAdmin = true
            onSuccess()
        } catch (e: Exception) {
            PokerioLogger.error("Failed to create game, reason: $e")
            onError()
        }
    }

    suspend fun joinGameRequest(
        gameID: String,
        context: Context,
        onSuccess: () -> Unit,
        onError: () -> Unit,
        baseUrl: String = BASE_URL,
        firebaseId: String? = null
    ) {
        // Get all values
        val sharedPreferences = context.getSharedPreferences(
            context.getString(R.string.shared_preferences_file),
            Context.MODE_PRIVATE
        )

        val nickname = sharedPreferences.getString(
            context.getString(R.string.sharedPreferences_nickname),
            "Player"
        ) ?: "Player"

        // Make request
        try {
            val playerID = firebaseId ?: FirebaseMessaging.getInstance().token.await()
            // Prepare url
            val urlString = "/joinGame?nickname=$nickname&playerToken=$playerID&gameId=$gameID"
            val url = URL(baseUrl + urlString)

            val responseJson = url.readText()

            this.gameID = gameID

            val joinGameResponse = Json.decodeFromString(JoinGameResponseSerializer, responseJson)
            startingFunds = joinGameResponse.startingFunds
            smallBlind = joinGameResponse.smallBlind
            val gameMasterHash = joinGameResponse.gameMasterHash

            joinGameResponse.players.forEach { jsonElement ->
                val playerResponse = Json.decodeFromString(PlayerResponseSerializer, jsonElement.toString())
                val newPlayer = Player(
                    playerResponse.nickname,
                    playerResponse.playerHash,
                    playerResponse.playerHash == gameMasterHash
                )

                addPlayer(newPlayer)
            }

            // This player is not included in the player list, so we need to add them separately
            addPlayer(Player(nickname, playerID))

            onSuccess()
        } catch (e: Exception) {
            PokerioLogger.error("Failed to join game, reason: $e")
            onError()
        }
    }

    suspend fun modifyGameRequest(
        smallBlind: Int,
        startingFunds: Int,
        onSuccess: () -> Unit,
        onError: () -> Unit,
        baseUrl: String = BASE_URL,
        firebaseId: String? = null
    ) {
        try {
            val playerID = firebaseId ?: FirebaseMessaging.getInstance().token.await()

            // Prepare url
            val urlString =
                "/modifyGame?creatorToken=$playerID&smallBlind=$smallBlind&startingFunds=$startingFunds"
            val url = URL(baseUrl + urlString)
            url.readText()
            onSuccess()
        } catch (e: IOException) {
            PokerioLogger.error("Failed to modify game, reason: $e")
            onError()
        }
    }

    suspend fun kickPlayerRequest(
        playerID: String,
        onSuccess: () -> Unit,
        onError: () -> Unit,
        baseUrl: String = BASE_URL,
        firebaseId: String? = null
    ) {
        try {
            val myID = firebaseId ?: FirebaseMessaging.getInstance().token.await()

            // Prepare url
            val urlString = "/kickPlayer?creatorToken=$myID&playerToken=$playerID"
            val url = URL(baseUrl + urlString)

            url.readText()

            onSuccess()
        } catch (e: IOException) {
            PokerioLogger.error("Failed to kick player, reason: $e")
            onError()
        }
    }

    suspend fun leaveGameRequest(
        onSuccess: () -> Unit,
        onError: () -> Unit,
        baseUrl: String = BASE_URL,
        firebaseId: String? = null
    ) {
        try {
            val myID = firebaseId ?: FirebaseMessaging.getInstance().token.await()

            // Prepare url
            val urlString = "/leaveGame?playerToken=$myID"
            val url = URL(baseUrl + urlString)

            url.readText()

            resetGameState()
            onSuccess()
        } catch (e: IOException) {
            PokerioLogger.error("Failed to leave game, reason: $e")
            onError()
        }
    }

    suspend fun startGameRequest(
        onSuccess: () -> Unit,
        onError: () -> Unit,
        baseUrl: String = BASE_URL,
        firebaseId: String? = null
    ) {
        try {
            val myID = firebaseId ?: FirebaseMessaging.getInstance().token.await()

            // Prepare url
            val urlString = "/startGame?creatorToken=$myID"
            val url = URL(baseUrl + urlString)

            url.readText()

            onSuccess()
        } catch (e: IOException) {
            PokerioLogger.error("Failed to start game, reason: $e")
            onError()
        }
    }

    fun resetGameState() {
        // Class fields
        gameID = ""
        players.clear()
        startingFunds = -1
        smallBlind = -1
        isPlayerAdmin = false
        gameCard1 = null
        gameCard2 = null
        // Callbacks
        playerJoinedCallbacks.clear()
        playerRemovedCallbacks.clear()
        // Not resetting nextId, because someone might be holding on to an old one and we don't
        // want then to remove new callbacks by mistake

        onGameReset()
    }

    fun addOnPlayerJoinedCallback(callback: (Player) -> Unit): Int {
        playerJoinedCallbacks[nextId] = callback
        return nextId++
    }

    fun removeOnPlayerJoinedCallback(id: Int) {
        playerJoinedCallbacks.remove(id)
    }

    fun addOnPlayerRemovedCallback(callback: (Player) -> Unit): Int {
        playerRemovedCallbacks[nextId] = callback
        return nextId++
    }

    fun removeOnPlayerRemovedCallback(id: Int) {
        playerRemovedCallbacks.remove(id)
    }

    fun addOnSettingsChangedCallback(callback: () -> Unit): Int {
        settingsChangedCallback[nextId] = callback
        return nextId++
    }

    fun removeOnSettingsChangedCallback(id: Int) {
        settingsChangedCallback.remove(id)
    }

    fun addPlayer(player: Player) {
        players.add(player)
        playerJoinedCallbacks.forEach { it.value(player) }
    }

    @Throws(IllegalArgumentException::class)
    fun removePlayer(playerHash: String, newAdmin: String? = null) {
        if (!isInGame()) {
            return
        }

        val isThisPlayerRemoved = players.find { sha256(it.playerID) == playerHash } != null
        if (isThisPlayerRemoved) {
            return resetGameState()
        }

        val removedPlayer = players.find { it.playerID == playerHash }
        require(removedPlayer != null)

        if (removedPlayer.isAdmin) {
            require(newAdmin != null)
            val thisPlayerNewAdmin = players.find { sha256(it.playerID) == newAdmin }

            if (thisPlayerNewAdmin != null) {
                isPlayerAdmin = true
                thisPlayerNewAdmin.isAdmin = true
            } else {
                val newAdminPlayer = players.find { it.playerID == newAdmin }
                newAdminPlayer!!.isAdmin = true
            }
        }

        players.remove(removedPlayer)
        playerRemovedCallbacks.forEach { it.value(removedPlayer) }
    }

    fun startGame(data: Map<String, String>) {
        gameCard1 = GameCard(data["card1"]!!.slice(0..1), data["card1"]!!.slice(2..2))
        gameCard2 = GameCard(data["card2"]!!.slice(0..1), data["card2"]!!.slice(2..2))
        players.forEach { it.funds = startingFunds }

        onGameStart()
    }

    fun sha256(string: String): String {
        return MessageDigest
            .getInstance("SHA-256")
            .digest(string.toByteArray())
            .fold("") { str, byte -> str + "%02x".format(byte) }
    }

    fun changeGameSettings(newStartingFunds: Int, newSmallBlind: Int) {
        startingFunds = newStartingFunds
        smallBlind = newSmallBlind
        settingsChangedCallback.forEach { it.value() }
    }

    fun isInGame(): Boolean {
        return gameID.isNotBlank()
    }
}

data class CreateGameResponse(
    val gameId: Int,
    val startingFunds: Int,
    val smallBlind: Int
)

@Generated
@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = CreateGameResponse::class)
object CreateGameResponseSerializer

data class PlayerResponse(
    val nickname: String,
    val playerHash: String,
    val turn: Int
)

@Generated
@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = PlayerResponse::class)
object PlayerResponseSerializer

data class JoinGameResponse(
    val gameMasterHash: String,
    val startingFunds: Int,
    val smallBlind: Int,
    val players: JsonArray
)

@Generated
@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = JoinGameResponse::class)
object JoinGameResponseSerializer
