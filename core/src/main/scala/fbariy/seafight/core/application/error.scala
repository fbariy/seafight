package fbariy.seafight.core.application

import cats.Show
import fbariy.seafight.core.application.error.instances.ErrorCodes._
import fbariy.seafight.core.domain.{Cell, Player}
import io.circe.{Decoder, Encoder, HCursor, Json}

import java.util.UUID

object error {
  sealed trait AppError

  sealed trait AuthError                        extends AppError
  case object MissingGameIdError                extends AuthError
  case object MissingPlayerError                extends AuthError
  case object MissingInviteIdError              extends AuthError
  case class BadFormatGameIdError(id: String)   extends AuthError
  case class BadFormatInviteIdError(id: String) extends AuthError
  case object NotFoundGameError                 extends AuthError
  case object NotFoundInviteError               extends AuthError

  case object ServerSystemError                     extends AppError
  case class NotCorrectShipsError(ships: Seq[Cell]) extends AppError
  case object SamePlayersError                      extends AppError
  case class PlayerCannotMakeMoveError(p: Player)   extends AppError
  case class GameOverError(winner: Player)          extends AppError
  case object EmptyPlayerError                      extends AppError
  case object GameAlreadyExistError                 extends AppError
  case object MoveIsNotExistError                   extends AppError
  case object BackAlreadyRequestedError             extends AppError
  case object BackNotRequestedError                 extends AppError
  case object InitiatorCannotAcceptBackError        extends AppError
  case class ShipsAreNotSetupError(inviteId: UUID, player: Player)
      extends AppError

  object instances {
    implicit val appErrorShow: Show[AppError] =
      (t: AppError) => t.toString

    object ErrorCodes {
      val MissingGameId     = "MISSING_GAME_ID"
      val MissingPlayer     = "MISSING_PLAYER"
      val MissingInviteId   = "MISSING_INVITE_ID"
      val BadFormatGameId   = "BAD_FORMAT_GAME_ID"
      val BadFormatInviteId = "BAD_FORMAT_INVITE_ID"
      val NotFoundGame      = "NOT_FOUND_GAME"
      val NotFoundInvite    = "NOT_FOUND_INVITE"

      val NotCorrectShips           = "NOT_CORRECT_SHIPS"
      val SamePlayers               = "SAME_PLAYERS"
      val PlayerCannotMakeMove      = "PLAYER_CANNOT_MAKE_MOVE"
      val GameOver                  = "GAME_OVER"
      val EmptyPlayer               = "EMPTY_PLAYER"
      val GameAlreadyExist          = "GAME_ALREADY_EXIST"
      val MoveIsNotExist            = "MOVE_IS_NOT_EXIST"
      val BackAlreadyRequested      = "BACK_ALREADY_REQUESTED"
      val InitiatorCannotAcceptBack = "INITIATOR_CANNOT_ACCEPT_BACK"
      val BackNotRequested          = "BACK_NOT_REQUESTED"
      val ShipsAreNotSetup          = "SHIPS_ARE_NOT_SETUP"
      val ServerSystem              = "SERVER_SYSTEM"
    }

    implicit val appErrorDecoder: Decoder[AppError] =
      (c: HCursor) =>
        for {
          code <- c.get[String]("code")
          error <- code match {
            case MissingGameId   => Right(MissingGameIdError)
            case MissingPlayer   => Right(MissingPlayerError)
            case MissingInviteId => Right(MissingInviteIdError)
            case BadFormatGameId =>
              c.get[String]("id").map(BadFormatGameIdError(_))
            case BadFormatInviteId =>
              c.get[String]("id").map(BadFormatInviteIdError(_))
            case NotFoundGame   => Right(NotFoundGameError)
            case NotFoundInvite => Right(NotFoundInviteError)
            // above were auth errors
            case NotCorrectShips =>
              c.get[Seq[Cell]]("ships").map(NotCorrectShipsError(_))
            case SamePlayers => Right(SamePlayersError)
            case PlayerCannotMakeMove =>
              c.get[Player]("player").map(PlayerCannotMakeMoveError(_))
            case GameOver             => c.get[Player]("winner").map(GameOverError(_))
            case EmptyPlayer          => Right(EmptyPlayerError)
            case GameAlreadyExist     => Right(GameAlreadyExistError)
            case MoveIsNotExist       => Right(MoveIsNotExistError)
            case BackAlreadyRequested => Right(BackAlreadyRequestedError)
            case InitiatorCannotAcceptBack =>
              Right(InitiatorCannotAcceptBackError)
            case BackNotRequested => Right(BackNotRequestedError)
            case ShipsAreNotSetup =>
              for {
                inviteId <- c.get[UUID]("invite_id")
                player   <- c.get[Player]("player")
              } yield ShipsAreNotSetupError(inviteId, player)
            case ServerSystem => Right(ServerSystemError)
          }
        } yield error

    implicit def appErrorEncoder[E <: AppError]: Encoder[E] = {
      case e: AuthError => authErrorToJson(e)
      case NotCorrectShipsError(ships) =>
        Json.obj(
          "code"    -> Json.fromString(NotCorrectShips),
          "message" -> Json.fromString("Не корректное размещение кораблей"),
          "ships"   -> Encoder[Seq[Cell]].apply(ships)
        )
      case SamePlayersError =>
        Json.obj(
          "code"    -> Json.fromString(SamePlayers),
          "message" -> Json.fromString("Игроки должны быть разными")
        )
      case PlayerCannotMakeMoveError(p) =>
        Json.obj(
          "code"    -> Json.fromString(PlayerCannotMakeMove),
          "message" -> Json.fromString("Сейчас не ваш ход"),
          "player"  -> Encoder[Player].apply(p)
        )
      case GameOverError(winner) =>
        Json.obj(
          "code"    -> Json.fromString(GameOver),
          "message" -> Json.fromString(s"Игра окончена. Победитель: $winner"),
          "winner"  -> Encoder[Player].apply(winner)
        )
      case EmptyPlayerError =>
        Json.obj(
          "code"    -> Json.fromString(EmptyPlayer),
          "message" -> Json.fromString("Имя игрока не может быть пустым")
        )
      case GameAlreadyExistError =>
        Json.obj(
          "code"    -> Json.fromString(GameAlreadyExist),
          "message" -> Json.fromString("Игра уже создана")
        )
      case MoveIsNotExistError =>
        Json.obj(
          "code"    -> Json.fromString(MoveIsNotExist),
          "message" -> Json.fromString("Ход не существует")
        )
      case BackAlreadyRequestedError =>
        Json.obj(
          "code"    -> Json.fromString(BackAlreadyRequested),
          "message" -> Json.fromString("Возврат хода уже запрошен")
        )
      case BackNotRequestedError =>
        Json.obj(
          "code"    -> Json.fromString(BackNotRequested),
          "message" -> Json.fromString("Возврат хода не был запрошен")
        )
      case InitiatorCannotAcceptBackError =>
        Json.obj(
          "code" -> Json.fromString(InitiatorCannotAcceptBack),
          "message" -> Json.fromString(
            "Инициатор возврата не может принять его")
        )
      case ShipsAreNotSetupError(id, p) =>
        Json.obj(
          "code"      -> Json.fromString(ShipsAreNotSetup),
          "message"   -> Json.fromString("Игрок не расставил корабли"),
          "invite_id" -> Encoder[UUID].apply(id),
          "player"    -> Encoder[Player].apply(p)
        )
    }

    private val authErrorToJson: PartialFunction[AuthError, Json] = {
      case MissingGameIdError =>
        Json.obj(
          "code"    -> Json.fromString(MissingGameId),
          "message" -> Json.fromString("Не указан GameId заголовок")
        )
      case MissingPlayerError =>
        Json.obj(
          "code"    -> Json.fromString(MissingPlayer),
          "message" -> Json.fromString("Не указан Player заголовок")
        )
      case MissingInviteIdError =>
        Json.obj(
          "code"    -> Json.fromString(MissingInviteId),
          "message" -> Json.fromString("Не указан InviteId заголовок")
        )
      case BadFormatGameIdError(id) =>
        Json.obj(
          "code" -> Json.fromString(BadFormatGameId),
          "message" -> Json.fromString(
            s"Значение заголовка GameId должно быть в формате uuid v4, получено $id"),
          "id" -> Json.fromString(id)
        )
      case BadFormatInviteIdError(id) =>
        Json.obj(
          "code" -> Json.fromString(BadFormatInviteId),
          "message" -> Json.fromString(
            s"Значение заголовка GameId должно быть в формате uuid v4, получено $id"),
          "id" -> Json.fromString(id)
        )
      case NotFoundGameError =>
        Json.obj(
          "code"    -> Json.fromString(NotFoundGame),
          "message" -> Json.fromString("Не найдена игра по переданным данным")
        )
      case NotFoundInviteError =>
        Json.obj(
          "code"    -> Json.fromString(NotFoundInvite),
          "message" -> Json.fromString("Не найден инвайт по переданным данным")
        )
    }
  }
}
