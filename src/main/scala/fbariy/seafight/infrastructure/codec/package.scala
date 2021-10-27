package fbariy.seafight.infrastructure

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyChain, ValidatedNec}
import cats.implicits._
import fbariy.seafight.application.AppErrorOutput
import fbariy.seafight.application.error._
import fbariy.seafight.application.game.{GameOutput, TurnOutput}
import fbariy.seafight.application.invite.{CreateInviteInput, InviteOutput}
import fbariy.seafight.application.notification._
import fbariy.seafight.application.ship.AddShipsOutput
import fbariy.seafight.domain._
import fbariy.seafight.infrastructure.codec.NotificationCodes._
import io.circe._
import io.circe.generic.semiauto._

import java.util.UUID

package object codec {
  implicit val playerDecoder: Decoder[Player] =
    (c: HCursor) => c.as[String].map(Player)

  implicit val playerEncoder: Encoder[Player] =
    (a: Player) => Json.fromString(a.name)

  implicit val cellDecoder: Decoder[Cell] = deriveDecoder
  implicit val cellEncoder: Encoder[Cell] = deriveEncoder
  implicit val turnDecoder: Decoder[Turn] = deriveDecoder
  implicit val turnEncoder: Encoder[Turn] = deriveEncoder

  implicit val createInviteInputDecoder: Decoder[CreateInviteInput] =
    deriveDecoder
  implicit val createInviteInputEncoder: Encoder[CreateInviteInput] =
    deriveEncoder
  implicit val inviteOutputEncoder: Encoder[InviteOutput]     = deriveEncoder
  implicit val inviteOutputDecoder: Decoder[InviteOutput]     = deriveDecoder
  implicit val addShipsOutputEncoder: Encoder[AddShipsOutput] = deriveEncoder
  implicit val addShipsOutputDecoder: Decoder[AddShipsOutput] = deriveDecoder
  implicit val turnOutputEncoder: Encoder[TurnOutput]         = deriveEncoder
  implicit val turnOutputDecoder: Decoder[TurnOutput]         = deriveDecoder
  implicit val gameOutputEncoder: Encoder[GameOutput]         = deriveEncoder
  implicit val gameOutputDecoder: Decoder[GameOutput]         = deriveDecoder

  implicit val appErrorOutputDecoder: Decoder[AppErrorOutput] = deriveDecoder
  implicit val appErrorOutputEncoder: Encoder[AppErrorOutput] = deriveEncoder

  implicit def applicationErrorEncoder[E <: AppError]: Encoder[E] =
    (e: E) => Encoder[AppErrorOutput].apply(appErrorToJson(e))

  private val appErrorToJson: PartialFunction[AppError, AppErrorOutput] = {
    case e: AuthError => authErrorToJson(e)
    case NotCorrectShipsError(_) =>
      AppErrorOutput("NOT_CORRECT_SHIPS", "Не корректное размещение кораблей")
    case SamePlayersError =>
      AppErrorOutput("PLAYERS_ARE_SAME", "Игроки должны быть разными")
    case PlayerCannotMakeMoveError(_) =>
      AppErrorOutput("PLAYER_CANNOT_MAKE_MOVE", "Сейчас не ваш ход")
    case GameOverError(winner) =>
      AppErrorOutput("GAME_OVER", s"Игра окончена. Победитель: $winner")
    case EmptyPlayerError =>
      AppErrorOutput("PLAYER_IS_EMPTY", "Имя игрока не может быть пустым")
    case GameAlreadyExist =>
      AppErrorOutput("GAME_ALREADY_EXIST", "Игра уже создана")
    case MoveIsNotExistError =>
      AppErrorOutput("MOVE_IS_NOT_EXIST", "Ход не существует")
    case BackAlreadyRequestedError =>
      AppErrorOutput("BACK_ALREADY_REQUESTED", "Возврат хода уже запрошен")
    case BackNotRequestedError =>
      AppErrorOutput("BACK_NOT_REQUESTED", "Возврат хода не был запрошен")
    case InitiatorCannotAcceptBackError =>
      AppErrorOutput("INITIATOR_CANNOT_ACCEPT_BACK",
                     "Инициатор возврата не может принять его")
  }

  private val authErrorToJson: PartialFunction[AuthError, AppErrorOutput] = {
    case MissingGameIdError =>
      AppErrorOutput("MISSING_GAME_ID", "Не указан GameId заголовок")
    case MissingPlayerError =>
      AppErrorOutput("MISSING_PLAYER", "Не указан Player заголовок")
    case MissingInviteIdError =>
      AppErrorOutput("MISSING_INVITE_ID", "Не указан InviteId заголовок")
    case BadFormatGameIdError(id) =>
      AppErrorOutput(
        "BAD_FORMAT_GAME_ID",
        s"Значение заголовка GameId должно быть в формате uuid v4, получено $id")
    case BadFormatInviteIdError(id) =>
      AppErrorOutput(
        "BAD_FORMAT_INVITE_ID",
        s"Значение заголовка InviteId должно быть в формате uuid v4, получено $id")
    case NotFoundGameError =>
      AppErrorOutput("NOT_FOUND_GAME", "Не найдена игра по переданным данным")
    case NotFoundInviteError =>
      AppErrorOutput("NOT_FOUND_INVITE",
                     "Не найден инвайт по переданным данным")
  }

  implicit def validatedNecEncoder[E: Encoder, A: Encoder]
    : Encoder[ValidatedNec[E, A]] = {
    case Valid(a) => Encoder[A].apply(a)
    case Invalid(e) =>
      Json.obj(
        "type"   -> Json.fromString("error"),
        "errors" -> Encoder[NonEmptyChain[E]].apply(e)
      )
  }

  implicit def validatedNecDecoder[E: Decoder, A: Decoder]
    : Decoder[ValidatedNec[E, A]] =
    (c: HCursor) =>
      (c.get[String]("type"), c.get[Json]("errors")).tupled match {
        case Left(_) => Decoder[A].apply(c).map(_.valid.toValidatedNec)
        case Right(_ -> errors) =>
          Decoder[NonEmptyChain[E]].decodeJson(errors).map(_.invalid)
    }

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
  }

  object NotificationCodes {
    def instanceToCode(notification: AppNotification): String =
      notification match {
        case BackRequestedNotification(_, _, _) => BackRequested
        case BackAcceptedNotification(_, _)     => BackAccepted
        case BackCanceledNotification(_, _)     => BackCanceled
        case MoveMadeNotification(_, _, _)      => MoveMade
      }

    val BackRequested = "BACK_REQUESTED"
    val BackAccepted  = "BACK_ACCEPTED"
    val BackCanceled  = "BACK_CANCELED"
    val MoveMade      = "MOVE_MADE"
  }
}
