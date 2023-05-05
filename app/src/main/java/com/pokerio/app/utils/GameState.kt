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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URL
import java.security.MessageDigest

// This is an object - a static object if you will that will exist in the global context. There
// will always be one and only one instance of this object
object GameState {
    // Class fields
    var gameID = ""
    val players = mutableListOf<Player>()
    var startingFunds: Int = -1
    var smallBlind: Int = -1
    var isPlayerAdmin: Boolean = false
    var card1: Card? = null
    var card2: Card? = null

    // Callbacks
    var onGameReset = {}
    var onGameStart = {}
    private val playerJoinedCallbacks = HashMap<Int, (Player) -> Unit>()
    private val playerRemovedCallbacks = HashMap<Int, (Player) -> Unit>()
    private var settingsChangedCallback = HashMap<Int, () -> Unit>()
    private var nextId = 0

    // Constants
    private const val BASE_URL = "http://158.101.160.143:42069"
    private val networkCoroutine = CoroutineScope(Dispatchers.IO)

    // Methods

    // This method makes a request to create a game and sets the field of the GameState object on
    // success and returns true. Returns false if something goes wrong.
    fun createGameRequest(
        context: Context,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        networkCoroutine.launch {
            createGameRequestSuspend(context, onSuccess, onError)
        }
    }

    suspend fun createGameRequestSuspend(
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
            1000
        )

        val preferredStartingFunds = sharedPreferences.getInt(
            context.getString(R.string.sharedPreferences_starting_funds),
            100
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

            gameID = responseObject.gameKey.toString()
            startingFunds = responseObject.startingFunds
            smallBlind = responseObject.smallBlind

            // We have to add the creator to the list of players
            players.clear()
            addPlayer(Player(nickname, creatorID, true))

            isPlayerAdmin = true
            onSuccess()
        } catch (e: Exception) {
            PokerioLogger.error(e.toString())
            onError()
        }
    }

    fun joinGameRequest(
        gameID: String,
        context: Context,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        networkCoroutine.launch {
            joinGameRequestSuspend(gameID, context, onSuccess, onError)
        }
    }

    suspend fun joinGameRequestSuspend(
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
            val responseObject = Json.parseToJsonElement(responseJson).jsonObject

            this@GameState.gameID = gameID
            // TODO: This has to be parsed better
            startingFunds = responseObject["startingFunds"]!!.jsonPrimitive.content.toInt()
            smallBlind = responseObject["smallBlind"]!!.jsonPrimitive.content.toInt()

            val gameMasterHash = responseObject["gameMasterHash"]!!.jsonPrimitive.content

            responseObject["players"]!!.jsonArray.forEach {
                val playerNickname = it.jsonObject["nickname"]!!.jsonPrimitive.content
                val playerHash = it.jsonObject["playerHash"]!!.jsonPrimitive.content

                addPlayer(Player(playerNickname, playerHash, playerHash == gameMasterHash))
            }

            // This player is not included in the player list, so we need to add them separately
            addPlayer(Player(nickname, playerID))

            onSuccess()
        } catch (e: Exception) {
            e.printStackTrace()
            PokerioLogger.error(e.toString())
            onError()
        }
    }

    fun exitSettingsRequest(
        context: Context,
        onError: () -> Unit,
        onSuccess: () -> Unit,
        baseUrl: String = BASE_URL
    ) {
        if (isInGame()) {
            val sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.shared_preferences_file),
                Context.MODE_PRIVATE
            )

            val smallBlind = sharedPreferences.getInt(
                context.getString(R.string.sharedPreferences_small_blind),
                1000
            )

            val startingFunds = sharedPreferences.getInt(
                context.getString(R.string.sharedPreferences_starting_funds),
                100
            )

            networkCoroutine.launch {
                try {
                    val playerID = FirebaseMessaging.getInstance().token.await()

                    // Prepare url
                    val urlString =
                        "/modifyGame?creatorToken=$playerID&smallBlind=$smallBlind&startingFunds=$startingFunds"
                    val url = URL(baseUrl + urlString)
                    url.readText()
                    onSuccess()
                } catch (e: Exception) {
                    e.printStackTrace()
                    PokerioLogger.error(e.toString())
                    onError()
                }
            }
        }
    }

    fun kickPlayerRequest(
        playerID: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        networkCoroutine.launch {
            kickPlayerRequestSuspend(playerID, onSuccess, onError)
        }
    }

    suspend fun kickPlayerRequestSuspend(
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
        } catch (e: Exception) {
            e.printStackTrace()
            PokerioLogger.error(e.toString())
            onError()
        }
    }

    fun leaveGameRequest(
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        networkCoroutine.launch {
            leaveGameRequestSuspend(onSuccess, onError)
        }
    }

    suspend fun leaveGameRequestSuspend(
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
        } catch (e: Exception) {
            e.printStackTrace()
            PokerioLogger.error(e.toString())
            onError()
        }
    }

    fun startGameRequest(
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        networkCoroutine.launch {
            startGameRequestSuspend(onSuccess, onError)
        }
    }

    suspend fun startGameRequestSuspend(
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
        } catch (e: Exception) {
            e.printStackTrace()
            PokerioLogger.error(e.toString())
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
        card1 = null
        card2 = null
        // Callbacks
        playerJoinedCallbacks.clear()
        playerRemovedCallbacks.clear()
        // Not resetting nextId, because someone might be holding on to an old one and we don't
        // want then to remove new callbacks by mistake

        onGameReset()
    }

    fun addOnPlayerJoinedCallback(callback: (Player) -> Unit): Int {
        playerJoinedCallbacks.put(nextId, callback)
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
        settingsChangedCallback.put(nextId, callback)
        return nextId++
    }

    fun removeOnSettingsChangedCallback(id: Int) {
        settingsChangedCallback.remove(id)
    }

    fun addPlayer(player: Player) {
        players.add(player)
        playerJoinedCallbacks.forEach { it.value(player) }
    }

    fun removePlayer(playerHash: String, newAdmin: String? = null) {
        // Check if this player is being removed
        val isThisPlayerRemoved = players.find {
            sha256(it.playerID) == playerHash
        } != null
        if (isThisPlayerRemoved) {
            resetGameState()
        } else {
            val removedPlayer = players.find { it.playerID == playerHash } ?: return

            if (removedPlayer.isAdmin && newAdmin == null) {
                throw Exception("Admin removed without new admin given")
            } else if (removedPlayer.isAdmin) {
                val isThisPlayerNewAdmin = players.find {
                    sha256(it.playerID) == newAdmin
                } != null

                if (isThisPlayerNewAdmin) {
                    isPlayerAdmin = true
                    val thisPlayer = players.find {
                        sha256(it.playerID) == newAdmin
                    }
                    thisPlayer!!.isAdmin = true
                } else {
                    val newAdminPlayer = players.find { it.playerID == newAdmin }
                    newAdminPlayer!!.isAdmin = true
                }
            }

            players.removeIf { it.playerID == playerHash }
            playerRemovedCallbacks.forEach { it.value(removedPlayer) }
        }
    }

    fun startGame(data: Map<String, String>) {
        card1 = Card(data["card1"]!!.slice(0..1), data["card1"]!!.slice(2..2))
        card2 = Card(data["card2"]!!.slice(0..1), data["card2"]!!.slice(2..2))
        players.forEach { it.funds = startingFunds }

        onGameStart()
    }

    fun sha256(string: String): String {
        return MessageDigest
            .getInstance("SHA-256")
            .digest(string.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
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
    val gameKey: Int,
    val startingFunds: Int,
    val smallBlind: Int
)

@Generated
@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = CreateGameResponse::class)
object CreateGameResponseSerializer
