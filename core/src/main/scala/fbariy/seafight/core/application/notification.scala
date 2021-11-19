package fbariy.seafight.core.application

import fbariy.seafight.core.domain.{Cell, Player}
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}
import fbariy.seafight.core.application.notification.instances.NotificationCodes._
import cats.syntax.either._

import java.util.UUID

object notification {
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

  object instances {
    implicit val notificationDecoder: Decoder[AppNotification] =
      (c: HCursor) =>
        for {
          code <- c.get[String]("code")
          notification <- code match {
            case BackRequested =>
              for {
                initiator <- c.get[Player]("initiator")
                gameId    <- c.get[UUID]("gameId")
                move      <- c.get[TurnOutput]("move")
              } yield BackRequestedNotification(initiator, gameId, move)
            case BackAccepted =>
              for {
                initiator <- c.get[Player]("initiator")
                gameId    <- c.get[UUID]("gameId")
              } yield BackAcceptedNotification(initiator, gameId)
            case BackCanceled =>
              for {
                initiator <- c.get[Player]("initiator")
                gameId    <- c.get[UUID]("gameId")
              } yield BackCanceledNotification(initiator, gameId)
            case MoveMade =>
              for {
                initiator <- c.get[Player]("initiator")
                gameId    <- c.get[UUID]("gameId")
                kick      <- c.get[Cell]("kick")
              } yield MoveMadeNotification(initiator, gameId, kick)
            case ShipsAdded =>
              for {
                initiator <- c.get[Player]("initiator")
                gameId    <- c.get[UUID]("gameId")
              } yield ShipsAddedNotification(initiator, gameId)
            case GameCreated =>
              for {
                gameId <- c.get[UUID]("gameId")
              } yield GameCreatedNotification(gameId)
            case _ =>
              Either.left[DecodingFailure, AppNotification](
                DecodingFailure(s"Not support notification code '$code'",
                                List.empty))
          }
        } yield notification
    implicit val notificationEncoder: Encoder[AppNotification] = {
      case n @ BackRequestedNotification(initiator, gameId, move) =>
        Json.obj(
          "code"      -> Json.fromString(instanceToCode(n)),
          "initiator" -> Encoder[Player].apply(initiator),
          "gameId"    -> Encoder[UUID].apply(gameId),
          "move"      -> Encoder[TurnOutput].apply(move)
        )
      case n @ BackAcceptedNotification(initiator, gameId) =>
        Json.obj(
          "code"      -> Json.fromString(instanceToCode(n)),
          "initiator" -> Encoder[Player].apply(initiator),
          "gameId"    -> Encoder[UUID].apply(gameId)
        )
      case n @ BackCanceledNotification(initiator, gameId) =>
        Json.obj(
          "code"      -> Json.fromString(instanceToCode(n)),
          "initiator" -> Encoder[Player].apply(initiator),
          "gameId"    -> Encoder[UUID].apply(gameId)
        )
      case n @ MoveMadeNotification(initiator, gameId, kick) =>
        Json.obj(
          "code"      -> Json.fromString(instanceToCode(n)),
          "initiator" -> Encoder[Player].apply(initiator),
          "gameId"    -> Encoder[UUID].apply(gameId),
          "kick"      -> Encoder[Cell].apply(kick)
        )
      case n @ ShipsAddedNotification(initiator, gameId) =>
        Json.obj(
          "code"      -> Json.fromString(instanceToCode(n)),
          "initiator" -> Encoder[Player].apply(initiator),
          "gameId"    -> Encoder[UUID].apply(gameId)
        )
      case n @ GameCreatedNotification(gameId) =>
        Json.obj(
          "code"   -> Json.fromString(instanceToCode(n)),
          "gameId" -> Encoder[UUID].apply(gameId)
        )
    }

    object NotificationCodes {
      def instanceToCode(notification: AppNotification): String =
        notification match {
          case BackRequestedNotification(_, _, _) => BackRequested
          case BackAcceptedNotification(_, _)     => BackAccepted
          case BackCanceledNotification(_, _)     => BackCanceled
          case MoveMadeNotification(_, _, _)      => MoveMade
          case ShipsAddedNotification(_, _)       => ShipsAdded
          case GameCreatedNotification(_)         => GameCreated
        }

      val BackRequested = "BACK_REQUESTED"
      val BackAccepted  = "BACK_ACCEPTED"
      val BackCanceled  = "BACK_CANCELED"
      val MoveMade      = "MOVE_MADE"
      val ShipsAdded    = "SHIPS_ADDED"
      val GameCreated   = "GAME_CREATED"
    }
  }
}
