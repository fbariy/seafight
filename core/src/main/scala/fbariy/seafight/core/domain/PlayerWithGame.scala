package fbariy.seafight.core.domain

case class PlayerWithGame(override val p: Player,
                          override val opp: Player,
                          override val isFirst: Boolean,
                          game: GameWithPlayers)
    extends PlayerContext(p, opp, isFirst)

object PlayerWithGame {
  implicit class PlayerWithGameOps(playerCtx: PlayerWithGame) {
    def updateMoves(moves: Seq[Turn]): PlayerWithGame =
      playerCtx.copy(game = playerCtx.game.updateMoves(moves))

    def addMove(kick: Cell): PlayerWithGame =
      playerCtx.copy(game = playerCtx.game.addMove(playerCtx.p, kick))
  }
}
