package fbariy.seafight.application

import fbariy.seafight.application.game.TurnOutput
import fbariy.seafight.domain.Player

import java.util.UUID

package object notification {
  sealed trait AppNotification

  final case class BackToMoveNotification(initiator: Player,
                                          gameId: UUID,
                                          move: TurnOutput)
      extends AppNotification
  final case class AcceptBackToMoveNotification(initiator: Player, gameId: UUID)
      extends AppNotification
  final case class CancelBackToMoveNotification(initiator: Player, gameId: UUID)
      extends AppNotification
}
