package com.example.xtreamplayer

enum class DualTile { LEFT, RIGHT }

fun DualTile.opposite(): DualTile = if (this == DualTile.LEFT) DualTile.RIGHT else DualTile.LEFT

sealed interface DualTilePlayerState {
    data object Idle : DualTilePlayerState
    data object Loading : DualTilePlayerState
    data object Reconnecting : DualTilePlayerState  // silent auto-retry, shows spinner not error
    data object Ready : DualTilePlayerState
    data class Error(val message: String?) : DualTilePlayerState
}
