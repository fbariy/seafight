package fbariy.seafight.application

import fbariy.seafight.application.game.TurnOutput
import fbariy.seafight.domain.{Cell, Player}

import java.util.UUID

package object notification {
  sealed trait AppNotification
  final case class BackRequestedNotification(initiator: Player,
                                             gameId: UUID,
                                             move: TurnOutput)
      extends AppNotification
  final case class BackAcceptedNotification(initiator: Player, gameId: UUID)
      extends AppNotification
  final case class BackCanceledNotification(initiator: Player, gameId: UUID)
      extends AppNotification
  final case class MoveMadeNotification(initiator: Player,
                                        gameId: UUID,
                                        kick: Cell)
      extends AppNotification
  final case class ShipsAddedNotification(initiator: Player, gameId: UUID)
      extends AppNotification

  final case class GameCreatedNotification(gameId: UUID) extends AppNotification
}
