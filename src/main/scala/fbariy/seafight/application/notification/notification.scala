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
  final case class AcceptBackNotification(initiator: Player, gameId: UUID)
      extends AppNotification
  final case class CancelBackNotification(initiator: Player, gameId: UUID)
      extends AppNotification
}
